-- Views and transactional guards for the approved 49-table model.
CREATE VIEW v_contract_current AS
SELECT c.*,r.property_id,p.tenant_id primary_tenant_id,t.lease_end end_date,a.rental_price,
 a.deposit_required deposit_amount,a.payment_day payment_cycle_day,a.terms,a.collection_timing,
 a.id term_id,(SELECT max(x.returned_at)::date FROM room_possessions x WHERE x.contract_id=c.id AND x.returned_at IS NOT NULL) actual_checkout_date
FROM contracts c JOIN rooms r ON r.id=c.room_id
LEFT JOIN contract_parties p ON p.contract_id=c.id AND p.party_role='PRIMARY'
LEFT JOIN LATERAL(SELECT * FROM contract_terms x WHERE x.contract_id=c.id AND status='APPROVED'
 ORDER BY effective_from DESC,version_no DESC LIMIT 1)t ON true
LEFT JOIN LATERAL(SELECT * FROM contract_terms x WHERE x.contract_id=c.id AND status='APPROVED' AND effective_from<=greatest(current_date,c.start_date)
 ORDER BY effective_from DESC,version_no DESC LIMIT 1)a ON true;

CREATE VIEW v_room_status AS
SELECT r.*,price.amount base_price,coalesce(op.status,'READY') operational_status,
 CASE WHEN EXISTS(SELECT 1 FROM room_possessions x JOIN contracts c ON c.id=x.contract_id
 WHERE c.room_id=r.id AND x.taken_at<=clock_timestamp() AND x.returned_at IS NULL) THEN 'RENTED'
 WHEN coalesce(op.status,'READY')='READY' THEN 'AVAILABLE' ELSE op.status END display_status
FROM rooms r
LEFT JOIN LATERAL(SELECT amount FROM room_price_history WHERE room_id=r.id AND effective_from<=CURRENT_DATE ORDER BY effective_from DESC LIMIT 1)price ON true
LEFT JOIN LATERAL(SELECT status FROM room_operating_periods WHERE room_id=r.id AND valid_from<=clock_timestamp() AND (valid_to IS NULL OR valid_to>clock_timestamp()) LIMIT 1)op ON true;

CREATE VIEW v_segments AS
SELECT s.*,a.meter_id,m.room_id,r.property_id,m.service_item_id,a.observed_at service_start,b.observed_at service_end,
 a.reading_value old_index,b.reading_value new_index,b.reading_value-a.reading_value consumption,
 extract(epoch FROM b.observed_at-a.observed_at)/86400.0 duration_days
FROM consumption_segments s JOIN meter_readings a ON a.id=s.start_reading_id JOIN meter_readings b ON b.id=s.end_reading_id
JOIN meters m ON m.id=a.meter_id JOIN rooms r ON r.id=m.room_id;

CREATE VIEW v_billing_lines AS
SELECT i.*,d.contract_id,d.reservation_id,d.status document_status,d.document_type,d.due_date,
 coalesce(s.consumption,i.quantity_snapshot) billed_quantity,
 round(i.unit_price_snapshot*coalesce(s.consumption,i.quantity_snapshot)/CASE WHEN i.calculation_basis='PRORATED'
 THEN extract(day FROM i.calculation_period+interval '1 month - 1 day') ELSE 1 END,2)::numeric(16,2) subtotal,
 s.old_index,s.new_index
FROM billing_items i JOIN billing_documents d ON d.id=i.document_id LEFT JOIN v_segments s ON s.id=i.segment_id;

CREATE VIEW v_fund_movements AS
SELECT id entry_id,to_fund_id fund_id,amount delta FROM fund_entries WHERE to_fund_id IS NOT NULL
UNION ALL SELECT id,from_fund_id,-amount FROM fund_entries WHERE from_fund_id IS NOT NULL
UNION ALL SELECT r.id,o.to_fund_id,-r.amount FROM fund_entries r JOIN fund_entries o ON o.id=r.original_entry_id WHERE r.entry_type='REVERSAL' AND o.to_fund_id IS NOT NULL
UNION ALL SELECT r.id,o.from_fund_id,r.amount FROM fund_entries r JOIN fund_entries o ON o.id=r.original_entry_id WHERE r.entry_type='REVERSAL' AND o.from_fund_id IS NOT NULL;

CREATE VIEW v_fund_balances AS
SELECT f.*,coalesce(x.balance,0)::numeric(16,2) balance,coalesce(h.held,0)::numeric(16,2) held_amount,
 (coalesce(x.balance,0)-coalesce(h.held,0))::numeric(16,2) available
FROM fund_accounts f LEFT JOIN(SELECT fund_id,sum(delta) balance FROM v_fund_movements GROUP BY fund_id)x ON x.fund_id=f.id
LEFT JOIN(SELECT fund_account_id,sum(amount) held FROM fund_holds WHERE released_at IS NULL GROUP BY fund_account_id)h ON h.fund_account_id=f.id;

CREATE VIEW v_item_applications AS
SELECT id entry_id,debit_item_id item_id,amount amount FROM fund_entries WHERE entry_type='PAYMENT'
UNION ALL SELECT r.id,o.debit_item_id,-r.amount FROM fund_entries r JOIN fund_entries o ON o.id=r.original_entry_id WHERE r.entry_type IN ('REVERSAL','ADJUSTMENT_RELEASE') AND o.entry_type='PAYMENT'
UNION ALL SELECT r.id,p.debit_item_id,r.amount FROM fund_entries r JOIN fund_entries o ON o.id=r.original_entry_id JOIN fund_entries p ON p.id=o.original_entry_id WHERE r.entry_type='REVERSAL' AND o.entry_type='ADJUSTMENT_RELEASE';

