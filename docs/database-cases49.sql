-- Execute only after quanlytro.sql and database-rules.sql (through V13) in a disposable database.
BEGIN;
CREATE TEMP TABLE results49(name text PRIMARY KEY);
CREATE FUNCTION pg_temp.expect49(label text,statement text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
 BEGIN EXECUTE statement;
 EXCEPTION WHEN check_violation OR exclusion_violation OR foreign_key_violation OR unique_violation THEN
  INSERT INTO results49 VALUES(label); RETURN;
 END;
 RAISE EXCEPTION 'Expected rejection: %',label;
END $$;
CREATE FUNCTION pg_temp.assert49(label text,ok boolean) RETURNS void LANGUAGE plpgsql AS $$
BEGIN IF ok IS DISTINCT FROM true THEN RAISE EXCEPTION 'Failed: %',label; END IF;INSERT INTO results49 VALUES(label);END $$;
INSERT INTO users(id,username,password_hash,role) VALUES(1,'fixture49','test-only','ADMIN');
INSERT INTO properties(id,property_code,name,address) VALUES(1,'P','Fixture','Local');
INSERT INTO rooms(id,property_id,room_code,max_occupants,opened_on) VALUES(1,1,'101',2,'2026-01-01');
INSERT INTO tenant_profiles(id,full_name) VALUES(1,'Primary'),(2,'Resident'),(3,'Third');
INSERT INTO contracts(id,room_id,contract_code,start_date,created_by) VALUES(1,1,'C','2026-01-01',1);
INSERT INTO contract_parties(contract_id,tenant_id,party_role,effective_from) VALUES(1,1,'PRIMARY','2026-01-01');
INSERT INTO contract_terms(contract_id,version_no,effective_from,lease_end,rental_price,signed_at,approved_by,status)
VALUES(1,1,'2026-01-01','2027-01-01',1000,'2025-12-31T10:00:00+07:00',1,'APPROVED');
UPDATE contracts SET status='ACTIVE' WHERE id=1;
INSERT INTO cash_accounts(id,property_id,account_code,name,account_type) VALUES(1,1,'CASH','Cash','CASH');
INSERT INTO fund_accounts(id,fund_code,fund_type,contract_id,beneficiary_tenant_id) VALUES(1,'DEP','DEPOSIT',1,1),(2,'ADV','ADVANCE',1,1);
SELECT pg_temp.assert49('signing does not receive deposit',(SELECT balance=0 FROM v_fund_balances WHERE id=1));
SELECT pg_temp.assert49('primary signer is not implicit resident',(SELECT count(*)=0 FROM contract_residencies));
INSERT INTO cash_transactions(id,cash_account_id,transaction_code,direction,amount,method,occurred_at,status,recorded_by,confirmed_by,confirmed_at)
VALUES(1,1,'RC','IN',1200,'CASH','2026-01-02T10:00:00+07:00','CONFIRMED',1,1,clock_timestamp());
INSERT INTO fund_accounts(id,fund_code,fund_type,unapplied_receipt_id) VALUES(3,'UNAPPLIED','UNAPPLIED',1);
INSERT INTO fund_entries(entry_code,entry_type,cash_transaction_id,to_fund_id,amount,posted_by) VALUES('RC-ENTRY','RECEIVE',1,3,1200,1);
INSERT INTO fund_entries(entry_code,entry_type,from_fund_id,to_fund_id,amount,posted_by) VALUES('CLASSIFY','TRANSFER',3,2,1200,1);
INSERT INTO billing_documents(id,contract_id,document_code,document_type,billing_period,issue_date,due_date,created_by) VALUES(1,1,'B','MONTHLY','2026-01-01','2026-01-03','2026-01-05',1);
INSERT INTO billing_items(id,document_id,line_no,item_kind,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,calculation_basis,quantity_snapshot,service_from,service_to)
VALUES(1,1,1,'OTHER','Verified charge','Charge',1000,'FIXED',1,'2026-01-01','2026-02-01');
UPDATE billing_documents SET status='ISSUED',issued_by=1 WHERE id=1;
INSERT INTO fund_entries(id,entry_code,entry_type,from_fund_id,debit_item_id,amount,posted_by) VALUES(3,'PAY','PAYMENT',2,1,1000,1);
SELECT pg_temp.assert49('overpayment remains held',(SELECT balance=200 FROM v_fund_balances WHERE id=2));
SELECT pg_temp.assert49('allocating money is not another cash receipt',(SELECT sum(amount)=1200 FROM cash_transactions WHERE direction='IN'));
SELECT pg_temp.expect49('cannot allocate paid debt',$q$INSERT INTO fund_entries(entry_code,entry_type,from_fund_id,debit_item_id,amount,posted_by) VALUES('OVER','PAYMENT',2,1,1,1)$q$);
SELECT pg_temp.expect49('ledger append only',$q$UPDATE fund_entries SET amount=1 WHERE entry_code='PAY'$q$);
SELECT pg_temp.expect49('issued line locked',$q$UPDATE billing_items SET unit_price_snapshot=1 WHERE id=1$q$);
SELECT pg_temp.expect49('confirmed cash locked',$q$UPDATE cash_transactions SET amount=1 WHERE id=1$q$);
SELECT pg_temp.expect49('signed primary locked',$q$DELETE FROM contract_parties WHERE contract_id=1$q$);
SELECT pg_temp.expect49('signed terms locked',$q$UPDATE contract_terms SET rental_price=1 WHERE contract_id=1$q$);
INSERT INTO contract_residencies(contract_id,tenant_id,effective_from) VALUES(1,1,'2026-01-01'),(1,2,'2026-01-01');
SELECT pg_temp.expect49('capacity counts unique date boundary',$q$INSERT INTO contract_residencies(contract_id,tenant_id,effective_from) VALUES(1,3,'2026-01-01')$q$);
INSERT INTO fund_holds(fund_account_id,amount,reason,review_on,created_by) VALUES(2,100,'Pending verification','2026-10-29',1);
SELECT pg_temp.assert49('hold reduces availability',(SELECT available=100 FROM v_fund_balances WHERE id=2));
SELECT pg_temp.expect49('hold cannot exceed balance',$q$INSERT INTO fund_holds(fund_account_id,amount,reason,review_on,created_by) VALUES(2,101,'Too much','2026-10-29',1)$q$);
SELECT pg_temp.expect49('hold history cannot be overwritten',$q$UPDATE fund_holds SET amount=50 WHERE id=(SELECT min(id) FROM fund_holds)$q$);
SELECT pg_temp.expect49('outgoing cash cannot claim invoice payment',$q$INSERT INTO cash_transactions(cash_account_id,transaction_code,direction,amount,method,occurred_at,status,recorded_by,payment_document_id) VALUES(1,'BAD-OUT','OUT',10,'CASH','2026-01-03','PENDING',1,1)$q$);
INSERT INTO service_items(id,service_code,name,unit_name,calculation_type) VALUES(10,'SQL-ELEC','Electricity','kWh','METER_BASED');
INSERT INTO meters(id,room_id,service_item_id,meter_code,installed_at,recorded_by) VALUES(1,1,10,'SQL-METER','2026-01-01T00:00:00+07:00',1);
INSERT INTO meter_readings(meter_id,observed_at,reading_value,recorded_by) VALUES(1,'2026-01-10T00:00:00+07:00',100,1);
SELECT pg_temp.expect49('backdated reading respects later measurement',$q$INSERT INTO meter_readings(meter_id,observed_at,reading_value,recorded_by) VALUES(1,'2026-01-05T00:00:00+07:00',150,1)$q$);
SELECT pg_temp.expect49('reading cannot precede meter installation',$q$INSERT INTO meter_readings(meter_id,observed_at,reading_value,recorded_by) VALUES(1,'2025-12-31T00:00:00+07:00',0,1)$q$);
SELECT pg_temp.expect49('reading cannot be recorded in future',$q$INSERT INTO meter_readings(meter_id,observed_at,reading_value,recorded_by) VALUES(1,clock_timestamp()+interval '1 day',200,1)$q$);
INSERT INTO contracts(id,room_id,contract_code,start_date,created_by) VALUES(2,1,'OTHER','2027-01-01',1);
SELECT pg_temp.expect49('adjustment header cannot link another tenancy',$q$INSERT INTO billing_documents(contract_id,document_code,document_type,related_document_id,issue_date,due_date) VALUES(2,'BAD-SCOPE','ADJUSTMENT',1,'2026-01-03','2026-01-05')$q$);
SET CONSTRAINTS ALL IMMEDIATE;
SELECT pg_temp.expect49('confirmed cash must be classified',$q$INSERT INTO cash_transactions(cash_account_id,transaction_code,direction,amount,method,occurred_at,status,recorded_by,confirmed_by,confirmed_at) VALUES(1,'MISSING','IN',1,'CASH','2026-01-02','CONFIRMED',1,1,clock_timestamp())$q$);
SELECT 'MODEL49_CASES_PASSED',count(*) FROM results49;
ROLLBACK;
