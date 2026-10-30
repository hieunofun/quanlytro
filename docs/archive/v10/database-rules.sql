-- View tính toán và ràng buộc liên bảng cho quanlytro.sql.
CREATE VIEW v_meter_consumption AS
SELECT m.*, c.room_id, r.property_id, s.service_code, s.unit_name,
       m.new_index - m.old_index AS consumption
FROM meter_readings m JOIN contracts c ON c.id=m.contract_id
JOIN rooms r ON r.id=c.room_id JOIN service_items s ON s.id=m.service_item_id;

CREATE VIEW v_invoice_lines AS
SELECT x.*, COALESCE(x.service_item_id,m.service_item_id) AS resolved_service_id,
       COALESCE(m.new_index-m.old_index,x.quantity::numeric) AS billed_quantity,
       CASE WHEN x.meter_reading_id IS NOT NULL
         THEN round(x.unit_price_snapshot*(m.new_index-m.old_index),2)
         ELSE round(x.unit_price_snapshot*x.quantity /
           extract(day FROM i.billing_period + interval '1 month - 1 day'),2)
       END AS subtotal
FROM invoice_items x JOIN invoices i ON i.id=x.invoice_id
LEFT JOIN meter_readings m ON m.id=x.meter_reading_id;

CREATE VIEW v_invoice_balances AS
SELECT i.*, COALESCE(l.total_amount,0)::numeric(14,2) AS total_amount,
       COALESCE(p.paid_amount,0)::numeric(14,2) AS paid_amount,
       (COALESCE(l.total_amount,0)-COALESCE(p.paid_amount,0))::numeric(14,2) AS balance_due,
       COALESCE(p.cash_received,0)::numeric(14,2) AS cash_received,
       CASE WHEN COALESCE(p.paid_amount,0)>=COALESCE(l.total_amount,0) THEN 'PAID'
            WHEN COALESCE(p.paid_amount,0)=0 THEN 'UNPAID' ELSE 'PARTIALLY_PAID' END AS payment_status
FROM invoices i
LEFT JOIN (SELECT invoice_id,sum(subtotal) total_amount FROM v_invoice_lines GROUP BY invoice_id) l ON l.invoice_id=i.id
LEFT JOIN (SELECT invoice_id,sum(amount) paid_amount,
  sum(amount) FILTER (WHERE payment_method<>'DEPOSIT_OFFSET') cash_received
  FROM payment_transactions GROUP BY invoice_id) p ON p.invoice_id=i.id;

CREATE VIEW v_consumption_alerts AS
SELECT a.*, v.contract_id, v.room_id, v.service_item_id, v.consumption,
  round((v.consumption-a.baseline_average)*100/a.baseline_average,2) AS percentage_increase,
  CASE WHEN (v.consumption-a.baseline_average)*100/a.baseline_average >= 2*a.applied_threshold_percentage
       THEN 'CRITICAL' ELSE 'WARNING' END AS alert_severity
FROM consumption_alerts a JOIN v_meter_consumption v ON v.id=a.meter_reading_id;

CREATE VIEW v_checkout_totals AS
SELECT x.*, c.actual_checkout_date AS checkout_date,
       x.deposit_snapshot-x.damage_amount-x.outstanding_snapshot AS settlement_amount
FROM checkout_records x JOIN contracts c ON c.id=x.contract_id;

CREATE VIEW v_room_status AS
SELECT r.*, CASE WHEN operational_status='MAINTENANCE' THEN 'MAINTENANCE'
  WHEN EXISTS(SELECT 1 FROM contracts c WHERE c.room_id=r.id AND c.status<>'CANCELLED'
    AND daterange(c.start_date,COALESCE(c.actual_checkout_date,c.end_date),'[)') @>
      (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date)
  THEN 'RENTED' ELSE 'AVAILABLE' END AS display_status
FROM rooms r;

CREATE FUNCTION can_manage_property(actor bigint, target_property bigint) RETURNS boolean
LANGUAGE sql STABLE AS $$
  SELECT EXISTS(SELECT 1 FROM users u WHERE u.id=actor AND u.status='ACTIVE' AND
    (u.role='ADMIN' OR (u.role='STAFF' AND EXISTS(SELECT 1 FROM employee_assignments a
      WHERE a.user_id=u.id AND a.property_id=target_property AND a.is_active))));
$$;

CREATE FUNCTION guard_assignment() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM users WHERE id=NEW.user_id AND role='STAFF') THEN
    RAISE EXCEPTION 'Assignments require STAFF' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_assignment BEFORE INSERT OR UPDATE ON employee_assignments
