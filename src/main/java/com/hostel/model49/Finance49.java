package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@Service @Profile("model49") @Transactional
public class Finance49 {
    private final Database db;private final Access49 access;private final Events49 events;
    public Finance49(Database db,Access49 access,Events49 events){this.db=db;this.access=access;this.events=events;}
    public long ensureFund(long contract,String kind){
        var c=access.contract(contract);db.one("select id from contracts where id=? for update",contract);
        var rows=db.rows("select id from fund_accounts where contract_id=? and fund_type=?",contract,kind);
        if(!rows.isEmpty())return com.hostel.service.Database.id(rows.getFirst(),"id");
        require(c.get("primary_tenant_id")!=null,"Contract has no primary party");
        return db.insert("fund_accounts","fund_code,fund_type,contract_id,beneficiary_tenant_id",kind+"-"+contract,kind,contract,c.get("primary_tenant_id"));
    }
    public Map<String,Object> document(long id){
        var d=new LinkedHashMap<>(access.document(id));d.put("invoice_code",d.get("document_code"));
        var lines=db.rows("select * from v_billing_lines where document_id=? order by line_no",id);
        for(var l:lines){
            l.put("quantity",l.get("billed_quantity"));
            String formula=switch(l.get("calculation_basis").toString()){
                case "METER"->"("+l.get("new_index")+" − "+l.get("old_index")+") "+l.get("unit_name_snapshot")+" × "+l.get("unit_price_snapshot");
                case "PRORATED"->l.get("unit_price_snapshot")+" / "+date(l.get("calculation_period")).lengthOfMonth()+" ngày × "+l.get("billed_quantity")+" đơn vị-ngày";
                default->l.get("billed_quantity")+" "+l.get("unit_name_snapshot")+" × "+l.get("unit_price_snapshot");
            };
            l.put("formula_description",formula+" = "+l.get("subtotal")+" VND; từ "+l.get("service_from")+" đến trước "+l.get("service_to"));
        }
        if(Set.of("ADJUSTMENT","REIMBURSEMENT").contains(d.get("document_type"))){BigDecimal signed=BigDecimal.ZERO;for(var line:lines)signed=signed.add("CREDIT".equals(line.get("direction"))?money(line,"subtotal").negate():money(line,"subtotal"));d.put("total_amount",signed);}
        d.put("items",lines);d.put("adjustments",db.rows("select a.*,doc.document_code adjustment_code,doc.issue_date adjustment_date from v_billing_lines a join billing_documents doc on doc.id=a.document_id join billing_items source on source.id=a.adjusts_item_id where source.document_id=? and doc.status='ISSUED' order by doc.id,a.id",id));
        d.put("payments",db.rows("select e.id,e.entry_code transaction_code,e.amount,e.posted_at payment_date,case when f.fund_type='DEPOSIT' then 'DEPOSIT_OFFSET' else 'FUNDS_ALLOCATION' end payment_method from fund_entries e join fund_accounts f on f.id=e.from_fund_id join billing_items i on i.id=e.debit_item_id where i.document_id=? and e.entry_type='PAYMENT' order by e.id",id));return d;
    }
    public long createDocument(Map<String,Object> m){
        access.manager();Long contract=optionalId(m,"contractId","contract_id"),reservation=optionalId(m,"reservationId","reservation_id");
        if(contract!=null)access.contract(contract);else {require(reservation!=null,"Contract or reservation is required");access.room(com.hostel.service.Database.id(db.one("select room_id from reservations where id=?",reservation),"room_id"));}
        Long related=optionalId(m,"relatedDocumentId");
        if(related!=null){var source=access.document(related);require(Objects.equals(contract,source.get("contract_id"))&&Objects.equals(reservation,source.get("reservation_id")),"Related document belongs to another tenancy");}
        LocalDate issue=day(m,LocalDate.now(),"issueDate","issue_date");
        return db.insert("billing_documents","contract_id,reservation_id,checkout_id,related_document_id,document_code,document_type,billing_period,issue_date,due_date,reason,created_by",
            contract,reservation,optionalId(m,"checkoutId"),optionalId(m,"relatedDocumentId"),required(m,"code","documentCode"),text(m,"MONTHLY","documentType","document_type"),
            day(m,null,"period","billingPeriod"),issue,day(m,issue.plusDays(5),"dueDate","due_date"),text(m,"","reason"),access.user());
    }
    public long line(long document,Map<String,Object> m){
        access.manager();access.document(document);db.one("select id from billing_documents where id=? for update",document);
        var d=db.one("select * from billing_documents where id=?",document);require("DRAFT".equals(d.get("status")),"Only draft lines may change");
        LocalDate from=day(m,d.get("billing_period")==null?date(d.get("issue_date")):date(d.get("billing_period")),"serviceFrom","service_from");
        LocalDate to=day(m,from.plusMonths(1),"serviceTo","service_to");
        int n=db.jdbc.queryForObject("select coalesce(max(line_no),0)+1 from billing_items where document_id=?",Integer.class,document);
        String basis=text(m,"FIXED","calculationBasis","calculation_basis");
        return db.insert("billing_items","document_id,line_no,item_kind,direction,adjusts_item_id,contract_term_id,subscription_id,segment_id,service_price_config_id,handover_item_id,maintenance_cost_id,violation_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,calculation_basis,quantity_snapshot,calculation_period,service_from,service_to,verification_status,basis_note,source_key",
            document,n,text(m,"OTHER","itemKind","item_kind"),text(m,"DEBIT","direction"),optionalId(m,"adjustsItemId"),optionalId(m,"contractTermId"),optionalId(m,"subscriptionId"),optionalId(m,"segmentId"),optionalId(m,"servicePriceConfigId"),optionalId(m,"handoverItemId"),optionalId(m,"maintenanceCostId"),optionalId(m,"violationId"),
            required(m,"name","itemName"),text(m,"Khoản","unit","unitName"),amount(m,"0","unitPrice","unit_price"),basis,"METER".equals(basis)?null:amount(m,"1","quantity"),day(m,from.withDayOfMonth(1),"calculationPeriod"),from,to,text(m,"VERIFIED","verificationStatus"),text(m,"","basisNote","reason"),text(m,null,"sourceKey"));
    }
    public Map<String,Object> issue(long id){
        access.manager();var d=access.document(id);
        if(d.get("contract_id")!=null)db.one("select id from contracts where id=? for update",d.get("contract_id"));
        else db.one("select id from reservations where id=? for update",d.get("reservation_id"));
        d=db.one("select * from billing_documents where id=? for update",id);
        BigDecimal authorizedAmount=db.jdbc.queryForObject("select coalesce(sum(subtotal),0) from v_billing_lines where document_id=?",BigDecimal.class,id);
        if(d.get("contract_id")!=null){var c=access.contract(com.hostel.service.Database.id(d,"contract_id"));access.action(com.hostel.service.Database.id(c,"room_id"),Set.of("ADJUSTMENT","COMPENSATION","REIMBURSEMENT").contains(d.get("document_type"))?"ADJUST":"BILLING",authorizedAmount);}
        else {var r=db.one("select room_id from reservations where id=?",d.get("reservation_id"));access.action(com.hostel.service.Database.id(r,"room_id"),"ADJUST",authorizedAmount);}
        if("ISSUED".equals(d.get("status")))return document(id);
        db.jdbc.update("update billing_documents set status='ISSUED',issued_by=?,issued_at=clock_timestamp() where id=?",access.user(),id);
        events.audit("ISSUE","billing_documents",id,d,db.one("select * from billing_documents where id=?",id),"Issue immutable document");
        events.notifyContract((Number)d.get("contract_id"),"INVOICE_ISSUED","Chứng từ mới",d.get("document_code").toString(),"DOC-"+id,id);
        return document(id);
    }
    public Map<String,Object> receive(Map<String,Object> m){
        access.manager();long account=keyId(m,"cashAccountId","cash_account_id");var cashAccount=access.cashAccount(account);
        Long scopedContract=optionalId(m,"contractId");if(scopedContract!=null){var c=access.contract(scopedContract);require(cashAccount.get("property_id")==null||Objects.equals(cashAccount.get("property_id"),c.get("property_id")),"Cash account and contract properties differ");}
        Long paymentDocument=optionalId(m,"paymentDocumentId");
        if(paymentDocument!=null){var d=access.document(paymentDocument);require("ISSUED".equals(d.get("status"))&&Objects.equals(scopedContract,d.get("contract_id")),"Payment document and receipt contract differ");}
        BigDecimal value=cashAmount(m,"amount");String code=required(m,"code","transactionCode");
        db.jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"cash:"+code);
        var prior=db.rows("select * from cash_transactions where transaction_code=?",code);
        if(!prior.isEmpty()){
            var old=prior.getFirst();require(money(old,"amount").compareTo(value)==0&&com.hostel.service.Database.id(old,"cash_account_id")==account&&"IN".equals(old.get("direction"))&&Objects.equals(paymentDocument,old.get("payment_document_id")),"Idempotency key has different cash data or invoice");
            require(text(m,"CASH","method").equals(old.get("method")),"Receipt method differs from the original request");
            if(value(m,"occurredAt","occurred_at")!=null)require(time(value(m,"occurredAt","occurred_at")).toInstant().equals(time(old.get("occurred_at")).toInstant()),"Receipt event time differs from the original request");
            if(value(m,"payerId")!=null)require(Objects.equals(optionalId(m,"payerId"),old.get("payer_or_payee_id")),"Receipt payer differs from the original request");
            if(value(m,"externalReference")!=null)require(Objects.equals(text(m,null,"externalReference"),old.get("external_reference")),"Bank reference differs from the original receipt");
            if(scopedContract!=null&&"CONFIRMED".equals(old.get("status")))require(db.jdbc.queryForObject("select count(*) from fund_entries e join fund_accounts f on f.id=e.to_fund_id where e.entry_type='TRANSFER' and f.contract_id=? and f.fund_type=? and e.entry_code=?",Long.class,scopedContract,text(m,"ADVANCE","fundType"),code+"-CLASSIFY")==1,"Receipt was classified differently");
            if(bool(m,false,"confirmed"))require("CONFIRMED".equals(old.get("status")),"Existing receipt must be verified through its confirmation operation");
            return old;
        }
        require(!time(com.hostel.model49.Input49.value(m,"occurredAt","occurred_at")).isAfter(now()),"Actual receipt cannot occur in the future");
        long tx=db.insert("cash_transactions","cash_account_id,payer_or_payee_id,transaction_code,direction,amount,method,occurred_at,status,external_reference,counterparty_note,recorded_by,payment_document_id",
            account,optionalId(m,"payerId"),code,"IN",value,text(m,"CASH","method"),time(com.hostel.model49.Input49.value(m,"occurredAt","occurred_at")),"PENDING",text(m,null,"externalReference"),text(m,"","note"),access.user(),paymentDocument);
        if(bool(m,false,"confirmed"))confirm(tx);
        Long contract=optionalId(m,"contractId");
        if(contract!=null&&bool(m,false,"confirmed")){
            long unapplied=com.hostel.service.Database.id(db.one("select id from fund_accounts where unapplied_receipt_id=?",tx),"id"),target=ensureFund(contract,text(m,"ADVANCE","fundType"));
            entry(Map.of("code",code+"-CLASSIFY","entryType","TRANSFER","fromFundId",unapplied,"toFundId",target,"amount",value,"reason","Classify confirmed receipt"));
        }
        return db.one("select * from cash_transactions where id=?",tx);
    }
    public Map<String,Object> confirm(long id){
        access.manager();var tx=db.one("select * from cash_transactions where id=? for update",id);var account=access.cashAccount(com.hostel.service.Database.id(tx,"cash_account_id"));
        if(access.role().equals("STAFF")&&com.hostel.service.Database.id(tx,"recorded_by")!=access.user())access.property(com.hostel.service.Database.id(account,"property_id"));
        require("IN".equals(tx.get("direction")),"Use the authorized refund operation for outgoing cash");
        if("CONFIRMED".equals(tx.get("status")))return tx;require("PENDING".equals(tx.get("status")),"Only pending receipts may be confirmed");
        db.jdbc.update("update cash_transactions set status='CONFIRMED',confirmed_by=?,confirmed_at=clock_timestamp() where id=?",access.user(),id);
        long fund=db.insert("fund_accounts","fund_code,fund_type,unapplied_receipt_id,beneficiary_tenant_id","UNAPPLIED-"+id,"UNAPPLIED",id,tx.get("payer_or_payee_id"));
        insertEntry("RECEIVE-"+id,"RECEIVE",id,null,fund,null,null,null,money(tx,"amount"),"Actual confirmed receipt");
        events.audit("CONFIRM_RECEIPT","cash_transactions",id,tx,db.one("select * from cash_transactions where id=?",id),"Verified actual receipt");return db.one("select * from cash_transactions where id=?",id);
    }
    public Map<String,Object> entry(Map<String,Object> m){
        access.manager();String code=required(m,"code"),kind=required(m,"entryType","entry_type");BigDecimal a=cashAmount(m,"amount");
        require(Set.of("PAYMENT","TRANSFER","CREDIT_TO_FUND","ADJUSTMENT_RELEASE","REVERSAL").contains(kind),"Use dedicated cash/reconciliation operations");
        Long from=optionalId(m,"fromFundId"),to=optionalId(m,"toFundId"),debit=optionalId(m,"debitItemId"),credit=optionalId(m,"creditItemId"),original=optionalId(m,"originalEntryId");
        Set<Long> funds=new TreeSet<>();if(from!=null)funds.add(from);if(to!=null)funds.add(to);
        if("TRANSFER".equals(kind)&&from!=null&&to!=null){var source=access.fund(from);var target=access.fund(to);if(source.get("beneficiary_tenant_id")!=null&&target.get("beneficiary_tenant_id")!=null)require(Objects.equals(source.get("beneficiary_tenant_id"),target.get("beneficiary_tenant_id")),"Transferring between different people requires a separately recorded agreement");}
        if(original!=null){var old=db.one("select * from fund_entries where id=?",original);if(old.get("from_fund_id")!=null)funds.add(com.hostel.service.Database.id(old,"from_fund_id"));if(old.get("to_fund_id")!=null)funds.add(com.hostel.service.Database.id(old,"to_fund_id"));}
        Set<Long> contracts=new TreeSet<>();String action=kind.equals("PAYMENT")?"BILLING":kind.equals("TRANSFER")?"TRANSFER":"ADJUST";
        for(long f:funds){var row=access.fund(f);if(row.get("contract_id")!=null){long c=com.hostel.service.Database.id(row,"contract_id");contracts.add(c);var co=access.contract(c);String grant=kind.equals("TRANSFER")&&from!=null&&"UNAPPLIED".equals(access.fund(from).get("fund_type"))?"RECEIVE":action;if(kind.equals("PAYMENT")&&"DEPOSIT".equals(row.get("fund_type")))grant="DEPOSIT_OFFSET";access.action(com.hostel.service.Database.id(co,"room_id"),grant,a);}else if(row.get("reservation_id")!=null){var r=db.one("select room_id from reservations where id=?",row.get("reservation_id"));access.action(com.hostel.service.Database.id(r,"room_id"),kind.equals("TRANSFER")&&from!=null&&"UNAPPLIED".equals(access.fund(from).get("fund_type"))?"RECEIVE":action,a);}}
        for(Long item:Arrays.asList(debit,credit))if(item!=null){var i=db.one("select document_id from billing_items where id=?",item);var d=access.document(com.hostel.service.Database.id(i,"document_id"));if(d.get("contract_id")!=null){contracts.add(com.hostel.service.Database.id(d,"contract_id"));if(!"PAYMENT".equals(kind))access.action(com.hostel.service.Database.id(access.contract(com.hostel.service.Database.id(d,"contract_id")),"room_id"),"ADJUST",a);}else {var r=db.one("select room_id from reservations where id=?",d.get("reservation_id"));access.action(com.hostel.service.Database.id(r,"room_id"),"ADJUST",a);}}
        for(long c:contracts)db.one("select id from contracts where id=? for update",c);
        for(long f:funds)db.one("select id from fund_accounts where id=? for update",f);
        db.jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"entry:"+code);
        var previous=db.rows("select * from fund_entries where entry_code=?",code);
        if(!previous.isEmpty()){var old=previous.getFirst();require(kind.equals(old.get("entry_type"))&&a.compareTo(money(old,"amount"))==0&&Objects.equals(from,old.get("from_fund_id"))&&Objects.equals(to,old.get("to_fund_id"))&&Objects.equals(debit,old.get("debit_item_id"))&&Objects.equals(credit,old.get("credit_item_id"))&&Objects.equals(original,old.get("original_entry_id")),"Idempotency key has different allocation data");return old;}
        long id=insertEntry(code,kind,null,from,to,debit,credit,original,a,text(m,"","reason"));events.audit("POST_ENTRY","fund_entries",id,null,db.one("select * from fund_entries where id=?",id),text(m,"","reason"));return db.one("select * from fund_entries where id=?",id);
    }
    private long insertEntry(String code,String kind,Long cash,Long from,Long to,Long debit,Long credit,Long original,BigDecimal amount,String reason){return db.insert("fund_entries","entry_code,entry_type,cash_transaction_id,from_fund_id,to_fund_id,debit_item_id,credit_item_id,original_entry_id,amount,reason,posted_by",code,kind,cash,from,to,debit,credit,original,amount,reason,access.user());}
    public Map<String,Object> correctMeterLine(long source,Map<String,Object> m){
        access.manager();var old=db.one("select * from v_billing_lines where id=?",source);access.document(com.hostel.service.Database.id(old,"document_id"));
        require("ISSUED".equals(old.get("document_status"))&&"METER".equals(old.get("calculation_basis"))&&"DEBIT".equals(old.get("direction"))&&old.get("adjusts_item_id")==null,"Choose an original issued meter line");
        long contract=com.hostel.service.Database.id(old,"contract_id");db.one("select id from contracts where id=? for update",contract);String code=required(m,"code"),reason=required(m,"reason");
        BigDecimal closing=amount(m,"-1","correctedClosingIndex");require(closing.signum()>=0&&closing.stripTrailingZeros().scale()<=3,"Corrected index must be nonnegative with at most three decimal places");
        BigDecimal corrected=closing.subtract(money(old,"old_index"));require(corrected.signum()>=0,"Corrected closing index is below the original opening index");
        String basis="METER_CORRECTION; source="+source+"; opening="+money(old,"old_index").toPlainString()+"; closing="+closing.stripTrailingZeros().toPlainString();
        db.jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"meter-correction:"+code);
        var previous=db.rows("select d.id,d.status,d.reason,i.basis_note,i.subtotal from billing_documents d join v_billing_lines i on i.document_id=d.id where d.document_code=?",code);
        if(!previous.isEmpty()){var p=previous.getFirst();require(previous.size()==1&&"ISSUED".equals(p.get("status"))&&basis.equals(p.get("basis_note"))&&reason.equals(p.get("reason")),"Correction code was used for a different source, index or reason");access.action(com.hostel.service.Database.id(access.contract(contract),"room_id"),"ADJUST",money(p,"subtotal"));return document(com.hostel.service.Database.id(p,"id"));}
        require(db.rows("select id from billing_documents where document_code=?",code).isEmpty(),"Document code already exists");
        var segment=db.one("select * from v_segments where id=?",old.get("segment_id"));
        db.one("select id from meters where id=? for update",segment.get("meter_id"));
        var point=db.one("select r.* from meter_readings r where r.meter_id=? and r.observed_at=? and not exists(select 1 from meter_readings n where n.supersedes_reading_id=r.id)",segment.get("meter_id"),segment.get("service_end"));
        require(db.rows("select s.id from consumption_segments s where s.start_reading_id=? and not exists(select 1 from consumption_segments n where n.supersedes_segment_id=s.id)",point.get("id")).isEmpty(),"A later interval uses this point; reconcile the dependent intervals before correcting the closing index");
        var later=db.rows("select r.* from meter_readings r where r.meter_id=? and r.observed_at>? and not exists(select 1 from meter_readings n where n.supersedes_reading_id=r.id) order by observed_at limit 1",segment.get("meter_id"),segment.get("service_end"));
        require(later.isEmpty()||money(later.getFirst(),"reading_value").compareTo(closing)>=0,"Corrected closing index conflicts with a later measurement");
        BigDecimal already=db.jdbc.queryForObject("select coalesce(sum(case direction when 'DEBIT' then billed_quantity else -billed_quantity end),0) from v_billing_lines where adjusts_item_id=? and document_status='ISSUED' and source_key like ?",BigDecimal.class,source,"METER-CORRECTION-"+source+"-%");
        BigDecimal delta=corrected.subtract(money(old,"billed_quantity").add(already));require(delta.signum()!=0,"Corrected usage is already reflected in issued adjustments");
        BigDecimal amount=delta.abs().multiply(money(old,"unit_price_snapshot"));access.action(com.hostel.service.Database.id(access.contract(contract),"room_id"),"ADJUST",amount);
        long d=createDocument(Map.of("contractId",contract,"code",code,"documentType","ADJUSTMENT","relatedDocumentId",old.get("document_id"),"issueDate",LocalDate.now(),"reason",reason));
        var line=new HashMap<String,Object>();line.put("itemKind","UTILITY");line.put("direction",delta.signum()>0?"DEBIT":"CREDIT");line.put("adjustsItemId",source);line.put("name","Điều chỉnh sản lượng "+old.get("item_name_snapshot"));line.put("unit",old.get("unit_name_snapshot"));line.put("unitPrice",old.get("unit_price_snapshot"));line.put("quantity",delta.abs());line.put("serviceFrom",old.get("service_from"));line.put("serviceTo",old.get("service_to"));line.put("basisNote",basis);line.put("sourceKey","METER-CORRECTION-"+source+"-"+code);
        long correction=line(d,line);issue(d);if(delta.signum()<0)releaseOverpayment(correction,reason);
        db.insert("meter_readings","meter_id,observed_at,reading_value,revision_no,supersedes_reading_id,correction_item_id,recorded_by,reason",segment.get("meter_id"),segment.get("service_end"),closing,((Number)point.get("revision_no")).intValue()+1,point.get("id"),correction,access.user(),reason);
        events.audit("CORRECT_METER_CHARGE","billing_items",source,old,Map.of("correctedClosingIndex",closing,"adjustmentDocumentId",d),reason);return document(d);
    }
    public void releaseOverpayment(long creditId,String reason){
        var credit=db.one("select * from v_billing_lines where id=?",creditId);require("CREDIT".equals(credit.get("direction"))&&credit.get("adjusts_item_id")!=null,"Adjustment credit required");
        BigDecimal unused=money(credit,"subtotal").subtract(db.jdbc.queryForObject("select coalesce(sum(e.amount-coalesce((select sum(r.amount) from fund_entries r where r.entry_type='REVERSAL' and r.original_entry_id=e.id),0)),0) from fund_entries e where e.credit_item_id=?",BigDecimal.class,creditId));
        var payments=db.rows("select p.*,p.amount-coalesce((select sum(e.amount) from fund_entries e where e.original_entry_id=p.id and e.entry_type in ('REVERSAL','ADJUSTMENT_RELEASE')),0)+coalesce((select sum(r.amount) from fund_entries r join fund_entries e on e.id=r.original_entry_id where r.entry_type='REVERSAL' and e.entry_type='ADJUSTMENT_RELEASE' and e.original_entry_id=p.id),0) remaining from fund_entries p where p.entry_type='PAYMENT' and p.debit_item_id=? order by p.id desc",credit.get("adjusts_item_id"));
        for(var pay:payments){BigDecimal over=money(db.one("select * from v_item_balances where id=?",credit.get("adjusts_item_id")),"overpayment");BigDecimal part=unused.min(over).min(money(pay,"remaining"));if(part.signum()<=0)continue;
            entry(Map.of("code","RELEASE-CREDIT-"+creditId+"-PAY-"+pay.get("id"),"entryType","ADJUSTMENT_RELEASE","creditItemId",creditId,"originalEntryId",pay.get("id"),"toFundId",pay.get("from_fund_id"),"amount",part,"reason",reason));unused=unused.subtract(part);
        }
    }
    public Object reconcileFund(long id,Map<String,Object> m){
        access.admin();var f=access.fund(id);db.one("select id from fund_accounts where id=? for update",id);
        require("PENDING".equals(f.get("reconciliation_status")),"Fund already reconciled");String reason=required(m,"reason");BigDecimal balance=amount(m,"-1","verifiedBalance");require(balance.signum()>=0,"Verified balance must be nonnegative");
        BigDecimal delta=balance.subtract(money(f,"balance"));require(delta.signum()>=0,"Existing posted balance needs linked corrections before reconciliation");
        if(delta.signum()>0)insertEntry("RECONCILE-"+id,"OPENING_BALANCE",null,null,id,null,null,null,delta,reason);
        db.jdbc.update("update fund_accounts set reconciliation_status='VERIFIED' where id=?",id);events.audit("RECONCILE_FUND","fund_accounts",id,f,access.fund(id),reason);return access.fund(id);
    }
    public Object refund(long fund,Map<String,Object> m){
        access.manager();var f=access.fund(fund);BigDecimal a=cashAmount(m,"amount");
        if(f.get("contract_id")!=null){var c=access.contract(com.hostel.service.Database.id(f,"contract_id"));access.action(com.hostel.service.Database.id(c,"room_id"),"REFUND",a);db.one("select id from contracts where id=? for update",f.get("contract_id"));}
        else if(f.get("reservation_id")!=null){var r=db.one("select room_id from reservations where id=?",f.get("reservation_id"));access.action(com.hostel.service.Database.id(r,"room_id"),"REFUND",a);}else access.admin();
        db.one("select id from fund_accounts where id=? for update",fund);f=access.fund(fund);
        long account=keyId(m,"cashAccountId");var cashAccount=access.cashAccount(account);String code=required(m,"code");
        Object property=f.get("contract_id")!=null?access.contract(com.hostel.service.Database.id(f,"contract_id")).get("property_id"):f.get("reservation_id")!=null?db.one("select r.property_id from reservations s join rooms r on r.id=s.room_id where s.id=?",f.get("reservation_id")).get("property_id"):null;
        require(property==null||cashAccount.get("property_id")==null||Objects.equals(property,cashAccount.get("property_id")),"Refund account and tenancy properties differ");
        db.jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"cash:"+code);
        var old=db.rows("select * from cash_transactions where transaction_code=?",code);if(!old.isEmpty()){require(a.compareTo(money(old.getFirst(),"amount"))==0&&"OUT".equals(old.getFirst().get("direction"))&&com.hostel.service.Database.id(old.getFirst(),"cash_account_id")==account&&db.jdbc.queryForObject("select count(*) from fund_entries where cash_transaction_id=? and entry_type='REFUND' and from_fund_id=?",Long.class,old.getFirst().get("id"),fund)==1,"Refund key conflict");return old.getFirst();}
        require(a.compareTo(money(f,"available"))<=0,"Refund exceeds available money");
        require(!time(value(m,"occurredAt")).isAfter(now()),"Actual refund cannot occur in the future");
        require(bool(m,false,"paid"),"Record actual payment only after cash has left; failed refund stays pending outside the ledger");
        long tx=db.insert("cash_transactions","cash_account_id,payer_or_payee_id,transaction_code,direction,amount,method,occurred_at,status,recorded_by,confirmed_by,confirmed_at,reason",account,f.get("beneficiary_tenant_id"),code,"OUT",a,text(m,"CASH","method"),time(value(m,"occurredAt")),"CONFIRMED",access.user(),access.user(),now(),required(m,"reason"));
        insertEntry(code+"-REFUND","REFUND",tx,fund,null,null,null,null,a,required(m,"reason"));events.audit("REFUND","cash_transactions",tx,null,db.one("select * from cash_transactions where id=?",tx),required(m,"reason"));return db.one("select * from cash_transactions where id=?",tx);
    }
}
