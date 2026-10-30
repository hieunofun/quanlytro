"""Produce a forward-only V11 from canonical DDL/rules and an explicit V10 import."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('schema49',ROOT/'scripts/schema49.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

PREFIX="""-- V11: approved 49-table application. V1-V10 are intentionally unchanged.
-- Atomic archival/import. Never infer an unrecorded deposit receipt or signature.
DO $$ DECLARE t record; n integer; BEGIN
 IF EXISTS(SELECT 1 FROM pg_namespace WHERE nspname='model_v10') THEN RAISE EXCEPTION 'model_v10 already exists; investigate previous cutover'; END IF;
 SELECT count(*) INTO n FROM pg_tables WHERE schemaname='public' AND tablename<>'flyway_schema_history';
 IF n<>17 THEN RAISE EXCEPTION 'Expected V10 with 17 application tables, got %',n; END IF;
 CREATE SCHEMA model_v10;
 FOR t IN SELECT viewname FROM pg_views WHERE schemaname='public' LOOP
 EXECUTE format('ALTER VIEW public.%I SET SCHEMA model_v10',t.viewname); END LOOP;
 FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename<>'flyway_schema_history' LOOP
 EXECUTE format('ALTER TABLE public.%I SET SCHEMA model_v10',t.tablename); END LOOP;
END $$;
"""

IMPORT="""
INSERT INTO users SELECT * FROM model_v10.users;
INSERT INTO tenant_profiles(id,user_id,full_name,identity_number,phone,email,date_of_birth,hometown,created_at)
 SELECT id,user_id,full_name,identity_number,phone,email,date_of_birth,hometown,created_at FROM model_v10.tenant_profiles;
INSERT INTO properties(id,property_code,name,address,status,meter_cutoff_day,invoice_issue_day,collection_timing,created_at)
 SELECT id,property_code,name,address,status,meter_cutoff_day,invoice_issue_day,'IN_ARREARS',created_at FROM model_v10.properties;
INSERT INTO rooms(id,property_id,floor_number,room_code,max_occupants,area_sqm,description,internal_note,opened_on)
 SELECT r.id,r.property_id,r.floor_number,r.room_code,r.max_occupants,r.area_sqm,r.description,r.internal_note,
 coalesce((SELECT min(c.start_date) FROM model_v10.contracts c WHERE c.room_id=r.id),CURRENT_DATE) FROM model_v10.rooms r;
INSERT INTO room_price_history(room_id,effective_from,amount) SELECT id,CURRENT_DATE,base_price FROM model_v10.rooms;
INSERT INTO room_operating_periods(room_id,valid_from,status,note)
 SELECT r.id,clock_timestamp(),CASE WHEN r.operational_status='MAINTENANCE' THEN 'MAINTENANCE'
 WHEN EXISTS(SELECT 1 FROM model_v10.contracts c WHERE c.room_id=r.id AND c.status='ACTIVE') THEN 'TURNOVER' ELSE 'READY' END,
 'V10 import: current possession/opening dates require review; no inferred handover' FROM model_v10.rooms r;
INSERT INTO employee_assignments(id,user_id,property_id,assigned_at,revoked_at)
 SELECT id,user_id,property_id,assigned_at,CASE WHEN is_active THEN NULL ELSE clock_timestamp() END FROM model_v10.employee_assignments;
INSERT INTO contracts(id,room_id,contract_code,start_date,status,reconciliation_status,created_at)
 SELECT id,room_id,contract_code,start_date,CASE status WHEN 'TERMINATED' THEN 'ENDED' WHEN 'CANCELLED' THEN 'CANCELLED' ELSE status END,
 'PENDING',created_at FROM model_v10.contracts;
INSERT INTO contract_parties(contract_id,tenant_id,party_role,effective_from,agreement_note)
 SELECT id,primary_tenant_id,'PRIMARY',start_date,'Imported V10 primary tenant; original signed document was not stored' FROM model_v10.contracts;