FOR EACH ROW EXECUTE FUNCTION guard_assignment();

CREATE FUNCTION guard_contract() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE r rooms%ROWTYPE;
BEGIN
  SELECT * INTO r FROM rooms WHERE id=NEW.room_id FOR UPDATE;
  IF NOT FOUND THEN RETURN NEW; END IF;
  IF TG_OP='UPDATE' AND EXISTS(SELECT 1 FROM checkout_records WHERE contract_id=OLD.id) THEN
    RAISE EXCEPTION 'Settled contract is immutable' USING ERRCODE='23514';
  END IF;
  IF NEW.status IN ('ACTIVE','PENDING') AND
    (r.operational_status='MAINTENANCE' OR EXISTS(SELECT 1 FROM properties WHERE id=r.property_id AND status<>'ACTIVE')) THEN
    RAISE EXCEPTION 'Property/room unavailable' USING ERRCODE='23514';
  END IF;
  IF TG_OP='UPDATE' AND EXISTS(SELECT 1 FROM invoices WHERE contract_id=OLD.id AND status='ISSUED') AND
    ROW(NEW.room_id,NEW.primary_tenant_id,NEW.start_date,NEW.end_date,NEW.rental_price,NEW.deposit_amount)
    IS DISTINCT FROM ROW(OLD.room_id,OLD.primary_tenant_id,OLD.start_date,OLD.end_date,OLD.rental_price,OLD.deposit_amount) THEN
    RAISE EXCEPTION 'Issued contract terms are immutable' USING ERRCODE='23514';
  END IF;
  IF NEW.status='CANCELLED' AND EXISTS(SELECT 1 FROM invoices WHERE contract_id=NEW.id) THEN
    RAISE EXCEPTION 'Cannot cancel a billed contract' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_contract BEFORE INSERT OR UPDATE ON contracts FOR EACH ROW EXECUTE FUNCTION guard_contract();

CREATE FUNCTION guard_member() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c contracts%ROWTYPE; limit_date date; b record; old_days integer; new_days integer;
BEGIN
  SELECT * INTO c FROM contracts WHERE id=CASE WHEN TG_OP='DELETE' THEN OLD.contract_id ELSE NEW.contract_id END FOR UPDATE;
  IF NOT FOUND THEN RETURN NEW; END IF;
  limit_date := COALESCE(c.actual_checkout_date,c.end_date);
  IF TG_OP='UPDATE' AND ROW(NEW.contract_id,NEW.tenant_id) IS DISTINCT FROM ROW(OLD.contract_id,OLD.tenant_id) THEN
    RAISE EXCEPTION 'Membership identity is immutable' USING ERRCODE='23514';
  END IF;
  IF TG_OP<>'DELETE' AND (NEW.tenant_id=c.primary_tenant_id OR NEW.effective_from<c.start_date OR
    NEW.effective_from>=limit_date OR (NEW.effective_to IS NOT NULL AND NEW.effective_to>limit_date)) THEN
    RAISE EXCEPTION 'Member/occupancy interval invalid' USING ERRCODE='23514';
  END IF;
  FOR b IN SELECT billing_period FROM invoices WHERE contract_id=c.id LOOP
    old_days := 0; new_days := 0;
    IF TG_OP<>'INSERT' THEN
      old_days := GREATEST(0,LEAST(COALESCE(OLD.effective_to,limit_date),limit_date,(b.billing_period+interval '1 month')::date)
        -GREATEST(OLD.effective_from,c.start_date,b.billing_period));
    END IF;
    IF TG_OP<>'DELETE' THEN
      new_days := GREATEST(0,LEAST(COALESCE(NEW.effective_to,limit_date),limit_date,(b.billing_period+interval '1 month')::date)
        -GREATEST(NEW.effective_from,c.start_date,b.billing_period));
    END IF;
    IF old_days<>new_days THEN RAISE EXCEPTION 'Membership change affects existing invoice' USING ERRCODE='23514'; END IF;
  END LOOP;
  IF TG_OP='DELETE' THEN RETURN OLD; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_member BEFORE INSERT OR UPDATE OR DELETE ON contract_members FOR EACH ROW EXECUTE FUNCTION guard_member();