CREATE VIEW v_item_balances AS
SELECT l.*, (l.subtotal+coalesce(adj.delta,0))::numeric(16,2) charge_amount,
 coalesce(pay.paid,0)::numeric(16,2) paid_amount,
 greatest(l.subtotal+coalesce(adj.delta,0)-coalesce(pay.paid,0),0)::numeric(16,2) balance_due,
 greatest(coalesce(pay.paid,0)-l.subtotal-coalesce(adj.delta,0),0)::numeric(16,2) overpayment
FROM v_billing_lines l
LEFT JOIN(SELECT adjusts_item_id,sum(CASE direction WHEN 'DEBIT' THEN subtotal ELSE -subtotal END) delta
 FROM v_billing_lines WHERE document_status='ISSUED' AND adjusts_item_id IS NOT NULL GROUP BY adjusts_item_id)adj ON adj.adjusts_item_id=l.id
LEFT JOIN(SELECT item_id,sum(amount) paid FROM v_item_applications GROUP BY item_id)pay ON pay.item_id=l.id
WHERE l.direction='DEBIT' AND l.adjusts_item_id IS NULL;

CREATE VIEW v_document_balances AS
SELECT d.*,coalesce(a.total,0)::numeric(16,2) total_amount,coalesce(a.paid,0)::numeric(16,2) paid_amount,
 coalesce(a.debt,0)::numeric(16,2) balance_due,
 CASE WHEN coalesce(a.debt,0)=0 AND coalesce(a.paid,0)>0 THEN 'PAID' WHEN coalesce(a.paid,0)>0 THEN 'PARTIALLY_PAID' ELSE 'UNPAID' END payment_status
FROM billing_documents d LEFT JOIN(SELECT document_id,sum(charge_amount) total,sum(paid_amount) paid,sum(balance_due) debt FROM v_item_balances GROUP BY document_id)a ON a.document_id=d.id;

CREATE VIEW v_maintenance AS
SELECT m.*,coalesce(m.room_id,ap.room_id) resolved_room_id,
 coalesce(m.property_id,ap.property_id,r.property_id) resolved_property_id,
 coalesce(cost.total,0)::numeric(16,2) actual_cost,
 (SELECT note FROM maintenance_events e WHERE e.request_id=m.id ORDER BY e.id DESC LIMIT 1) progress_note
FROM maintenance_requests m LEFT JOIN asset_placements ap ON ap.id=m.asset_placement_id
LEFT JOIN rooms r ON r.id=coalesce(m.room_id,ap.room_id)
LEFT JOIN(SELECT request_id,sum(amount) total FROM maintenance_costs GROUP BY request_id)cost ON cost.request_id=m.id;

CREATE OR REPLACE FUNCTION can_manage_property(actor bigint,target_property bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM users u WHERE u.id=actor AND u.status='ACTIVE' AND (u.role='ADMIN' OR
 (u.role='STAFF' AND EXISTS(SELECT 1 FROM employee_assignments a WHERE a.user_id=u.id AND a.property_id=target_property AND a.assigned_at<=clock_timestamp() AND a.revoked_at IS NULL))));
$$;
CREATE FUNCTION can_manage_room(actor bigint,target_room bigint) RETURNS boolean LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM rooms r JOIN users u ON u.id=actor WHERE r.id=target_room AND u.status='ACTIVE' AND
 (u.role='ADMIN' OR (u.role='STAFF' AND EXISTS(SELECT 1 FROM employee_assignments a WHERE a.user_id=u.id AND a.assigned_at<=clock_timestamp() AND a.revoked_at IS NULL AND
 (a.room_id=r.id OR a.property_id=r.property_id)))));
$$;

CREATE FUNCTION guard49_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION '% is append-only; create a linked correction',TG_TABLE_NAME USING ERRCODE='23514'; END $$;
CREATE TRIGGER immutable_fund_entry BEFORE UPDATE OR DELETE ON fund_entries FOR EACH ROW EXECUTE FUNCTION guard49_append_only();
CREATE TRIGGER immutable_audit BEFORE UPDATE OR DELETE ON audit_events FOR EACH ROW EXECUTE FUNCTION guard49_append_only();
CREATE TRIGGER immutable_reading BEFORE UPDATE OR DELETE ON meter_readings FOR EACH ROW EXECUTE FUNCTION guard49_append_only();
CREATE TRIGGER immutable_segment BEFORE UPDATE OR DELETE ON consumption_segments FOR EACH ROW EXECUTE FUNCTION guard49_append_only();

