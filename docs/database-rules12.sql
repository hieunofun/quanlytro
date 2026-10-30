-- Forward migration. V11 remains unchanged; no table or column is added.
CREATE FUNCTION check12_meter_responsibility(target_contract bigint, measured_from timestamptz, measured_to timestamptz)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
 IF target_contract IS NULL THEN RETURN; END IF;
 PERFORM id FROM contracts WHERE id=target_contract FOR UPDATE;
 IF NOT EXISTS(SELECT 1 FROM room_possessions p WHERE p.contract_id=target_contract
  AND p.taken_at<=measured_from AND (p.returned_at IS NULL OR p.returned_at>=measured_to)) THEN
  RAISE EXCEPTION 'Meter interval is outside actual possession; split at handover readings before assigning tenant responsibility'
   USING ERRCODE='23514';
 END IF;
END $$;

CREATE FUNCTION guard12_segment_bounds() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE a timestamptz; b timestamptz;
BEGIN
 IF NEW.contract_id IS NOT NULL THEN
  SELECT observed_at INTO a FROM meter_readings WHERE id=NEW.start_reading_id;
  SELECT observed_at INTO b FROM meter_readings WHERE id=NEW.end_reading_id;
  PERFORM check12_meter_responsibility(NEW.contract_id,a,b);
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER segment_bounds12 BEFORE INSERT ON consumption_segments FOR EACH ROW EXECUTE FUNCTION guard12_segment_bounds();

CREATE FUNCTION guard12_meter_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE segment v_segments%ROWTYPE; target_contract bigint;
BEGIN
 IF NEW.segment_id IS NOT NULL THEN
  IF NEW.calculation_basis<>'METER' THEN RAISE EXCEPTION 'A meter-source line must use measured quantity' USING ERRCODE='23514'; END IF;
  SELECT * INTO segment FROM v_segments WHERE id=NEW.segment_id;
  SELECT contract_id INTO target_contract FROM billing_documents WHERE id=NEW.document_id;
  IF target_contract IS DISTINCT FROM segment.contract_id THEN RAISE EXCEPTION 'Meter responsibility differs from document' USING ERRCODE='23514'; END IF;
  PERFORM check12_meter_responsibility(target_contract,segment.service_start,segment.service_end);
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER meter_item_bounds12 BEFORE INSERT OR UPDATE ON billing_items FOR EACH ROW EXECUTE FUNCTION guard12_meter_item();

CREATE FUNCTION guard12_issue_meter_bounds() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE measured record;
BEGIN
 IF NEW.status='ISSUED' AND OLD.status='DRAFT' THEN
  FOR measured IN SELECT s.* FROM billing_items i JOIN v_segments s ON s.id=i.segment_id WHERE i.document_id=NEW.id LOOP
   PERFORM check12_meter_responsibility(NEW.contract_id,measured.service_start,measured.service_end);
  END LOOP;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER meter_issue_bounds12 BEFORE UPDATE ON billing_documents FOR EACH ROW EXECUTE FUNCTION guard12_issue_meter_bounds();

CREATE FUNCTION guard12_reading_dependency() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.supersedes_reading_id IS NOT NULL AND EXISTS(SELECT 1 FROM consumption_segments s
  WHERE s.start_reading_id=NEW.supersedes_reading_id
  AND NOT EXISTS(SELECT 1 FROM consumption_segments later WHERE later.supersedes_segment_id=s.id)) THEN
  RAISE EXCEPTION 'A subsequent interval uses this opening point; reconcile the dependent intervals explicitly' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER reading_dependency12 BEFORE INSERT ON meter_readings FOR EACH ROW EXECUTE FUNCTION guard12_reading_dependency();
