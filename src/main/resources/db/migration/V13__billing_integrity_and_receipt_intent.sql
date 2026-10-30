-- Forward-only changes to the 49-table model. No legacy rows are rewritten.
-- A receipt may pay an invoice, but received cash and allocated debt stay separate.
ALTER TABLE cash_transactions ADD COLUMN payment_document_id bigint REFERENCES billing_documents(id);
CREATE INDEX idx_cash_payment_document ON cash_transactions(payment_document_id) WHERE payment_document_id IS NOT NULL;
-- Keep the billed point immutable, while future intervals use its verified successor.
ALTER TABLE meter_readings ADD COLUMN correction_item_id bigint UNIQUE REFERENCES billing_items(id);

ALTER TABLE properties DROP CONSTRAINT properties_invoice_issue_day_check;
ALTER TABLE properties ADD CONSTRAINT properties_invoice_issue_day_check CHECK(invoice_issue_day BETWEEN 1 AND 31);
ALTER TABLE contract_terms DROP CONSTRAINT contract_terms_payment_day_check;
ALTER TABLE contract_terms ADD CONSTRAINT contract_terms_payment_day_check CHECK(payment_day BETWEEN 1 AND 31);

CREATE FUNCTION guard13_cash_purpose() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target billing_documents%ROWTYPE; account_property bigint; target_property bigint;
BEGIN
 IF NEW.payment_document_id IS NOT NULL THEN
  SELECT * INTO target FROM billing_documents WHERE id=NEW.payment_document_id;
  SELECT property_id INTO account_property FROM cash_accounts WHERE id=NEW.cash_account_id;
  SELECT r.property_id INTO target_property FROM contracts c JOIN rooms r ON r.id=c.room_id WHERE c.id=target.contract_id;
  IF NEW.direction<>'IN' OR target.contract_id IS NULL OR target.status<>'ISSUED'
   OR (account_property IS NOT NULL AND account_property IS DISTINCT FROM target_property) THEN
   RAISE EXCEPTION 'Receipt invoice must be issued and belong to its receiving property' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER cash_purpose13 BEFORE INSERT OR UPDATE ON cash_transactions FOR EACH ROW EXECUTE FUNCTION guard13_cash_purpose();

CREATE FUNCTION guard13_document_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE related billing_documents%ROWTYPE;
BEGIN
 IF NEW.related_document_id IS NOT NULL THEN
  SELECT * INTO related FROM billing_documents WHERE id=NEW.related_document_id;
  IF related.contract_id IS DISTINCT FROM NEW.contract_id OR related.reservation_id IS DISTINCT FROM NEW.reservation_id THEN
   RAISE EXCEPTION 'Related documents must belong to the same tenancy or reservation' USING ERRCODE='23514';
  END IF;
 END IF;
 IF NEW.checkout_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM checkout_records WHERE id=NEW.checkout_id AND contract_id=NEW.contract_id) THEN
  RAISE EXCEPTION 'Checkout belongs to a different tenancy' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER document_scope13 BEFORE INSERT OR UPDATE ON billing_documents FOR EACH ROW EXECUTE FUNCTION guard13_document_scope();

CREATE FUNCTION guard13_reading_chain() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE device meters%ROWTYPE; previous meter_readings%ROWTYPE; correction v_billing_lines%ROWTYPE;
 original v_billing_lines%ROWTYPE; endpoint v_segments%ROWTYPE; delta numeric;