CREATE FUNCTION guard49_terms() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c contracts%ROWTYPE; proposed_end date;
BEGIN
 IF TG_OP<>'INSERT' AND OLD.status='APPROVED' THEN RAISE EXCEPTION 'Approved terms are immutable' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 SELECT * INTO c FROM contracts WHERE id=NEW.contract_id;
 PERFORM id FROM rooms WHERE id=c.room_id FOR UPDATE;
 PERFORM id FROM contracts WHERE id=c.id FOR UPDATE;
 IF NEW.effective_from<c.start_date THEN RAISE EXCEPTION 'Terms precede contract' USING ERRCODE='23514'; END IF;
 IF NEW.status='APPROVED' THEN
  IF EXISTS(SELECT 1 FROM contract_terms t WHERE t.contract_id=NEW.contract_id AND t.status='APPROVED' AND t.id<>NEW.id AND
  (t.version_no>=NEW.version_no OR t.effective_from>=NEW.effective_from)) THEN RAISE EXCEPTION 'New terms must follow approved versions' USING ERRCODE='23514'; END IF;
  IF EXISTS(SELECT 1 FROM billing_items i JOIN billing_documents d ON d.id=i.document_id WHERE d.contract_id=NEW.contract_id AND d.status='ISSUED' AND i.contract_term_id IS NOT NULL AND i.service_to>NEW.effective_from) THEN
  RAISE EXCEPTION 'Terms affect issued period; use linked adjustment' USING ERRCODE='23514'; END IF;
  proposed_end:=NEW.lease_end;
  IF EXISTS(SELECT 1 FROM v_contract_current x WHERE x.room_id=c.room_id AND x.id<>c.id AND x.status IN ('ACTIVE','PENDING')
   AND daterange(x.start_date,x.end_date,'[)') && daterange(c.start_date,proposed_end,'[)')) THEN
   RAISE EXCEPTION 'Contract period overlaps another contract' USING ERRCODE='23514'; END IF;
  IF EXISTS(SELECT 1 FROM reservations r WHERE r.room_id=c.room_id AND r.status='HELD' AND r.expires_at>clock_timestamp()
   AND r.id IS DISTINCT FROM c.reservation_id AND daterange(r.start_date,r.end_date,'[)') && daterange(c.start_date,proposed_end,'[)')) THEN
   RAISE EXCEPTION 'Contract conflicts with held reservation' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_terms BEFORE INSERT OR UPDATE OR DELETE ON contract_terms FOR EACH ROW EXECUTE FUNCTION guard49_terms();

CREATE FUNCTION guard49_contract() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 PERFORM id FROM rooms WHERE id=NEW.room_id FOR UPDATE;
 IF TG_OP='UPDATE' AND ROW(NEW.room_id,NEW.start_date) IS DISTINCT FROM ROW(OLD.room_id,OLD.start_date) AND
 EXISTS(SELECT 1 FROM contract_terms WHERE contract_id=OLD.id AND status='APPROVED') THEN
 RAISE EXCEPTION 'Signed contract identity cannot change' USING ERRCODE='23514'; END IF;
 IF NEW.previous_contract_id IS NOT NULL AND EXISTS(WITH RECURSIVE chain AS(SELECT id,previous_contract_id FROM contracts WHERE id=NEW.previous_contract_id
 UNION SELECT c.id,c.previous_contract_id FROM contracts c JOIN chain p ON c.id=p.previous_contract_id)SELECT 1 FROM chain WHERE id=NEW.id) THEN
 RAISE EXCEPTION 'Contract continuation cycle' USING ERRCODE='23514'; END IF;
 IF NEW.status IN ('ACTIVE','PENDING') AND EXISTS(SELECT 1 FROM v_contract_current x JOIN
 (SELECT lease_end FROM contract_terms WHERE contract_id=NEW.id AND status='APPROVED' ORDER BY effective_from DESC LIMIT 1)t ON true
 WHERE x.room_id=NEW.room_id AND x.id<>NEW.id AND x.status IN ('ACTIVE','PENDING')
 AND daterange(x.start_date,x.end_date,'[)') && daterange(NEW.start_date,t.lease_end,'[)')) THEN
 RAISE EXCEPTION 'Active contract overlaps another tenancy' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_contract49 BEFORE INSERT OR UPDATE ON contracts FOR EACH ROW EXECUTE FUNCTION guard49_contract();

CREATE FUNCTION check49_contract_complete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM contracts c WHERE c.id=NEW.id AND c.status IN ('ACTIVE','PENDING') AND
 (NOT EXISTS(SELECT 1 FROM contract_terms t WHERE t.contract_id=c.id AND t.status='APPROVED') OR
 NOT EXISTS(SELECT 1 FROM contract_parties p WHERE p.contract_id=c.id AND p.party_role='PRIMARY'))) THEN
 RAISE EXCEPTION 'Active contract requires approved terms and primary party' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER complete_contract49 AFTER INSERT OR UPDATE ON contracts DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check49_contract_complete();

CREATE FUNCTION guard49_residency() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c v_contract_current%ROWTYPE; cap integer; peak integer;
BEGIN
 SELECT * INTO c FROM v_contract_current WHERE id=NEW.contract_id;
 PERFORM id FROM contracts WHERE id=c.id FOR UPDATE;
 IF NEW.effective_from<c.start_date OR NEW.effective_from>=c.end_date OR (NEW.effective_to IS NOT NULL AND NEW.effective_to>c.end_date) THEN
 RAISE EXCEPTION 'Residency outside contract' USING ERRCODE='23514'; END IF;
 SELECT max_occupants INTO cap FROM rooms WHERE id=c.room_id;
 SELECT coalesce(max(n),0) INTO peak FROM(SELECT count(*) n FROM contract_residencies a
 CROSS JOIN LATERAL(SELECT DISTINCT b.effective_from d FROM contract_residencies b WHERE b.contract_id=c.id) days
 WHERE a.contract_id=c.id AND a.effective_from<=days.d AND (a.effective_to IS NULL OR a.effective_to>days.d) GROUP BY days.d) counts;
 IF peak>cap THEN RAISE EXCEPTION 'Room capacity exceeded' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER capacity49 AFTER INSERT OR UPDATE ON contract_residencies FOR EACH ROW EXECUTE FUNCTION guard49_residency();