INSERT INTO contract_contacts(contract_id,tenant_id,effective_from,can_view_billing)
 SELECT id,primary_tenant_id,start_date,true FROM model_v10.contracts;
INSERT INTO contract_residencies(contract_id,tenant_id,effective_from,effective_to,relationship)
 SELECT contract_id,tenant_id,effective_from,effective_to,relationship FROM model_v10.contract_members;
-- No implicit primary-tenant residency and no invented physical handover.
INSERT INTO contract_terms(contract_id,version_no,effective_from,lease_end,rental_price,deposit_required,payment_day,collection_timing,terms,origin,status,created_at)
 SELECT id,1,start_date,end_date,rental_price,deposit_amount,payment_cycle_day,'IN_ARREARS',terms,'LEGACY_V10','APPROVED',created_at FROM model_v10.contracts;
INSERT INTO service_items(id,service_code,name,unit_name,calculation_type,is_mandatory)
 SELECT id,service_code,name,unit_name,calculation_type,is_mandatory FROM model_v10.service_items;
INSERT INTO service_price_configs(id,property_id,service_item_id,effective_from,unit_price)
 SELECT id,property_id,service_item_id,effective_from,unit_price FROM model_v10.service_price_configs;
INSERT INTO contract_service_subscriptions(contract_id,service_item_id,effective_from,effective_to,charge_mode)
 SELECT c.id,s.id,c.start_date,coalesce(c.actual_checkout_date,c.end_date),'PRORATED' FROM model_v10.contracts c CROSS JOIN model_v10.service_items s
 WHERE c.status<>'CANCELLED' AND EXISTS(SELECT 1 FROM model_v10.invoice_items i JOIN model_v10.invoices v ON v.id=i.invoice_id
 WHERE v.contract_id=c.id AND i.service_item_id=s.id);
INSERT INTO alert_policies(property_id,service_item_id,effective_from,history_months,threshold_percentage)
 SELECT p.id,s.id,'1900-01-01',p.alert_history_months,p.alert_threshold_percentage FROM model_v10.properties p CROSS JOIN service_items s WHERE s.calculation_type='METER_BASED';