CREATE FUNCTION check_member_capacity() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE capacity integer; peak integer;
BEGIN
  SELECT r.max_occupants INTO capacity FROM contracts c JOIN rooms r ON r.id=c.room_id WHERE c.id=NEW.contract_id;
  SELECT COALESCE(max(1+(SELECT count(*) FROM contract_members m
    WHERE m.contract_id=NEW.contract_id AND m.effective_from<=d.day AND (m.effective_to IS NULL OR m.effective_to>d.day))),1)
    INTO peak FROM (SELECT effective_from AS day FROM contract_members WHERE contract_id=NEW.contract_id) d;
  IF peak>capacity THEN RAISE EXCEPTION 'Room capacity exceeded' USING ERRCODE='23514'; END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_member_capacity AFTER INSERT OR UPDATE ON contract_members FOR EACH ROW EXECUTE FUNCTION check_member_capacity();

CREATE FUNCTION guard_reading() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c contracts%ROWTYPE; p bigint; previous numeric;
BEGIN
  IF TG_OP IN ('UPDATE','DELETE') THEN
    IF EXISTS(SELECT 1 FROM invoices WHERE contract_id=OLD.contract_id AND billing_period=OLD.billing_period) OR
       EXISTS(SELECT 1 FROM meter_readings WHERE contract_id=OLD.contract_id AND service_item_id=OLD.service_item_id AND billing_period>OLD.billing_period) THEN
      RAISE EXCEPTION 'Only latest, unbilled reading may change' USING ERRCODE='23514';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    IF ROW(NEW.contract_id,NEW.service_item_id,NEW.billing_period,NEW.old_index)
      IS DISTINCT FROM ROW(OLD.contract_id,OLD.service_item_id,OLD.billing_period,OLD.old_index) THEN
      RAISE EXCEPTION 'Reading identity and opening index are immutable' USING ERRCODE='23514';
    END IF;
    DELETE FROM consumption_alerts WHERE meter_reading_id=OLD.id;
  END IF;
  SELECT * INTO c FROM contracts WHERE id=NEW.contract_id FOR UPDATE;
  IF NOT FOUND THEN RETURN NEW; END IF;
  SELECT property_id INTO p FROM rooms WHERE id=c.room_id;
  IF NOT can_manage_property(NEW.recorded_by,p) OR
    NOT EXISTS(SELECT 1 FROM service_items WHERE id=NEW.service_item_id AND calculation_type='METER_BASED') OR
    NEW.billing_period>=COALESCE(c.actual_checkout_date,c.end_date) OR NEW.billing_period+interval '1 month'<=c.start_date THEN
    RAISE EXCEPTION 'Reading scope/type/period invalid' USING ERRCODE='23514';
  END IF;
  SELECT new_index INTO previous FROM meter_readings WHERE contract_id=c.id
    AND service_item_id=NEW.service_item_id AND billing_period<NEW.billing_period ORDER BY billing_period DESC LIMIT 1;
  IF previous IS NOT NULL AND NEW.old_index<>previous THEN
    RAISE EXCEPTION 'Opening index does not match previous closing index' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_reading BEFORE INSERT OR UPDATE OR DELETE ON meter_readings FOR EACH ROW EXECUTE FUNCTION guard_reading();

CREATE FUNCTION guard_alert() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE m meter_readings%ROWTYPE; p bigint;
BEGIN
  SELECT * INTO m FROM meter_readings WHERE id=CASE WHEN TG_OP='DELETE' THEN OLD.meter_reading_id ELSE NEW.meter_reading_id END;
  IF FOUND AND EXISTS(SELECT 1 FROM invoices WHERE contract_id=m.contract_id AND billing_period=m.billing_period AND status='ISSUED') THEN
    RAISE EXCEPTION 'Issued period alert is immutable' USING ERRCODE='23514';
  END IF;
  IF TG_OP='DELETE' THEN RETURN OLD; END IF;
  IF TG_OP='INSERT' AND NEW.status<>'PENDING' THEN RAISE EXCEPTION 'Alert starts pending' USING ERRCODE='23514'; END IF;
  SELECT r.property_id INTO p FROM contracts c JOIN rooms r ON r.id=c.room_id WHERE c.id=m.contract_id;
  IF NEW.status<>'PENDING' AND NOT can_manage_property(NEW.checked_by,p) THEN
    RAISE EXCEPTION 'Alert reviewer outside property' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_alert BEFORE INSERT OR UPDATE OR DELETE ON consumption_alerts FOR EACH ROW EXECUTE FUNCTION guard_alert();