CREATE FUNCTION guard49_possession() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE room bigint;
BEGIN
 SELECT room_id INTO room FROM contracts WHERE id=NEW.contract_id;
 PERFORM id FROM rooms WHERE id=room FOR UPDATE;
 IF EXISTS(SELECT 1 FROM room_possessions p JOIN contracts c ON c.id=p.contract_id WHERE c.room_id=room AND p.id<>NEW.id
 AND tstzrange(p.taken_at,p.returned_at,'[)') && tstzrange(NEW.taken_at,NEW.returned_at,'[)')) THEN
 RAISE EXCEPTION 'Room is still possessed by another tenancy' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' AND EXISTS(SELECT 1 FROM room_operating_periods o WHERE o.room_id=room AND o.status<>'READY'
 AND tstzrange(o.valid_from,o.valid_to,'[)') @> NEW.taken_at) THEN RAISE EXCEPTION 'Room is not ready for handover' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER possession49 BEFORE INSERT OR UPDATE ON room_possessions FOR EACH ROW EXECUTE FUNCTION guard49_possession();

CREATE FUNCTION guard49_segment() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE a meter_readings%ROWTYPE;b meter_readings%ROWTYPE; room bigint;
BEGIN
 SELECT * INTO a FROM meter_readings WHERE id=NEW.start_reading_id; SELECT * INTO b FROM meter_readings WHERE id=NEW.end_reading_id;
 IF a.meter_id<>b.meter_id OR a.observed_at>=b.observed_at OR a.reading_value>b.reading_value THEN
 RAISE EXCEPTION 'Consumption endpoints must increase on the same meter' USING ERRCODE='23514'; END IF;
 SELECT room_id INTO room FROM meters WHERE id=a.meter_id;
 PERFORM id FROM meters WHERE id=a.meter_id FOR UPDATE;
 IF NEW.contract_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM contracts WHERE id=NEW.contract_id AND room_id=room) THEN
 RAISE EXCEPTION 'Segment belongs to another room' USING ERRCODE='23514'; END IF;
 IF NEW.supersedes_segment_id IS NOT NULL AND (NOT EXISTS(SELECT 1 FROM v_segments s WHERE s.id=NEW.supersedes_segment_id AND s.meter_id=a.meter_id AND s.contract_id IS NOT DISTINCT FROM NEW.contract_id)
 OR EXISTS(SELECT 1 FROM billing_items WHERE segment_id=NEW.supersedes_segment_id)) THEN
 RAISE EXCEPTION 'Billed segment requires linked billing correction; replacement scope must match' USING ERRCODE='23514'; END IF;
 IF EXISTS(SELECT 1 FROM meter_readings n WHERE n.supersedes_reading_id IN(a.id,b.id)) THEN
 RAISE EXCEPTION 'Use current reading versions' USING ERRCODE='23514'; END IF;
 IF EXISTS(SELECT 1 FROM v_segments s WHERE s.meter_id=a.meter_id AND s.id IS DISTINCT FROM NEW.supersedes_segment_id
 AND NOT EXISTS(SELECT 1 FROM consumption_segments n WHERE n.supersedes_segment_id=s.id)
 AND tstzrange(s.service_start,s.service_end,'[)') && tstzrange(a.observed_at,b.observed_at,'[)')) THEN
 RAISE EXCEPTION 'Consumption interval overlaps an existing interval' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER segment49 BEFORE INSERT ON consumption_segments FOR EACH ROW EXECUTE FUNCTION guard49_segment();

CREATE FUNCTION guard49_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE d billing_documents%ROWTYPE; source_contract bigint;
BEGIN
 SELECT * INTO d FROM billing_documents WHERE id=CASE WHEN TG_OP='DELETE' THEN OLD.document_id ELSE NEW.document_id END FOR UPDATE;
 IF d.status<>'DRAFT' THEN RAISE EXCEPTION 'Issued billing lines are immutable' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 IF NEW.adjusts_item_id IS NOT NULL THEN
 SELECT bd.contract_id INTO source_contract FROM billing_items i JOIN billing_documents bd ON bd.id=i.document_id WHERE i.id=NEW.adjusts_item_id;
 IF d.document_type<>'ADJUSTMENT' OR d.contract_id IS DISTINCT FROM source_contract THEN RAISE EXCEPTION 'Adjustment scope invalid' USING ERRCODE='23514'; END IF;
 IF NOT EXISTS(SELECT 1 FROM v_item_balances WHERE id=NEW.adjusts_item_id AND document_status='ISSUED') THEN
 RAISE EXCEPTION 'Adjustment must target original issued debit' USING ERRCODE='23514'; END IF;
 END IF;
 IF NEW.segment_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM consumption_segments WHERE id=NEW.segment_id AND contract_id=d.contract_id) THEN
 RAISE EXCEPTION 'Meter segment does not belong to billed contract' USING ERRCODE='23514'; END IF;
 IF NEW.contract_term_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM contract_terms WHERE id=NEW.contract_term_id AND contract_id=d.contract_id) THEN
 RAISE EXCEPTION 'Terms do not belong to billed contract' USING ERRCODE='23514'; END IF;
 IF NEW.subscription_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM contract_service_subscriptions WHERE id=NEW.subscription_id AND contract_id=d.contract_id) THEN
 RAISE EXCEPTION 'Subscription does not belong to billed contract' USING ERRCODE='23514'; END IF;
 IF NEW.handover_item_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM handover_items i JOIN handovers h ON h.id=i.handover_id WHERE i.id=NEW.handover_item_id AND h.contract_id=d.contract_id) THEN RAISE EXCEPTION 'Handover charge scope differs' USING ERRCODE='23514'; END IF;
 IF NEW.violation_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM contract_violations WHERE id=NEW.violation_id AND contract_id=d.contract_id) THEN RAISE EXCEPTION 'Violation charge scope differs' USING ERRCODE='23514'; END IF;
 IF NEW.maintenance_cost_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM maintenance_costs cost JOIN v_maintenance m ON m.id=cost.request_id JOIN contracts c ON c.id=d.contract_id JOIN rooms r ON r.id=c.room_id WHERE cost.id=NEW.maintenance_cost_id AND (m.resolved_room_id=c.room_id OR (m.resolved_room_id IS NULL AND m.resolved_property_id=r.property_id))) THEN RAISE EXCEPTION 'Maintenance cost scope differs' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER item49 BEFORE INSERT OR UPDATE OR DELETE ON billing_items FOR EACH ROW EXECUTE FUNCTION guard49_item();