CREATE TEMP TABLE v11_reading_map(old_id bigint PRIMARY KEY,segment_id bigint NOT NULL) ON COMMIT DROP;
DO $$ DECLARE x record; mid bigint; start_id bigint; end_id bigint; seg bigint; opening_at timestamptz;closing_at timestamptz; prev record;v numeric; BEGIN
 FOR x IN SELECT m.*,c.room_id,c.start_date FROM model_v10.meter_readings m JOIN model_v10.contracts c ON c.id=m.contract_id
 ORDER BY c.room_id,m.service_item_id,m.billing_period,c.start_date,m.id LOOP
  SELECT id INTO mid FROM meters WHERE room_id=x.room_id AND service_item_id=x.service_item_id;
  IF mid IS NULL THEN INSERT INTO meters(room_id,service_item_id,meter_code,installed_at)
   VALUES(x.room_id,x.service_item_id,'LEGACY-R'||x.room_id||'-S'||x.service_item_id,
   (least(x.start_date,x.billing_period)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh')) RETURNING id INTO mid; END IF;
  closing_at:=((x.reading_date+1)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh');
  SELECT * INTO prev FROM meter_readings WHERE meter_id=mid AND observed_at<closing_at ORDER BY observed_at DESC LIMIT 1;
  opening_at:=(greatest(x.start_date,x.billing_period)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh');
  IF prev.id IS NOT NULL AND prev.reading_value=x.old_index THEN opening_at:=prev.observed_at; END IF;
  IF opening_at>=closing_at THEN RAISE EXCEPTION 'V10 reading % has no representable time interval',x.id; END IF;
  SELECT id,reading_value INTO start_id,v FROM meter_readings WHERE meter_id=mid AND observed_at=opening_at;
  IF start_id IS NOT NULL AND v<>x.old_index THEN RAISE EXCEPTION 'V10 opening indices conflict at reading %; reconcile first',x.id; END IF;
  IF start_id IS NULL THEN INSERT INTO meter_readings(meter_id,observed_at,reading_value,time_precision,recorded_by,reason)
   VALUES(mid,opening_at,x.old_index,'LEGACY_DAY_BOUNDARY',x.recorded_by,'V10 opening index; timestamp is inferred from calendar dates') RETURNING id INTO start_id; END IF;
  SELECT id,reading_value INTO end_id,v FROM meter_readings WHERE meter_id=mid AND observed_at=closing_at;
  IF end_id IS NOT NULL AND v<>x.new_index THEN RAISE EXCEPTION 'V10 closing indices conflict at reading %; reconcile first',x.id; END IF;
  IF end_id IS NULL THEN INSERT INTO meter_readings(meter_id,observed_at,reading_value,time_precision,recorded_by,reason)
   VALUES(mid,closing_at,x.new_index,'LEGACY_DAY_BOUNDARY',x.recorded_by,'V10 closing index; exact measurement time was not stored') RETURNING id INTO end_id; END IF;
  INSERT INTO consumption_segments(start_reading_id,end_reading_id,contract_id,responsibility,recorded_by)
   VALUES(start_id,end_id,x.contract_id,'TENANT',x.recorded_by) RETURNING id INTO seg;
  INSERT INTO v11_reading_map VALUES(x.id,seg);
 END LOOP;
END $$;
INSERT INTO billing_documents(id,contract_id,document_code,document_type,billing_period,issue_date,due_date,status,issued_at,reason)
 SELECT id,contract_id,invoice_code,'MONTHLY',billing_period,issue_date,due_date,status,issued_at,'Imported V10 invoice; source retained in model_v10' FROM model_v10.invoices;
INSERT INTO billing_items(id,document_id,line_no,item_kind,contract_term_id,segment_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,
 calculation_basis,quantity_snapshot,calculation_period,service_from,service_to,source_key,basis_note)
 SELECT i.id,i.invoice_id,row_number() OVER(PARTITION BY i.invoice_id ORDER BY i.id),
 CASE WHEN i.meter_reading_id IS NOT NULL THEN 'UTILITY' WHEN i.service_item_id IS NULL THEN 'ROOM_RENT' ELSE 'SERVICE' END,
 CASE WHEN i.service_item_id IS NULL AND i.meter_reading_id IS NULL THEN t.id ELSE NULL END,map.segment_id,
 i.item_name_snapshot,i.unit_name_snapshot,i.unit_price_snapshot,
 CASE WHEN i.meter_reading_id IS NOT NULL THEN 'METER' ELSE 'PRORATED' END,i.quantity,v.billing_period,
 greatest(v.billing_period,c.start_date),least((v.billing_period+interval '1 month')::date,coalesce(c.actual_checkout_date,c.end_date)),
 'V10-LINE-'||i.id,'Immutable V10 billing input; no retrospectively invented subscription or handover'
 FROM model_v10.invoice_items i JOIN model_v10.invoices v ON v.id=i.invoice_id JOIN model_v10.contracts c ON c.id=v.contract_id
 JOIN contract_terms t ON t.contract_id=c.id LEFT JOIN v11_reading_map map ON map.old_id=i.meter_reading_id;

INSERT INTO cash_accounts(property_id,account_code,name,account_type,is_active)
 SELECT id,'LEGACY-'||id,'V10 receipts: original receiving account was not stored','CASH',false FROM properties;
INSERT INTO fund_accounts(fund_code,fund_type,contract_id,beneficiary_tenant_id,reconciliation_status)
 SELECT 'DEPOSIT-'||id,'DEPOSIT',id,primary_tenant_id,'PENDING' FROM model_v10.contracts;
INSERT INTO fund_accounts(fund_code,fund_type,contract_id,beneficiary_tenant_id)
 SELECT 'ADVANCE-'||id,'ADVANCE',id,primary_tenant_id FROM model_v10.contracts;
-- Historical offsets are reconstructed only to preserve already-posted applications.
-- This does NOT confirm any remaining deposit or create a historical cash receipt.
DO $$ DECLARE x record; f bigint;actor bigint; BEGIN
 FOR x IN SELECT c.id,coalesce((SELECT sum(p.amount) FROM model_v10.payment_transactions p JOIN model_v10.invoices i ON i.id=p.invoice_id
 WHERE i.contract_id=c.id AND p.payment_method='DEPOSIT_OFFSET'),0)+coalesce((SELECT least(deposit_snapshot,damage_amount) FROM model_v10.checkout_records WHERE contract_id=c.id),0) used
 FROM model_v10.contracts c LOOP
 IF x.used>0 THEN
 SELECT id INTO f FROM fund_accounts WHERE contract_id=x.id AND fund_type='DEPOSIT';
 SELECT id INTO actor FROM users ORDER BY (role='ADMIN') DESC,id LIMIT 1;
 INSERT INTO fund_entries(entry_code,entry_type,to_fund_id,amount,reason,posted_by)
 VALUES('V10-OFFSET-HISTORY-'||x.id,'OPENING_BALANCE',f,x.used,'V10 already-posted offsets only; remaining deposit UNVERIFIED',actor);
 END IF; END LOOP;
END $$;

CREATE TEMP TABLE v11_payment_map(old_id bigint,entry_id bigint) ON COMMIT DROP;
DO $$ DECLARE p record; f bigint; cashid bigint;account bigint;remaining numeric;it record;amt numeric;entryid bigint;BEGIN
 FOR p IN SELECT pay.*,v.contract_id,r.property_id FROM model_v10.payment_transactions pay JOIN model_v10.invoices v ON v.id=pay.invoice_id
 JOIN model_v10.contracts c ON c.id=v.contract_id JOIN rooms r ON r.id=c.room_id ORDER BY pay.id LOOP
 SELECT id INTO f FROM fund_accounts WHERE contract_id=p.contract_id AND fund_type=CASE WHEN p.payment_method='DEPOSIT_OFFSET' THEN 'DEPOSIT' ELSE 'ADVANCE' END;
 IF p.payment_method<>'DEPOSIT_OFFSET' THEN
  SELECT id INTO account FROM cash_accounts WHERE property_id=p.property_id;
  INSERT INTO cash_transactions(cash_account_id,transaction_code,direction,amount,method,occurred_at,status,recorded_by,confirmed_by,confirmed_at,reason)
  VALUES(account,p.transaction_code,'IN',p.amount,p.payment_method,p.payment_date,'CONFIRMED',p.received_by,p.received_by,p.payment_date,'Imported recorded V10 cash; receiving account unknown') RETURNING id INTO cashid;
  INSERT INTO fund_entries(entry_code,entry_type,cash_transaction_id,to_fund_id,amount,posted_by,posted_at)
  VALUES('V10-RECEIVE-'||p.id,'RECEIVE',cashid,f,p.amount,p.received_by,p.payment_date);
 END IF;
 remaining:=p.amount;
 FOR it IN SELECT * FROM v_item_balances WHERE document_id=p.invoice_id AND balance_due>0 ORDER BY id LOOP
  amt:=least(remaining,it.balance_due);EXIT WHEN amt<=0;
  INSERT INTO fund_entries(entry_code,entry_type,from_fund_id,debit_item_id,amount,posted_by,posted_at,reason)
  VALUES('V10-PAY-'||p.id||'-'||it.id,'PAYMENT',f,it.id,amt,p.received_by,p.payment_date,'V10 allocation; original transaction retained in model_v10') RETURNING id INTO entryid;
  INSERT INTO v11_payment_map VALUES(p.id,entryid);remaining:=remaining-amt;
 END LOOP;
 IF remaining<>0 THEN RAISE EXCEPTION 'V10 payment % cannot be fully reconciled',p.id; END IF;
 END LOOP;
END $$;

SELECT setval(pg_get_serial_sequence('billing_documents','id'),greatest(coalesce((SELECT max(id) FROM billing_documents),0),1),EXISTS(SELECT 1 FROM billing_documents));
SELECT setval(pg_get_serial_sequence('billing_items','id'),greatest(coalesce((SELECT max(id) FROM billing_items),0),1),EXISTS(SELECT 1 FROM billing_items));
DO $$ DECLARE x record;d bigint;line bigint;f bigint;amt numeric;BEGIN
 FOR x IN SELECT oldx.*,c.start_date FROM model_v10.checkout_records oldx JOIN model_v10.contracts c ON c.id=oldx.contract_id LOOP
 INSERT INTO checkout_records(id,contract_id,billing_end,status,deposit_snapshot,outstanding_snapshot,notes)
 SELECT x.id,x.contract_id,actual_checkout_date,'LEGACY_RECORDED',x.deposit_snapshot,x.outstanding_snapshot,
 coalesce(x.notes,'')||' [V10 book deposit is NOT verified cash; actual refund was not recorded]' FROM model_v10.contracts WHERE id=x.contract_id;
 IF x.damage_amount>0 THEN
  INSERT INTO billing_documents(contract_id,checkout_id,document_code,document_type,issue_date,due_date,status,issued_at,reason)
  VALUES(x.contract_id,x.id,'V10-DAMAGE-'||x.id,'COMPENSATION',x.settled_at::date,x.settled_at::date,'ISSUED',x.settled_at,'Legacy aggregate, itemized handover evidence unavailable') RETURNING id INTO d;
  INSERT INTO billing_items(document_id,line_no,item_kind,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,calculation_basis,quantity_snapshot,service_from,service_to,source_key,basis_note)
  VALUES(d,1,'DAMAGE','V10 aggregate damage (legacy evidence)', 'Khoản',x.damage_amount,'FIXED',1,x.start_date,x.start_date+1,'V10-DAMAGE-'||x.id,'Imported amount only; not a new finding of responsibility') RETURNING id INTO line;
  amt:=least(x.deposit_snapshot,x.damage_amount);
  IF amt>0 THEN
   SELECT id INTO f FROM fund_accounts WHERE contract_id=x.contract_id AND fund_type='DEPOSIT';
   INSERT INTO fund_entries(entry_code,entry_type,from_fund_id,debit_item_id,amount,posted_by,posted_at,reason)
   VALUES('V10-DAMAGE-OFFSET-'||x.id,'PAYMENT',f,line,amt,x.settled_by,x.settled_at,'Reconstruct original V10 damage deduction, remaining deposit unknown');
  END IF;
 END IF; END LOOP;
END $$;

INSERT INTO maintenance_requests(id,room_id,submitted_by,assigned_to,title,description,priority,status,expected_date,created_at)
 SELECT m.id,c.room_id,m.submitted_by,m.assigned_to,m.title,m.description,m.priority,m.status,m.expected_date,m.created_at
 FROM model_v10.maintenance_requests m JOIN model_v10.contracts c ON c.id=m.contract_id;
INSERT INTO maintenance_events(request_id,action,actor_id,note,recorded_at)
 SELECT id,'LEGACY_IMPORT',submitted_by,coalesce(progress_note,'')||' [Earlier intermediate events were not retained]',created_at FROM model_v10.maintenance_requests;
INSERT INTO maintenance_costs(request_id,description,amount,incurred_on,recorded_by)
 SELECT id,'V10 recorded aggregate repair cost',actual_cost,created_at::date,coalesce(assigned_to,submitted_by) FROM model_v10.maintenance_requests WHERE actual_cost>0;
DO $$ DECLARE x record;f bigint;BEGIN
 FOR x IN SELECT * FROM model_v10.maintenance_images ORDER BY id LOOP
 INSERT INTO stored_files(storage_key,original_name,content_type,external_url,uploaded_by,uploaded_at)
 VALUES('legacy-image-'||x.id,'legacy-image','application/octet-stream',x.url,x.uploaded_by,x.created_at) RETURNING id INTO f;
 INSERT INTO file_attachments(file_id,maintenance_request_id,attached_by,created_at) VALUES(f,x.maintenance_request_id,x.uploaded_by,x.created_at);
 END LOOP;
END $$;

INSERT INTO audit_events(actor_type,action,resource_type,resource_id,after_data,reason)
 SELECT 'SYSTEM','MIGRATE_V10','contracts',id::text,jsonb_build_object('source','model_v10','reconciliation','PENDING'),
 'Signature, actual possession, implicit primary residency and remaining deposit must be reviewed; no cash/signature invented' FROM contracts;
DO $$ DECLARE t record; BEGIN
 FOR t IN SELECT table_name FROM information_schema.columns WHERE table_schema='public' AND column_name='id' AND is_identity='YES' LOOP
 EXECUTE format('SELECT setval(pg_get_serial_sequence(%L,%L),greatest(coalesce((SELECT max(id) FROM %I),0),1),coalesce((SELECT max(id) FROM %I),0)>0)',t.table_name,'id',t.table_name,t.table_name);
 END LOOP;
END $$;
"""

RECONCILE="""
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM model_v10.v_invoice_lines old JOIN v_billing_lines n ON n.id=old.id WHERE n.subtotal<>old.subtotal) OR
 EXISTS(SELECT 1 FROM model_v10.v_invoice_balances old LEFT JOIN v_document_balances n ON n.id=old.id
 WHERE n.id IS NULL OR n.total_amount<>old.total_amount OR n.paid_amount<>old.paid_amount OR n.balance_due<>old.balance_due) THEN
 RAISE EXCEPTION 'V11 invoice reconciliation failed; entire migration rolled back'; END IF;
 IF (SELECT coalesce(sum(amount),0) FROM cash_transactions WHERE direction='IN') <>
 (SELECT coalesce(sum(amount),0) FROM model_v10.payment_transactions WHERE payment_method<>'DEPOSIT_OFFSET') THEN
 RAISE EXCEPTION 'V11 cash reconciliation failed'; END IF;
END $$;
CREATE FUNCTION model_v10.reject_writes() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 RAISE EXCEPTION 'model_v10 is a read-only historical archive'; END $$;
DO $$ DECLARE t record;BEGIN
 FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='model_v10' LOOP
 EXECUTE format('CREATE TRIGGER aa_archive_read_only BEFORE INSERT OR UPDATE OR DELETE ON model_v10.%I FOR EACH ROW EXECUTE FUNCTION model_v10.reject_writes()',t.tablename);
 END LOOP;
END $$;
"""

def main():
    ddl=module.generate()
    rules=(ROOT/'docs/database-rules49.sql').read_text(encoding='utf-8')
    # Import requires the calculating views, before installing write guards.
    cut=rules.index('CREATE OR REPLACE FUNCTION can_manage_property')
    views,guards=rules[:cut],rules[cut:]
    text=PREFIX+'\n'+ddl+'\n'+views+'\n'+IMPORT+'\n'+RECONCILE+'\n'+guards
    (ROOT/'src/main/resources/db/migration/V11__approved_49_table_model.sql').write_text(text,encoding='utf-8')
    (ROOT/'docs/database-rules.sql').write_text(rules,encoding='utf-8')
    print('Generated V11 migration: 49 tables, V10 read-only archive, explicit reconciliation.')

if __name__=='__main__':main()