CREATE FUNCTION guard_invoice_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE i invoices%ROWTYPE; m meter_readings%ROWTYPE;
BEGIN
  SELECT * INTO i FROM invoices WHERE id=CASE WHEN TG_OP='DELETE' THEN OLD.invoice_id ELSE NEW.invoice_id END FOR UPDATE;
  IF NOT FOUND AND TG_OP='DELETE' THEN RETURN OLD; END IF;
  IF i.status<>'DRAFT' THEN RAISE EXCEPTION 'Issued invoice items are immutable' USING ERRCODE='23514'; END IF;
  IF TG_OP='DELETE' THEN RETURN OLD; END IF;
  IF TG_OP='UPDATE' AND NEW.invoice_id<>OLD.invoice_id THEN
    RAISE EXCEPTION 'Cannot move invoice item' USING ERRCODE='23514';
  END IF;
  IF NEW.meter_reading_id IS NOT NULL THEN
    SELECT * INTO m FROM meter_readings WHERE id=NEW.meter_reading_id;
    IF FOUND AND (m.contract_id<>i.contract_id OR m.billing_period<>i.billing_period) THEN
      RAISE EXCEPTION 'Reading must match invoice contract and period' USING ERRCODE='23514';
    END IF;
  ELSIF EXISTS(SELECT 1 FROM service_items WHERE id=NEW.service_item_id AND calculation_type='METER_BASED') THEN
    RAISE EXCEPTION 'Meter service requires a reading reference' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_invoice_item BEFORE INSERT OR UPDATE OR DELETE ON invoice_items FOR EACH ROW EXECUTE FUNCTION guard_invoice_item();

CREATE FUNCTION guard_invoice() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c contracts%ROWTYPE; p bigint; occupied integer; people_days integer; row_item record; price numeric;
BEGIN
  IF TG_OP='DELETE' THEN
    IF OLD.status='ISSUED' THEN RAISE EXCEPTION 'Issued invoice cannot be deleted' USING ERRCODE='23514'; END IF;
    RETURN OLD;
  END IF;
  IF TG_OP='INSERT' AND NEW.status<>'DRAFT' THEN RAISE EXCEPTION 'Create DRAFT first' USING ERRCODE='23514'; END IF;
  IF TG_OP='UPDATE' AND OLD.status='ISSUED' THEN RAISE EXCEPTION 'Issued invoice is immutable' USING ERRCODE='23514'; END IF;
  SELECT * INTO c FROM contracts WHERE id=NEW.contract_id;
  IF NOT FOUND THEN RETURN NEW; END IF;
  occupied := LEAST(COALESCE(c.actual_checkout_date,c.end_date),(NEW.billing_period+interval '1 month')::date)
    - GREATEST(c.start_date,NEW.billing_period);
  IF occupied<=0 OR c.status IN ('PENDING','CANCELLED') THEN
    RAISE EXCEPTION 'No billable occupancy' USING ERRCODE='23514';
  END IF;
  IF NEW.status='ISSUED' THEN
    IF NEW.issue_date>(CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date OR
      EXISTS(SELECT 1 FROM consumption_alerts a JOIN meter_readings m ON m.id=a.meter_reading_id
        WHERE m.contract_id=c.id AND m.billing_period=NEW.billing_period AND a.status='PENDING') THEN
      RAISE EXCEPTION 'Issue date or pending alerts block issuance' USING ERRCODE='23514';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM invoice_items WHERE invoice_id=NEW.id AND service_item_id IS NULL AND meter_reading_id IS NULL) THEN
      RAISE EXCEPTION 'Room charge required' USING ERRCODE='23514';
    END IF;
    SELECT property_id INTO p FROM rooms WHERE id=c.room_id;
    SELECT occupied+COALESCE(sum(GREATEST(0,
      LEAST(COALESCE(effective_to,COALESCE(c.actual_checkout_date,c.end_date)),COALESCE(c.actual_checkout_date,c.end_date),(NEW.billing_period+interval '1 month')::date)
      -GREATEST(effective_from,c.start_date,NEW.billing_period))),0) INTO people_days
      FROM contract_members WHERE contract_id=c.id;
    IF EXISTS(SELECT 1 FROM service_items s WHERE s.is_mandatory AND
      NOT EXISTS(SELECT 1 FROM v_invoice_lines l WHERE l.invoice_id=NEW.id AND l.resolved_service_id=s.id)) THEN
      RAISE EXCEPTION 'Mandatory service charge missing' USING ERRCODE='23514';
    END IF;
    FOR row_item IN SELECT l.*,s.calculation_type FROM v_invoice_lines l
      LEFT JOIN service_items s ON s.id=l.resolved_service_id WHERE l.invoice_id=NEW.id LOOP
      IF row_item.resolved_service_id IS NULL THEN
        IF row_item.quantity<>occupied OR row_item.unit_price_snapshot<>c.rental_price THEN
          RAISE EXCEPTION 'Room amount must match contract and days' USING ERRCODE='23514';
        END IF;
      ELSE
        SELECT unit_price INTO price FROM service_price_configs WHERE property_id=p
          AND service_item_id=row_item.resolved_service_id AND effective_from<=NEW.billing_period ORDER BY effective_from DESC LIMIT 1;
        IF price IS NULL OR row_item.unit_price_snapshot<>price THEN
          RAISE EXCEPTION 'Missing or wrong effective price' USING ERRCODE='23514';
        END IF;
        IF (row_item.calculation_type='PER_ROOM' AND row_item.quantity<>occupied) OR
           (row_item.calculation_type='PER_PERSON' AND row_item.quantity<>people_days) THEN
          RAISE EXCEPTION 'Wrong occupied/person days' USING ERRCODE='23514';
        END IF;
      END IF;
    END LOOP;
    NEW.issued_at := CURRENT_TIMESTAMP;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_invoice BEFORE INSERT OR UPDATE OR DELETE ON invoices FOR EACH ROW EXECUTE FUNCTION guard_invoice();