BEGIN
 SELECT * INTO device FROM meters WHERE id=NEW.meter_id FOR UPDATE;
 IF NEW.observed_at<device.installed_at OR (device.retired_at IS NOT NULL AND NEW.observed_at>device.retired_at) OR NEW.observed_at>clock_timestamp() THEN
  RAISE EXCEPTION 'Measurement must be within the physical meter lifetime and cannot be in the future' USING ERRCODE='23514';
 END IF;
 IF NEW.supersedes_reading_id IS NOT NULL THEN
  SELECT * INTO previous FROM meter_readings WHERE id=NEW.supersedes_reading_id;
  IF previous.meter_id<>NEW.meter_id OR previous.observed_at<>NEW.observed_at OR NEW.revision_no<>previous.revision_no+1 OR btrim(coalesce(NEW.reason,''))='' THEN
   RAISE EXCEPTION 'A revision must describe the same meter and instant with a reason' USING ERRCODE='23514';
  END IF;
  IF NEW.correction_item_id IS NOT NULL THEN
   SELECT * INTO correction FROM v_billing_lines WHERE id=NEW.correction_item_id;
   SELECT * INTO original FROM v_billing_lines WHERE id=correction.adjusts_item_id;
   SELECT * INTO endpoint FROM v_segments WHERE id=original.segment_id;
   IF correction.document_status IS DISTINCT FROM 'ISSUED' OR original.calculation_basis IS DISTINCT FROM 'METER'
    OR endpoint.meter_id IS DISTINCT FROM NEW.meter_id OR endpoint.service_end IS DISTINCT FROM NEW.observed_at
    OR correction.source_key IS DISTINCT FROM 'METER-CORRECTION-'||original.id||'-'||(SELECT document_code FROM billing_documents WHERE id=correction.document_id) THEN
    RAISE EXCEPTION 'Meter revision requires its issued, linked correction at the same endpoint' USING ERRCODE='23514';
   END IF;
   SELECT coalesce(sum(CASE direction WHEN 'DEBIT' THEN billed_quantity ELSE -billed_quantity END),0) INTO delta
    FROM v_billing_lines WHERE adjusts_item_id=original.id AND document_status='ISSUED' AND source_key LIKE 'METER-CORRECTION-'||original.id||'-%';
   IF NEW.reading_value<>original.old_index+original.billed_quantity+delta THEN
    RAISE EXCEPTION 'Corrected point disagrees with the issued usage corrections' USING ERRCODE='23514';
   END IF;
  ELSIF previous.correction_item_id IS NOT NULL OR EXISTS(SELECT 1 FROM consumption_segments s JOIN billing_items i ON i.segment_id=s.id
   WHERE s.start_reading_id=previous.id OR s.end_reading_id=previous.id) THEN
   RAISE EXCEPTION 'Remove unissued draft first; issued measurement requires a linked financial correction' USING ERRCODE='23514';
  END IF;
 ELSIF NEW.revision_no<>1 OR NEW.correction_item_id IS NOT NULL THEN
  RAISE EXCEPTION 'A later reading version must reference its predecessor' USING ERRCODE='23514';
 END IF;
 IF EXISTS(SELECT 1 FROM meter_readings r WHERE r.meter_id=NEW.meter_id AND r.observed_at=NEW.observed_at
   AND r.id IS DISTINCT FROM NEW.supersedes_reading_id AND NOT EXISTS(SELECT 1 FROM meter_readings n WHERE n.supersedes_reading_id=r.id)) THEN
  RAISE EXCEPTION 'A current measurement exists at this instant; use a revision' USING ERRCODE='23514';
 END IF;
 IF EXISTS(SELECT 1 FROM meter_readings r WHERE r.meter_id=NEW.meter_id
   AND NOT EXISTS(SELECT 1 FROM meter_readings n WHERE n.supersedes_reading_id=r.id)
   AND ((r.observed_at<NEW.observed_at AND r.reading_value>NEW.reading_value) OR (r.observed_at>NEW.observed_at AND r.reading_value<NEW.reading_value))) THEN
  RAISE EXCEPTION 'Measurement conflicts with an earlier or later reading on this meter' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
-- Replaces V11's blanket revision ban with the checked correction path above.
DROP TRIGGER reading49 ON meter_readings;
CREATE TRIGGER reading_chain13 BEFORE INSERT ON meter_readings FOR EACH ROW EXECUTE FUNCTION guard13_reading_chain();

CREATE FUNCTION guard13_possession_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM v_segments s WHERE s.contract_id=NEW.contract_id
   AND NOT EXISTS(SELECT 1 FROM consumption_segments n WHERE n.supersedes_segment_id=s.id)
   AND (s.service_start<NEW.taken_at OR (NEW.returned_at IS NOT NULL AND s.service_end>NEW.returned_at))) THEN
  RAISE EXCEPTION 'Possession change conflicts with assigned consumption; reconcile the measured intervals first' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER possession_history13 BEFORE UPDATE ON room_possessions FOR EACH ROW EXECUTE FUNCTION guard13_possession_history();

CREATE FUNCTION guard13_hold_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE contract bigint;
BEGIN
 SELECT contract_id INTO contract FROM fund_accounts WHERE id=NEW.fund_account_id;
 IF NEW.related_billing_item_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM billing_items i JOIN billing_documents d ON d.id=i.document_id
   WHERE i.id=NEW.related_billing_item_id AND d.contract_id=contract) THEN
  RAISE EXCEPTION 'Held money and its supporting charge must belong to the same tenancy' USING ERRCODE='23514';
 END IF;
 IF TG_OP='UPDATE' AND ROW(NEW.fund_account_id,NEW.related_billing_item_id,NEW.amount,NEW.reason,NEW.review_on,NEW.created_by)
   IS DISTINCT FROM ROW(OLD.fund_account_id,OLD.related_billing_item_id,OLD.amount,OLD.reason,OLD.review_on,OLD.created_by) THEN
  RAISE EXCEPTION 'Release the hold and record a new justified hold instead of overwriting its history' USING ERRCODE='23514';
 END IF;
 IF TG_OP='UPDATE' AND OLD.released_at IS NOT NULL AND NEW.released_at IS DISTINCT FROM OLD.released_at THEN
  RAISE EXCEPTION 'Released hold cannot be reopened' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER hold_scope13 BEFORE INSERT OR UPDATE ON fund_holds FOR EACH ROW EXECUTE FUNCTION guard13_hold_scope();