CREATE FUNCTION guard49_document() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP<>'INSERT' AND OLD.status='ISSUED' THEN RAISE EXCEPTION 'Issued documents are immutable' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 IF TG_OP='INSERT' AND NEW.status<>'DRAFT' THEN RAISE EXCEPTION 'Create draft before issue' USING ERRCODE='23514'; END IF;
 IF NEW.status='ISSUED' THEN
  PERFORM b.id FROM billing_items b WHERE b.id IN(SELECT adjusts_item_id FROM billing_items WHERE document_id=NEW.id) ORDER BY b.id FOR UPDATE;
  IF EXISTS(SELECT 1 FROM v_item_balances b JOIN
   (SELECT adjusts_item_id,sum(CASE direction WHEN 'DEBIT' THEN subtotal ELSE -subtotal END) delta FROM v_billing_lines WHERE document_id=NEW.id AND adjusts_item_id IS NOT NULL GROUP BY adjusts_item_id)a
   ON a.adjusts_item_id=b.id WHERE b.charge_amount+a.delta<0) THEN
  RAISE EXCEPTION 'Adjustment cannot reduce charge below zero' USING ERRCODE='23514'; END IF;
  IF NEW.issue_date>(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date OR NOT EXISTS(SELECT 1 FROM billing_items WHERE document_id=NEW.id) THEN
  RAISE EXCEPTION 'Issue date or missing lines prevents issue' USING ERRCODE='23514'; END IF;
  IF EXISTS(SELECT 1 FROM billing_items WHERE document_id=NEW.id AND verification_status IN ('PENDING','DISPUTED')) OR
  EXISTS(SELECT 1 FROM billing_items i JOIN consumption_alerts a ON a.segment_id=i.segment_id WHERE i.document_id=NEW.id AND a.status='PENDING') THEN
  RAISE EXCEPTION 'Unresolved verification or consumption alert' USING ERRCODE='23514'; END IF;
  NEW.issued_at:=clock_timestamp();
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER document49 BEFORE INSERT OR UPDATE OR DELETE ON billing_documents FOR EACH ROW EXECUTE FUNCTION guard49_document();

CREATE FUNCTION guard49_entry() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE source v_fund_balances%ROWTYPE; target fund_accounts%ROWTYPE; line v_item_balances%ROWTYPE;
 original fund_entries%ROWTYPE; cash cash_transactions%ROWTYPE; credit v_billing_lines%ROWTYPE; used numeric; allowed numeric; cid bigint;
BEGIN
 -- Every writer locks affected funds in stable ID order before re-reading totals.
 IF NEW.original_entry_id IS NOT NULL THEN SELECT * INTO original FROM fund_entries WHERE id=NEW.original_entry_id; END IF;
 PERFORM id FROM fund_accounts WHERE id IN(NEW.from_fund_id,NEW.to_fund_id,original.from_fund_id,original.to_fund_id) ORDER BY id FOR UPDATE;
 IF NEW.from_fund_id IS NOT NULL THEN
  SELECT * INTO source FROM v_fund_balances WHERE id=NEW.from_fund_id;
  IF source.reconciliation_status<>'VERIFIED' THEN RAISE EXCEPTION 'Fund requires reconciliation before use' USING ERRCODE='23514'; END IF;
  IF source.available<NEW.amount THEN RAISE EXCEPTION 'Insufficient available funds' USING ERRCODE='23514'; END IF;
 END IF;
 IF NEW.to_fund_id IS NOT NULL THEN SELECT * INTO target FROM fund_accounts WHERE id=NEW.to_fund_id; END IF;
 IF NEW.cash_transaction_id IS NOT NULL THEN
  SELECT * INTO cash FROM cash_transactions WHERE id=NEW.cash_transaction_id FOR UPDATE;
  IF cash.status<>'CONFIRMED' OR (NEW.entry_type='RECEIVE' AND cash.direction<>'IN') OR (NEW.entry_type='REFUND' AND cash.direction<>'OUT') THEN
  RAISE EXCEPTION 'Cash must be confirmed with matching direction' USING ERRCODE='23514'; END IF;
  SELECT coalesce(sum(amount),0) INTO used FROM fund_entries WHERE cash_transaction_id=cash.id;
  IF used+NEW.amount>cash.amount THEN RAISE EXCEPTION 'Cash allocation exceeds actual cash' USING ERRCODE='23514'; END IF;
 END IF;
 IF NEW.entry_type='PAYMENT' THEN
  SELECT * INTO line FROM v_item_balances WHERE id=NEW.debit_item_id;
  PERFORM id FROM billing_items WHERE id=NEW.debit_item_id FOR UPDATE;
  SELECT * INTO line FROM v_item_balances WHERE id=NEW.debit_item_id;
  IF NOT FOUND OR line.document_status<>'ISSUED' OR line.verification_status NOT IN ('VERIFIED','ESTIMATED') OR line.balance_due<NEW.amount THEN
  RAISE EXCEPTION 'Allocation exceeds eligible debt or targets draft' USING ERRCODE='23514'; END IF;
  IF source.contract_id IS DISTINCT FROM line.contract_id OR source.reservation_id IS DISTINCT FROM line.reservation_id THEN
  RAISE EXCEPTION 'Fund and charge scopes differ; transfer explicitly first' USING ERRCODE='23514'; END IF;
 END IF;
 IF NEW.entry_type='TRANSFER' AND source.contract_id IS NOT NULL AND target.contract_id IS NOT NULL AND source.contract_id<>target.contract_id
 AND source.beneficiary_tenant_id IS DISTINCT FROM target.beneficiary_tenant_id THEN
 RAISE EXCEPTION 'Cannot transfer ownership without a separate agreement' USING ERRCODE='23514'; END IF;
 IF NEW.entry_type IN ('CREDIT_TO_FUND','ADJUSTMENT_RELEASE') THEN
  SELECT * INTO credit FROM v_billing_lines WHERE id=NEW.credit_item_id;
  PERFORM id FROM billing_items WHERE id=NEW.credit_item_id FOR UPDATE;
  IF credit.direction<>'CREDIT' OR credit.document_status<>'ISSUED' OR target.contract_id IS DISTINCT FROM credit.contract_id THEN
  RAISE EXCEPTION 'Invalid credit source or fund scope' USING ERRCODE='23514'; END IF;
  SELECT coalesce(sum(e.amount-coalesce((SELECT sum(r.amount) FROM fund_entries r WHERE r.entry_type='REVERSAL' AND r.original_entry_id=e.id),0)),0)
  INTO used FROM fund_entries e WHERE e.credit_item_id=credit.id;
  IF used+NEW.amount>credit.subtotal THEN RAISE EXCEPTION 'Credit already consumed' USING ERRCODE='23514'; END IF;
  IF NEW.entry_type='CREDIT_TO_FUND' AND credit.adjusts_item_id IS NOT NULL THEN RAISE EXCEPTION 'Adjustment cannot also mint fund credit' USING ERRCODE='23514'; END IF;
  IF NEW.entry_type='ADJUSTMENT_RELEASE' THEN
   IF original.entry_type<>'PAYMENT' OR original.debit_item_id IS DISTINCT FROM credit.adjusts_item_id OR target.id<>original.from_fund_id THEN
   RAISE EXCEPTION 'Release must restore original payment source' USING ERRCODE='23514'; END IF;
   SELECT overpayment INTO allowed FROM v_item_balances WHERE id=original.debit_item_id;
   IF allowed<NEW.amount THEN RAISE EXCEPTION 'Release exceeds actual overpayment' USING ERRCODE='23514'; END IF;
   SELECT coalesce(sum(CASE WHEN e.entry_type IN ('REVERSAL','ADJUSTMENT_RELEASE') THEN e.amount ELSE 0 END),0)
    -coalesce((SELECT sum(r.amount) FROM fund_entries r JOIN fund_entries e ON e.id=r.original_entry_id WHERE r.entry_type='REVERSAL' AND e.entry_type='ADJUSTMENT_RELEASE' AND e.original_entry_id=original.id),0)
    INTO used FROM fund_entries e WHERE e.original_entry_id=original.id;
   IF used+NEW.amount>original.amount THEN RAISE EXCEPTION 'Release exceeds remaining original payment' USING ERRCODE='23514'; END IF;
  END IF;
 END IF;
 IF NEW.entry_type='REVERSAL' THEN
  IF original.entry_type NOT IN ('PAYMENT','TRANSFER','CREDIT_TO_FUND','ADJUSTMENT_RELEASE') THEN RAISE EXCEPTION 'Cash corrections require explicit cash reconciliation' USING ERRCODE='23514'; END IF;
  SELECT coalesce(sum(amount),0) INTO used FROM fund_entries WHERE entry_type='REVERSAL' AND original_entry_id=original.id;
  IF used+NEW.amount>original.amount THEN RAISE EXCEPTION 'Reversal exceeds original entry' USING ERRCODE='23514'; END IF;
  IF original.entry_type='PAYMENT' THEN
   SELECT coalesce(sum(e.amount),0)-coalesce((SELECT sum(r.amount) FROM fund_entries r JOIN fund_entries e ON e.id=r.original_entry_id WHERE r.entry_type='REVERSAL' AND e.entry_type='ADJUSTMENT_RELEASE' AND e.original_entry_id=original.id),0)
    INTO allowed FROM fund_entries e WHERE e.entry_type='ADJUSTMENT_RELEASE' AND e.original_entry_id=original.id;
   IF used+NEW.amount+allowed>original.amount THEN RAISE EXCEPTION 'Reverse only unreturned payment amount' USING ERRCODE='23514'; END IF;
  END IF;
  IF original.to_fund_id IS NOT NULL AND (SELECT available FROM v_fund_balances WHERE id=original.to_fund_id)<NEW.amount THEN
  RAISE EXCEPTION 'Reverse dependent uses first' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER entry49 BEFORE INSERT ON fund_entries FOR EACH ROW EXECUTE FUNCTION guard49_entry();

CREATE FUNCTION check49_cash_allocated() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE cid bigint;expected numeric;actual numeric;st text;
BEGIN
 IF TG_TABLE_NAME='cash_transactions' THEN cid:=NEW.id; ELSE cid:=NEW.cash_transaction_id; END IF;
 IF cid IS NULL THEN RETURN NULL; END IF;
 SELECT amount,status INTO expected,st FROM cash_transactions WHERE id=cid;
 SELECT coalesce(sum(amount),0) INTO actual FROM fund_entries WHERE cash_transaction_id=cid;
 IF st='CONFIRMED' AND actual<>expected THEN RAISE EXCEPTION 'Confirmed cash must be fully classified, including unapplied money' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER cash_complete49 AFTER INSERT OR UPDATE ON cash_transactions DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check49_cash_allocated();
CREATE CONSTRAINT TRIGGER entry_cash_complete49 AFTER INSERT ON fund_entries DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check49_cash_allocated();

CREATE FUNCTION guard49_hold() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 PERFORM id FROM fund_accounts WHERE id=NEW.fund_account_id FOR UPDATE;
 IF NEW.released_at IS NULL AND NEW.amount>coalesce((SELECT balance FROM v_fund_balances WHERE id=NEW.fund_account_id),0)
 -coalesce((SELECT sum(amount) FROM fund_holds WHERE fund_account_id=NEW.fund_account_id AND released_at IS NULL AND id<>NEW.id),0) THEN
 RAISE EXCEPTION 'Hold exceeds fund balance' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER hold49 BEFORE INSERT OR UPDATE ON fund_holds FOR EACH ROW EXECUTE FUNCTION guard49_hold();

CREATE FUNCTION guard49_parent_locked() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE locked boolean;
BEGIN
 IF TG_TABLE_NAME='handover_items' THEN SELECT status='CONFIRMED' INTO locked FROM handovers WHERE id=CASE WHEN TG_OP='DELETE' THEN OLD.handover_id ELSE NEW.handover_id END FOR UPDATE;
 ELSIF TG_TABLE_NAME='handovers' THEN locked:=OLD.status='CONFIRMED';
 ELSIF TG_TABLE_NAME='checkout_records' THEN locked:=OLD.status IN ('APPROVED','LEGACY_RECORDED');
 END IF;
 IF locked THEN RAISE EXCEPTION 'Confirmed record is immutable' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;RETURN NEW;
END $$;
CREATE TRIGGER handover_lock49 BEFORE UPDATE OR DELETE ON handovers FOR EACH ROW EXECUTE FUNCTION guard49_parent_locked();
CREATE TRIGGER handover_item_lock49 BEFORE INSERT OR UPDATE OR DELETE ON handover_items FOR EACH ROW EXECUTE FUNCTION guard49_parent_locked();
CREATE TRIGGER checkout_lock49 BEFORE UPDATE OR DELETE ON checkout_records FOR EACH ROW EXECUTE FUNCTION guard49_parent_locked();

CREATE FUNCTION guard49_cash_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.status IN ('CONFIRMED','VOID') THEN RAISE EXCEPTION 'Confirmed/void cash is immutable; record an explicit correction' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;RETURN NEW;
END $$;
CREATE TRIGGER cash_immutable49 BEFORE UPDATE OR DELETE ON cash_transactions FOR EACH ROW EXECUTE FUNCTION guard49_cash_immutable();

CREATE FUNCTION guard49_reading() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE m meters%ROWTYPE; p meter_readings%ROWTYPE;
BEGIN
 SELECT * INTO m FROM meters WHERE id=NEW.meter_id FOR UPDATE;
 IF NEW.observed_at<m.installed_at OR NEW.observed_at>coalesce(m.retired_at,'infinity') THEN RAISE EXCEPTION 'Reading outside meter life' USING ERRCODE='23514'; END IF;
 IF NEW.supersedes_reading_id IS NOT NULL THEN
  SELECT * INTO p FROM meter_readings WHERE id=NEW.supersedes_reading_id;
  IF p.meter_id<>NEW.meter_id OR p.observed_at<>NEW.observed_at OR NEW.revision_no<>p.revision_no+1 OR nullif(btrim(NEW.reason),'') IS NULL THEN
  RAISE EXCEPTION 'Invalid reading revision' USING ERRCODE='23514'; END IF;
  IF EXISTS(SELECT 1 FROM consumption_segments s JOIN billing_items b ON b.segment_id=s.id WHERE p.id IN(s.start_reading_id,s.end_reading_id)) THEN
  RAISE EXCEPTION 'Billed reading cannot be revised; use financial adjustment' USING ERRCODE='23514'; END IF;
 ELSIF NEW.revision_no<>1 THEN RAISE EXCEPTION 'Revision chain required' USING ERRCODE='23514'; END IF;
 IF EXISTS(SELECT 1 FROM meter_readings r WHERE r.meter_id=NEW.meter_id AND NOT EXISTS(SELECT 1 FROM meter_readings n WHERE n.supersedes_reading_id=r.id)
 AND ((r.observed_at<NEW.observed_at AND r.reading_value>NEW.reading_value) OR (r.observed_at>NEW.observed_at AND r.reading_value<NEW.reading_value))) THEN
 RAISE EXCEPTION 'Reading must be monotonic on one physical meter' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER reading49 BEFORE INSERT ON meter_readings FOR EACH ROW EXECUTE FUNCTION guard49_reading();

CREATE FUNCTION guard49_party() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.party_role='PRIMARY' AND EXISTS(SELECT 1 FROM contract_terms WHERE contract_id=OLD.contract_id AND status='APPROVED') THEN
 RAISE EXCEPTION 'Signed primary party is immutable; use successor contract' USING ERRCODE='23514'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;RETURN NEW;
END $$;
CREATE TRIGGER party49 BEFORE UPDATE OR DELETE ON contract_parties FOR EACH ROW EXECUTE FUNCTION guard49_party();

CREATE FUNCTION guard49_reservation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 PERFORM id FROM rooms WHERE id=NEW.room_id FOR UPDATE;
 IF NEW.status='HELD' AND NEW.expires_at>clock_timestamp() AND (
 EXISTS(SELECT 1 FROM reservations r WHERE r.room_id=NEW.room_id AND r.id<>NEW.id AND r.status='HELD' AND r.expires_at>clock_timestamp() AND daterange(r.start_date,r.end_date,'[)') && daterange(NEW.start_date,NEW.end_date,'[)')) OR
 EXISTS(SELECT 1 FROM v_contract_current c WHERE c.room_id=NEW.room_id AND c.reservation_id IS DISTINCT FROM NEW.id AND c.status IN ('ACTIVE','PENDING') AND daterange(c.start_date,c.end_date,'[)') && daterange(NEW.start_date,NEW.end_date,'[)'))) THEN
 RAISE EXCEPTION 'Reservation conflicts with held or contracted period' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER reservation49 BEFORE INSERT OR UPDATE ON reservations FOR EACH ROW EXECUTE FUNCTION guard49_reservation();

CREATE FUNCTION guard49_handover_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.possession_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM room_possessions WHERE id=NEW.possession_id AND contract_id=NEW.contract_id) THEN RAISE EXCEPTION 'Possession belongs to another contract' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER handover_scope49 BEFORE INSERT OR UPDATE ON handovers FOR EACH ROW EXECUTE FUNCTION guard49_handover_scope();