CREATE FUNCTION guard_payment() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE i invoices%ROWTYPE; available numeric; p bigint; x checkout_records%ROWTYPE;
BEGIN
  IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'Payments are append-only' USING ERRCODE='23514'; END IF;
  SELECT * INTO i FROM invoices WHERE id=NEW.invoice_id FOR UPDATE;
  IF NOT FOUND THEN RETURN NEW; END IF;
  SELECT r.property_id INTO p FROM contracts c JOIN rooms r ON r.id=c.room_id WHERE c.id=i.contract_id;
  IF i.status<>'ISSUED' OR NOT can_manage_property(NEW.received_by,p) THEN
    RAISE EXCEPTION 'Payment scope/status invalid' USING ERRCODE='23514';
  END IF;
  SELECT balance_due INTO available FROM v_invoice_balances WHERE id=i.id;
  IF NEW.amount>available THEN RAISE EXCEPTION 'Payment exceeds debt' USING ERRCODE='23514'; END IF;
  IF NEW.payment_method='DEPOSIT_OFFSET' THEN
    SELECT * INTO x FROM checkout_records WHERE contract_id=i.contract_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Checkout required for deposit offset' USING ERRCODE='23514'; END IF;
    SELECT GREATEST(x.deposit_snapshot-x.damage_amount,0)-COALESCE(sum(t.amount),0) INTO available
      FROM payment_transactions t JOIN invoices j ON j.id=t.invoice_id
      WHERE j.contract_id=i.contract_id AND t.payment_method='DEPOSIT_OFFSET';
    IF NEW.amount>available THEN RAISE EXCEPTION 'Deposit offset exceeds available deposit' USING ERRCODE='23514'; END IF;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_payment BEFORE INSERT OR UPDATE OR DELETE ON payment_transactions FOR EACH ROW EXECUTE FUNCTION guard_payment();

