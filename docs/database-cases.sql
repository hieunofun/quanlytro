-- Chỉ chạy trên database kiểm tra trống đã nạp quanlytro.sql và database-rules.sql.
-- Mặc định rollback toàn bộ fixture. -v keep_data=1 chỉ dành cho bộ kiểm tra đồng thời.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
CREATE TEMP TABLE case_results(case_name text PRIMARY KEY, passed boolean NOT NULL);
CREATE FUNCTION pg_temp.expect_failure(label text, statement text, expected_code text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  BEGIN
    EXECUTE statement;
  EXCEPTION WHEN OTHERS THEN
    IF SQLSTATE=expected_code THEN INSERT INTO case_results VALUES(label,true); RETURN; END IF;
    RAISE EXCEPTION 'Case %: expected %, got %: %',label,expected_code,SQLSTATE,SQLERRM;
  END;
  RAISE EXCEPTION 'Case % unexpectedly accepted invalid data',label;
END $$;
CREATE FUNCTION pg_temp.assert_true(label text, value boolean) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF value IS DISTINCT FROM true THEN RAISE EXCEPTION 'Case % failed',label; END IF;
  INSERT INTO case_results VALUES(label,true);
END $$;

INSERT INTO users(id,username,password_hash,role) VALUES
(1,'owner','test-hash','ADMIN'),(2,'staff','test-hash','STAFF'),
(3,'tenant-a','test-hash','TENANT'),(4,'tenant-b','test-hash','TENANT'),(5,'other-staff','test-hash','STAFF');
INSERT INTO properties(id,property_code,name,address) VALUES(1,'K1','Khu 1','Dia chi 1'),(2,'K2','Khu 2','Dia chi 2');
INSERT INTO rooms(id,property_id,room_code,base_price,max_occupants,operational_status) VALUES
(1,1,'101',3000000,3,'AVAILABLE'),(2,2,'101',1000,2,'AVAILABLE'),
(3,1,'102',3000000,2,'MAINTENANCE'),(4,1,'103',3000000,2,'AVAILABLE');
INSERT INTO employee_assignments(user_id,property_id) VALUES(2,1),(5,2);
INSERT INTO tenant_profiles(id,user_id,full_name,identity_number) VALUES
(1,3,'Nguoi thue A','111'),(2,NULL,'Thanh vien A','222'),(3,4,'Nguoi thue B','333'),(4,NULL,'Thanh vien B','444');
INSERT INTO contracts(id,contract_code,room_id,primary_tenant_id,start_date,end_date,rental_price,deposit_amount)
SELECT v.id,v.code,v.room_id,v.tenant_id,date_trunc('month',CURRENT_DATE)::date-interval '1 month',
  date_trunc('month',CURRENT_DATE)::date,v.price,3000000
FROM (VALUES(1,'HD1',1,1,3000000),(2,'HD2',2,3,1000),(3,'HD3',4,3,3000000)) v(id,code,room_id,tenant_id,price);
INSERT INTO contract_members(contract_id,tenant_id,effective_from,effective_to)
SELECT 1,2,end_date-15,end_date FROM contracts WHERE id=1;
INSERT INTO service_items(id,service_code,name,calculation_type,unit_name,is_mandatory) VALUES
(1,'ELECTRICITY','Dien','METER_BASED','kWh',true),(2,'WATER','Nuoc','METER_BASED','m3',true),
(3,'INTERNET','Internet','PER_ROOM','thang',true),(4,'PERSON','Dau nguoi','PER_PERSON','nguoi/thang',false);
INSERT INTO service_price_configs(property_id,service_item_id,unit_price,effective_from)
SELECT p,s,price,(SELECT start_date FROM contracts WHERE id=1)
FROM (VALUES(1,1,3500),(1,2,20000),(1,3,100000),(1,4,60000),(2,1,0),(2,2,0),(2,3,0)) v(p,s,price);
INSERT INTO meter_readings(id,contract_id,service_item_id,billing_period,old_index,new_index,reading_date,recorded_by)
SELECT v.id,v.c,v.s,c.start_date,v.a,v.b,c.end_date-1,1 FROM
(VALUES(1,1,1,1000,1120),(2,1,2,120,128),(3,2,1,0,20),(4,2,2,0,1)) v(id,c,s,a,b) JOIN contracts c ON c.id=v.c;
INSERT INTO invoices(id,invoice_code,contract_id,billing_period,issue_date,due_date)
SELECT id,'INV-'||id,id,start_date,CURRENT_DATE,CURRENT_DATE+5 FROM contracts WHERE id IN (1,2);
INSERT INTO invoice_items(invoice_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,quantity)
SELECT id,'Tien phong','thang',rental_price,end_date-start_date FROM contracts WHERE id IN (1,2);
INSERT INTO invoice_items(invoice_id,meter_reading_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot)
VALUES(1,1,'Dien','kWh',3500),(1,2,'Nuoc','m3',20000),(2,3,'Dien','kWh',0),(2,4,'Nuoc','m3',0);
INSERT INTO invoice_items(invoice_id,service_item_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,quantity)
SELECT c.id,3,'Internet','thang',CASE c.id WHEN 1 THEN 100000 ELSE 0 END,c.end_date-c.start_date FROM contracts c WHERE id IN(1,2);
DO $$ DECLARE t text; n bigint; BEGIN
  FOREACH t IN ARRAY ARRAY['users','properties','rooms','tenant_profiles','contracts','meter_readings','invoices','consumption_alerts','checkout_records','maintenance_requests'] LOOP
    EXECUTE format('SELECT COALESCE(max(id),0)+1 FROM %I',t) INTO n;
    PERFORM setval(pg_get_serial_sequence(t,'id'),n,false);
  END LOOP;
END $$;

SELECT pg_temp.assert_true('01_scope',can_manage_property(2,1) AND NOT can_manage_property(2,2) AND NOT can_manage_property(3,1));
SELECT pg_temp.expect_failure('02_bad_role',$q$INSERT INTO users(username,password_hash,role) VALUES('bad','x','SUPERUSER')$q$,'23514');
SELECT pg_temp.expect_failure('03_duplicate_room',$q$INSERT INTO rooms(property_id,room_code,base_price) VALUES(1,'101',1)$q$,'23505');
SELECT pg_temp.assert_true('04_same_code_different_property',(SELECT count(*)=2 FROM rooms WHERE room_code='101'));
SELECT pg_temp.expect_failure('05_orphan_room',$q$INSERT INTO rooms(property_id,room_code,base_price) VALUES(999,'x',1)$q$,'23503');
SELECT pg_temp.expect_failure('06_negative_price',$q$INSERT INTO rooms(property_id,room_code,base_price) VALUES(1,'x',-1)$q$,'23514');
SELECT pg_temp.expect_failure('07_assignment_nonstaff',$q$INSERT INTO employee_assignments(user_id,property_id) VALUES(3,1)$q$,'23514');
SELECT pg_temp.expect_failure('08_contract_overlap',$q$INSERT INTO contracts(contract_code,room_id,primary_tenant_id,start_date,end_date,rental_price) SELECT 'overlap',1,1,start_date,end_date,1 FROM contracts WHERE id=1$q$,'23P01');
SELECT pg_temp.expect_failure('09_maintenance_room',$q$INSERT INTO contracts(contract_code,room_id,primary_tenant_id,start_date,end_date,rental_price) SELECT 'repair',3,1,start_date,end_date,1 FROM contracts WHERE id=1$q$,'23514');
SELECT pg_temp.expect_failure('10_primary_as_member',$q$INSERT INTO contract_members(contract_id,tenant_id,effective_from) SELECT 3,3,start_date FROM contracts WHERE id=3$q$,'23514');
INSERT INTO contract_members(contract_id,tenant_id,effective_from) SELECT 3,2,start_date FROM contracts WHERE id=3;
SELECT pg_temp.expect_failure('11_capacity',$q$INSERT INTO contract_members(contract_id,tenant_id,effective_from) SELECT 3,4,start_date FROM contracts WHERE id=3$q$,'23514');
SELECT pg_temp.expect_failure('12_billed_reading_frozen',$q$UPDATE meter_readings SET new_index=1121 WHERE id=1$q$,'23514');
SELECT pg_temp.expect_failure('13_wrong_invoice_reading',$q$INSERT INTO invoice_items(invoice_id,meter_reading_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot) VALUES(1,3,'wrong','kWh',3500)$q$,'23514');
SELECT pg_temp.expect_failure('14_zero_quantity',$q$INSERT INTO invoice_items(invoice_id,service_item_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,quantity) VALUES(1,4,'person','month',60000,0)$q$,'23514');
SELECT pg_temp.assert_true('15_invoice_math',(SELECT total_amount=3680000 AND balance_due=3680000 FROM v_invoice_balances WHERE id=1));
SELECT pg_temp.assert_true('16_consumption',(SELECT consumption=120 FROM v_meter_consumption WHERE id=1));
SELECT pg_temp.expect_failure('17_duplicate_invoice',$q$INSERT INTO invoices(invoice_code,contract_id,billing_period,issue_date,due_date) SELECT 'duplicate',1,start_date,CURRENT_DATE,CURRENT_DATE+5 FROM contracts WHERE id=1$q$,'23505');
SELECT pg_temp.expect_failure('18_zero_baseline',$q$INSERT INTO consumption_alerts(meter_reading_id,baseline_average,applied_threshold_percentage,history_months) VALUES(1,0,50,3)$q$,'23514');
INSERT INTO consumption_alerts(id,meter_reading_id,baseline_average,applied_threshold_percentage,history_months) VALUES(1,1,60,50,3);
SELECT pg_temp.expect_failure('19_pending_blocks_issue',$q$UPDATE invoices SET status='ISSUED' WHERE id=1$q$,'23514');
SELECT pg_temp.expect_failure('20_tenant_cannot_review',$q$UPDATE consumption_alerts SET status='CONFIRMED',checked_by=3,checked_at=CURRENT_TIMESTAMP WHERE id=1$q$,'23514');
UPDATE consumption_alerts SET status='CONFIRMED',checked_by=1,checked_at=CURRENT_TIMESTAMP WHERE id=1;
UPDATE invoices SET status='ISSUED' WHERE id IN(1,2);
SELECT pg_temp.expect_failure('21_issued_item_frozen',$q$UPDATE invoice_items SET unit_price_snapshot=1 WHERE invoice_id=1$q$,'23514');
SELECT pg_temp.expect_failure('22_issued_reading_frozen',$q$UPDATE meter_readings SET new_index=1200 WHERE id=1$q$,'23514');
SELECT pg_temp.expect_failure('23_issued_header_frozen',$q$DELETE FROM invoices WHERE id=1$q$,'23514');
SELECT pg_temp.expect_failure('24_negative_payment',$q$INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(1,'neg',-1,'CASH',1)$q$,'23514');
SELECT pg_temp.expect_failure('25_wrong_staff_payment',$q$INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(1,'outsider',100,'CASH',5)$q$,'23514');
INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(1,'PAY-1',2000000,'CASH',2);
SELECT pg_temp.assert_true('26_partial_payment',(SELECT balance_due=1680000 AND payment_status='PARTIALLY_PAID' FROM v_invoice_balances WHERE id=1));
SELECT pg_temp.expect_failure('27_duplicate_payment',$q$INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(1,'PAY-1',100,'CASH',1)$q$,'23505');
SELECT pg_temp.expect_failure('28_overpayment',$q$INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(1,'over',2000000,'CASH',1)$q$,'23514');
SELECT pg_temp.expect_failure('29_early_deposit_offset',$q$INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(1,'early',100,'DEPOSIT_OFFSET',1)$q$,'23514');
SELECT pg_temp.expect_failure('30_wrong_tenant_request',$q$INSERT INTO maintenance_requests(contract_id,submitted_by,title,description,expected_date) VALUES(1,4,'x','x',CURRENT_DATE)$q$,'23514');
INSERT INTO maintenance_requests(id,contract_id,submitted_by,assigned_to,title,description,expected_date) VALUES(1,1,3,2,'Voi nuoc','Can kiem tra',CURRENT_DATE);
INSERT INTO maintenance_images(maintenance_request_id,url,uploaded_by) VALUES(1,'/images/a.png',3),(1,'/images/b.png',3);
SELECT pg_temp.assert_true('31_atomic_images',(SELECT count(*)=2 FROM maintenance_images WHERE maintenance_request_id=1));
SELECT pg_temp.expect_failure('32_bad_workflow',$q$UPDATE maintenance_requests SET status='COMPLETED' WHERE id=1$q$,'23514');
UPDATE contracts SET status='TERMINATED',actual_checkout_date=end_date WHERE id=1;
INSERT INTO checkout_records(id,contract_id,deposit_snapshot,outstanding_snapshot,damage_amount,settled_by) VALUES(1,1,0,0,200000,1);
SELECT pg_temp.assert_true('33_checkout_snapshot',(SELECT deposit_snapshot=3000000 AND outstanding_snapshot=1680000 AND settlement_amount=1120000 FROM v_checkout_totals WHERE id=1));
INSERT INTO payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) VALUES(1,'OFFSET-1',1680000,'DEPOSIT_OFFSET',1);
SELECT pg_temp.assert_true('34_offset_not_cash',(SELECT balance_due=0 AND paid_amount=3680000 AND cash_received=2000000 FROM v_invoice_balances WHERE id=1));
SELECT pg_temp.assert_true('35_snapshot_stable',(SELECT settlement_amount=1120000 FROM v_checkout_totals WHERE id=1));
SELECT pg_temp.expect_failure('36_checkout_once',$q$INSERT INTO checkout_records(contract_id,deposit_snapshot,outstanding_snapshot,damage_amount,settled_by) VALUES(1,0,0,0,1)$q$,'23505');
SELECT pg_temp.expect_failure('37_payment_append_only',$q$UPDATE payment_transactions SET amount=1 WHERE transaction_code='PAY-1'$q$,'23514');
SELECT pg_temp.expect_failure('38_blank_image',$q$INSERT INTO maintenance_images(maintenance_request_id,url,uploaded_by) VALUES(1,' ',3)$q$,'23514');
SELECT pg_temp.expect_failure('39_settled_contract_frozen',$q$UPDATE contracts SET actual_checkout_date=actual_checkout_date-1 WHERE id=1$q$,'23514');
SELECT pg_temp.expect_failure('40_wrong_image_uploader',$q$INSERT INTO maintenance_images(maintenance_request_id,url,uploaded_by) VALUES(1,'/wrong.png',4)$q$,'23514');
SET CONSTRAINTS ALL IMMEDIATE;
SELECT count(*) AS passed_cases, bool_and(passed) AS all_passed FROM case_results;
SELECT case_name,passed FROM case_results ORDER BY case_name;
\if :{?keep_data}
COMMIT;
\else
ROLLBACK;
\endif