CREATE FUNCTION guard49_alert_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_TABLE_NAME='alert_policies' THEN
  IF EXISTS(SELECT 1 FROM consumption_alerts WHERE policy_id=OLD.id) THEN RAISE EXCEPTION 'Used alert policy is immutable; add effective version' USING ERRCODE='23514'; END IF;
 ELSE
  IF EXISTS(SELECT 1 FROM billing_items i JOIN billing_documents d ON d.id=i.document_id WHERE i.segment_id=OLD.segment_id AND d.status='ISSUED') THEN RAISE EXCEPTION 'Issued alert history is immutable' USING ERRCODE='23514'; END IF;
  IF TG_OP='UPDATE' AND ROW(NEW.segment_id,NEW.policy_id,NEW.baseline_daily_average,NEW.applied_threshold) IS DISTINCT FROM ROW(OLD.segment_id,OLD.policy_id,OLD.baseline_daily_average,OLD.applied_threshold) THEN RAISE EXCEPTION 'Baseline is immutable; create a linked evaluation' USING ERRCODE='23514'; END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;RETURN NEW;
END $$;
CREATE TRIGGER alert_policy49 BEFORE UPDATE OR DELETE ON alert_policies FOR EACH ROW EXECUTE FUNCTION guard49_alert_history();
CREATE TRIGGER alert_history49 BEFORE UPDATE OR DELETE ON consumption_alerts FOR EACH ROW EXECUTE FUNCTION guard49_alert_history();
CREATE TRIGGER baseline_history49 BEFORE UPDATE OR DELETE ON alert_baseline_sources FOR EACH ROW EXECUTE FUNCTION guard49_append_only();
CREATE TRIGGER maintenance_history49 BEFORE UPDATE OR DELETE ON maintenance_events FOR EACH ROW EXECUTE FUNCTION guard49_append_only();
CREATE TRIGGER file_identity49 BEFORE UPDATE OR DELETE ON stored_files FOR EACH ROW EXECUTE FUNCTION guard49_append_only();
CREATE TRIGGER file_link_history49 BEFORE UPDATE OR DELETE ON file_attachments FOR EACH ROW EXECUTE FUNCTION guard49_append_only();

CREATE FUNCTION guard49_fund_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(OLD.fund_type,OLD.contract_id,OLD.reservation_id,OLD.unapplied_receipt_id,OLD.beneficiary_tenant_id) IS DISTINCT FROM
 ROW(NEW.fund_type,NEW.contract_id,NEW.reservation_id,NEW.unapplied_receipt_id,NEW.beneficiary_tenant_id) THEN RAISE EXCEPTION 'Fund ownership is immutable; transfer with a ledger entry' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER fund_identity49 BEFORE UPDATE ON fund_accounts FOR EACH ROW EXECUTE FUNCTION guard49_fund_identity();