CREATE FUNCTION guard_checkout() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE c contracts%ROWTYPE; p bigint; month_day date;
BEGIN
  IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'Checkout is immutable' USING ERRCODE='23514'; END IF;
  SELECT * INTO c FROM contracts WHERE id=NEW.contract_id FOR UPDATE;
  IF NOT FOUND THEN RETURN NEW; END IF;
  SELECT property_id INTO p FROM rooms WHERE id=c.room_id;
  IF c.status<>'TERMINATED' OR NOT can_manage_property(NEW.settled_by,p) THEN
    RAISE EXCEPTION 'Checkout requires terminated contract and authorized manager' USING ERRCODE='23514';
  END IF;
  PERFORM id FROM invoices WHERE contract_id=c.id ORDER BY id FOR UPDATE;
  FOR month_day IN SELECT d::date FROM generate_series(date_trunc('month',c.start_date)::date,
    date_trunc('month',c.actual_checkout_date-1)::date,interval '1 month') d LOOP
    IF NOT EXISTS(SELECT 1 FROM invoices WHERE contract_id=c.id AND billing_period=month_day AND status='ISSUED') THEN
      RAISE EXCEPTION 'All occupied months must be billed' USING ERRCODE='23514';
    END IF;
    IF EXISTS(SELECT 1 FROM invoices i JOIN invoice_items l ON l.invoice_id=i.id
      WHERE i.contract_id=c.id AND i.billing_period=month_day AND l.service_item_id IS NULL AND l.meter_reading_id IS NULL
      AND l.quantity<>(LEAST(c.actual_checkout_date,(month_day+interval '1 month')::date)-GREATEST(c.start_date,month_day))) THEN
      RAISE EXCEPTION 'Issued days do not match checkout' USING ERRCODE='23514';
    END IF;
  END LOOP;
  NEW.deposit_snapshot := c.deposit_amount;
  SELECT COALESCE(sum(balance_due),0) INTO NEW.outstanding_snapshot FROM v_invoice_balances WHERE contract_id=c.id AND status='ISSUED';
  RETURN NEW;
END $$;
CREATE TRIGGER trg_checkout BEFORE INSERT OR UPDATE OR DELETE ON checkout_records FOR EACH ROW EXECUTE FUNCTION guard_checkout();

CREATE FUNCTION check_checkout_pair() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF EXISTS(SELECT 1 FROM contracts c WHERE c.id=NEW.id AND c.status='TERMINATED'
    AND NOT EXISTS(SELECT 1 FROM checkout_records x WHERE x.contract_id=c.id)) THEN
    RAISE EXCEPTION 'Terminated contract requires checkout in same transaction' USING ERRCODE='23514';
  END IF;
  RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER trg_checkout_pair AFTER INSERT OR UPDATE ON contracts
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_checkout_pair();

CREATE FUNCTION guard_maintenance() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE owner_id bigint; p bigint;
BEGIN
  SELECT t.user_id,r.property_id INTO owner_id,p FROM contracts c
    JOIN tenant_profiles t ON t.id=c.primary_tenant_id JOIN rooms r ON r.id=c.room_id WHERE c.id=NEW.contract_id;
  IF FOUND AND (NEW.submitted_by IS DISTINCT FROM owner_id) AND NOT can_manage_property(NEW.submitted_by,p) THEN
    RAISE EXCEPTION 'Maintenance request outside user contract' USING ERRCODE='23514';
  END IF;
  IF NEW.assigned_to IS NOT NULL AND NOT can_manage_property(NEW.assigned_to,p) THEN
    RAISE EXCEPTION 'Assignee outside property' USING ERRCODE='23514';
  END IF;
  IF TG_OP='UPDATE' AND ROW(NEW.contract_id,NEW.submitted_by) IS DISTINCT FROM ROW(OLD.contract_id,OLD.submitted_by) THEN
    RAISE EXCEPTION 'Request ownership is immutable' USING ERRCODE='23514';
  END IF;
  IF TG_OP='UPDATE' AND NEW.status<>OLD.status AND NOT
    ((OLD.status='NEW' AND NEW.status='ACCEPTED') OR (OLD.status='ACCEPTED' AND NEW.status='IN_PROGRESS') OR
     (OLD.status='IN_PROGRESS' AND NEW.status='COMPLETED')) THEN
    RAISE EXCEPTION 'Invalid maintenance transition' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_maintenance BEFORE INSERT OR UPDATE ON maintenance_requests FOR EACH ROW EXECUTE FUNCTION guard_maintenance();

CREATE FUNCTION guard_maintenance_image() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE requester bigint; p bigint;
BEGIN
  SELECT m.submitted_by,r.property_id INTO requester,p FROM maintenance_requests m
    JOIN contracts c ON c.id=m.contract_id JOIN rooms r ON r.id=c.room_id WHERE m.id=NEW.maintenance_request_id;
  IF FOUND AND NEW.uploaded_by<>requester AND NOT can_manage_property(NEW.uploaded_by,p) THEN
    RAISE EXCEPTION 'Image uploader outside request' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_maintenance_image BEFORE INSERT OR UPDATE ON maintenance_images FOR EACH ROW EXECUTE FUNCTION guard_maintenance_image();
