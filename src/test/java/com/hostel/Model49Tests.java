package com.hostel;

import com.hostel.model49.*;
import com.hostel.service.Database;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.hostel.service.Database.id;

@SpringBootTest(properties={"spring.flyway.target=13","spring.datasource.url=${DB49_URL:jdbc:postgresql://127.0.0.1:55449/hostel_test49}"})
@ActiveProfiles({"test","model49"})
class Model49Tests {
    @Autowired Database db; @Autowired Leases49 leases; @Autowired Finance49 finance;
    @Autowired Access49 access; @Autowired Meters49 meters; @Autowired Api49 api;
    @Autowired Settlements49 settlements; @Autowired PlatformTransactionManager manager; @Autowired Billing49 billing;
    long actor,property,room,person,contract,deposit,advance,account;String suffix;
    static final LocalDate START=LocalDate.of(2026,1,1),END=LocalDate.of(2027,1,1);
    void as(long user,String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(Long.toString(user),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
    <T>T tx(java.util.function.Supplier<T> fn){return new TransactionTemplate(manager).execute(s->fn.get());}
    @BeforeEach void seed(){
        suffix=UUID.randomUUID().toString().substring(0,12);
        actor=db.insert("users","username,password_hash,role","test49-"+suffix,"unused","ADMIN");as(actor,"ADMIN");
        property=db.insert("properties","property_code,name,address","P-"+suffix,"Test49","Local test");
        room=db.insert("rooms","property_id,room_code,max_occupants,opened_on",property,"R-"+suffix,2,START);
        db.insert("room_operating_periods","room_id,valid_from,status,recorded_by",room,START.atStartOfDay().atOffset(ZoneOffset.ofHours(7)),"READY",actor);
        person=db.insert("tenant_profiles","full_name","Tenant "+suffix);
        account=db.insert("cash_accounts","property_id,account_code,name,account_type",property,"A-"+suffix,"Cash","CASH");
        contract=id(leases.create(Map.of("roomId",room,"tenantId",person,"code","C-"+suffix,"startDate",START,"endDate",END,"rentalPrice",3100,"deposit",1000,"signedAt","2025-12-30T10:00:00+07:00")),"id");
        // Each case controls its registered services and prices explicitly.
        db.jdbc.update("delete from contract_service_subscriptions where contract_id=?",contract);
        deposit=finance.ensureFund(contract,"DEPOSIT");advance=finance.ensureFund(contract,"ADVANCE");
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Map<String,Object> receipt(long c,int amount,String kind,String code,boolean confirmed){return finance.receive(Map.of("cashAccountId",account,"contractId",c,"amount",amount,"fundType",kind,"code",code,"confirmed",confirmed,"occurredAt","2026-01-02T10:00:00+07:00"));}
    long bill(int amount){return tx(()->{long d=finance.createDocument(Map.of("contractId",contract,"documentType","COMPENSATION","code","B-"+UUID.randomUUID(),"issueDate","2026-01-03"));finance.line(d,Map.of("name","Test charge","unitPrice",amount,"itemKind","DAMAGE"));finance.issue(d);return id(db.one("select id from billing_items where document_id=?",d),"id");});}
    Map<String,Object> payment(long source,long line,int amount,String code){return finance.entry(Map.of("code",code,"entryType","PAYMENT","fromFundId",source,"debitItemId",line,"amount",amount,"reason","Verified test allocation"));}
    void equalMoney(String value,Object actual){assertEquals(0,new BigDecimal(value).compareTo((BigDecimal)actual));}
    @Test void schemaContains49TablesAndNoImplicitDepositOrResidence(){
        assertEquals(49,db.jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE' and table_name<>'flyway_schema_history'",Integer.class));
        equalMoney("0",access.fund(deposit).get("balance"));assertEquals(0,db.jdbc.queryForObject("select count(*) from contract_residencies where contract_id=?",Integer.class,contract));
        assertEquals(0,db.jdbc.queryForObject("select count(*) from cash_transactions where cash_account_id=?",Integer.class,account));
    }
    @Test void actualReceiptOverpaymentAndAllocationAreSeparate(){
        receipt(contract,1200,"ADVANCE","RC-"+suffix,true);long line=bill(1000);payment(advance,line,1000,"PAY-"+suffix);
        equalMoney("200",access.fund(advance).get("balance"));equalMoney("0",db.one("select balance_due from v_item_balances where id=?",line).get("balance_due"));
        equalMoney("1200",db.jdbc.queryForObject("select sum(amount) from cash_transactions where cash_account_id=? and direction='IN'",BigDecimal.class,account));
    }
    @Test void pendingCashRequiresConfirmationAndRetryDoesNotDoubleReceive(){
        var r=receipt(contract,500,"ADVANCE","PENDING-"+suffix,false);assertEquals("PENDING",r.get("status"));equalMoney("0",access.fund(advance).get("balance"));
        finance.confirm(id(r,"id"));finance.confirm(id(r,"id"));
        equalMoney("500",db.one("select * from v_fund_balances where unapplied_receipt_id=?",r.get("id")).get("balance"));
        assertEquals(1,db.jdbc.queryForObject("select count(*) from fund_entries where cash_transaction_id=?",Integer.class,r.get("id")));
    }
    @Test void holdProtectsRefundAndRefundRetryIsIdempotent(){
        receipt(contract,1000,"DEPOSIT","DEP-"+suffix,true);
        long hold=db.insert("fund_holds","fund_account_id,amount,reason,review_on,created_by",deposit,200,"Unconfirmed final utility",LocalDate.now().plusDays(3),actor);
        var m=Map.<String,Object>of("cashAccountId",account,"amount",800,"code","REF-"+suffix,"paid",true,"occurredAt","2026-01-05T10:00:00+07:00","reason","Actual returned money");
        finance.refund(deposit,m);finance.refund(deposit,m);equalMoney("200",access.fund(deposit).get("balance"));equalMoney("0",access.fund(deposit).get("available"));
        assertThrows(RuntimeException.class,()->finance.refund(deposit,Map.of("cashAccountId",account,"amount",1,"code","REF2-"+suffix,"paid",true,"occurredAt","2026-01-05T10:00:00+07:00","reason","Cannot use held money")));
        assertNotNull(db.one("select * from fund_holds where id=?",hold));
    }
    @Test void issuedItemsAndLedgerCannotBeOverwritten(){
        long line=bill(100);receipt(contract,100,"ADVANCE","IMM-"+suffix,true);var e=payment(advance,line,100,"IMM-PAY-"+suffix);
        assertThrows(RuntimeException.class,()->db.jdbc.update("update billing_items set unit_price_snapshot=1 where id=?",line));
        assertThrows(RuntimeException.class,()->db.jdbc.update("delete from fund_entries where id=?",e.get("id")));
    }
    @Test void deferredConstraintRejectsUnclassifiedConfirmedCash(){
        assertThrows(RuntimeException.class,()->tx(()->db.insert("cash_transactions","cash_account_id,transaction_code,direction,amount,method,occurred_at,status,recorded_by,confirmed_by,confirmed_at",account,"BAD-"+suffix,"IN",123,"CASH",OffsetDateTime.now().minusMinutes(1),"CONFIRMED",actor,actor,OffsetDateTime.now())));
        assertTrue(db.rows("select id from cash_transactions where transaction_code=?","BAD-"+suffix).isEmpty());
    }
    @Test void pendingVerificationBlocksIssue(){
        long d=finance.createDocument(Map.of("contractId",contract,"code","VERIFY-"+suffix,"documentType","COMPENSATION"));
        long line=finance.line(d,Map.of("name","Disputed damage","unitPrice",100,"verificationStatus","PENDING"));
        assertThrows(RuntimeException.class,()->finance.issue(d));db.jdbc.update("update billing_items set verification_status='VERIFIED' where id=?",line);finance.issue(d);
    }
    @Test void capacityUsesActualResidentsWithoutAssumingSigner(){
        leases.reside(contract,Map.of("tenantId",person,"effectiveFrom",START));
        long two=db.insert("tenant_profiles","full_name","Second"),three=db.insert("tenant_profiles","full_name","Third");
        leases.reside(contract,Map.of("tenantId",two,"effectiveFrom",START));assertThrows(RuntimeException.class,()->leases.reside(contract,Map.of("tenantId",three,"effectiveFrom",START)));
    }
    @Test void renewalKeepsSignedAndBilledHistory(){
        long line=bill(3100);long term=id(db.one("select * from contract_terms where contract_id=?",contract),"id");
        leases.renew(contract,Map.of("effectiveFrom",END,"endDate","2028-01-01","rentalPrice",3500,"signedAt","2026-10-01T10:00:00+07:00","reason","Renewal agreement"));
        equalMoney("3100",db.one("select * from contract_terms where id=?",term).get("rental_price"));equalMoney("3100",db.one("select * from v_billing_lines where id=?",line).get("subtotal"));
    }
    @Test void checkoutDoesNotCreateCashRefundAndRoomNeedsPreparation(){
        receipt(contract,1000,"DEPOSIT","CHECK-"+suffix,true);leases.possess(contract,Map.of("takenAt","2026-01-01T10:00:00+07:00"));
        assertEquals("RENTED",access.room(room).get("display_status"));leases.checkout(contract,Map.of("billingEnd","2026-02-01","returnedAt","2026-02-01T10:00:00+07:00"));
        settlements.approve(contract,Map.of("reason","All final evidence reviewed"));
        @SuppressWarnings("unchecked") var checkout=(Map<String,Object>)leases.detail(contract).get("checkout");equalMoney("0",checkout.get("actual_refund_amount"));equalMoney("1000",checkout.get("held_balance"));
        assertEquals("TURNOVER",access.room(room).get("display_status"));equalMoney("1000",access.fund(deposit).get("balance"));assertEquals(0,db.jdbc.queryForObject("select count(*) from cash_transactions where cash_account_id=? and direction='OUT'",Integer.class,account));
    }
    @Test void roomAssignedStaffCannotReadOtherRoomOrRefundWithoutGrant(){
        receipt(contract,1000,"DEPOSIT","STAFF-"+suffix,true);long staff=db.insert("users","username,password_hash,role","staff-"+suffix,"unused","STAFF");
        db.insert("employee_assignments","user_id,room_id",staff,room);long other=db.insert("rooms","property_id,room_code,max_occupants,opened_on",property,"OTHER-"+suffix,2,START);
        as(staff,"STAFF");assertNotNull(access.room(room));assertThrows(RuntimeException.class,()->access.room(other));
        assertThrows(RuntimeException.class,()->finance.refund(deposit,Map.of("cashAccountId",account,"amount",100,"code","STAFF-REF-"+suffix,"paid",true,"occurredAt","2026-02-01T10:00:00+07:00","reason","Missing permission")));
    }
    @Test void concurrentPaymentsCannotSpendSameFundTwice()throws Exception{receipt(contract,1000,"ADVANCE","RACE-"+suffix,true);long line=bill(1000);race(advance,advance,line);equalMoney("200",access.fund(advance).get("available"));}
    @Test void concurrentFundsCannotOverpaySameDebt()throws Exception{receipt(contract,1000,"ADVANCE","RACE-A-"+suffix,true);receipt(contract,1000,"DEPOSIT","RACE-D-"+suffix,true);race(advance,deposit,bill(1000));}
    @Test void perPersonChargeUsesActualPersonDays(){
        long service=db.insert("service_items","service_code,name,unit_name,calculation_type","PERS-"+suffix,"People charge","Person","PER_PERSON");
        db.insert("service_price_configs","property_id,service_item_id,effective_from,unit_price",property,service,START,310);
        db.insert("contract_service_subscriptions","contract_id,service_item_id,effective_from,quantity",contract,service,START,1);
        leases.reside(contract,Map.of("tenantId",person,"effectiveFrom","2026-01-16"));
        var doc=billing.monthly(Map.of("contractId",contract,"period","2026-01","issueDate","2026-01-01"));
        equalMoney("3260",doc.get("total_amount"));
    }
    long meteredService(){long s=db.insert("service_items","service_code,name,unit_name,calculation_type","MTR-"+suffix,"Electric test","kWh","METER_BASED");db.insert("service_price_configs","property_id,service_item_id,effective_from,unit_price",property,s,START,2);return s;}
    long device(long service){return db.insert("meters","room_id,service_item_id,meter_code,installed_at,recorded_by",room,service,"M-"+suffix,Input49.time(START),actor);}
    @Test void weightedDailyAlertBlocksIssueUntilReviewed(){
        leases.possess(contract,Map.of("takenAt","2026-01-01T00:00:00+07:00","reason","Explicit physical handover for metering"));
        long service=meteredService(),meter=device(service);db.insert("alert_policies","room_id,service_item_id,effective_from,history_months,minimum_days,threshold_percentage",room,service,START,3,7,50);
        meters.usage(Map.of("meterId",meter,"contractId",contract,"startAt","2026-01-01T00:00:00+07:00","endAt","2026-02-01T00:00:00+07:00","oldIndex",0,"newIndex",31));
        meters.usage(Map.of("meterId",meter,"contractId",contract,"startAt","2026-02-01T00:00:00+07:00","endAt","2026-03-01T00:00:00+07:00","oldIndex",31,"newIndex",59));
        meters.usage(Map.of("meterId",meter,"contractId",contract,"startAt","2026-03-01T00:00:00+07:00","endAt","2026-04-01T00:00:00+07:00","oldIndex",59,"newIndex",90));
        var s=meters.usage(Map.of("meterId",meter,"contractId",contract,"startAt","2026-04-01T00:00:00+07:00","endAt","2026-04-16T00:00:00+07:00","oldIndex",90,"newIndex",150));
        var a=db.one("select * from consumption_alerts where segment_id=?",s.get("id"));assertEquals("PENDING",a.get("status"));equalMoney("1",a.get("baseline_daily_average"));
        long d=finance.createDocument(Map.of("contractId",contract,"code","ALERT-"+suffix,"documentType","FINAL_CHARGE"));billing.meterLine(d,s,billing.effectivePrice(access.contract(contract),s));
        assertThrows(RuntimeException.class,()->finance.issue(d));api.review(id(a,"id"),Map.of("status","CONFIRMED","note","Verified actual appliance use"));finance.issue(d);
        assertThrows(RuntimeException.class,()->api.review(id(a,"id"),Map.of("status","DISMISSED","note","Cannot rewrite issued evaluation")));
    }
    @Test void meterReplacementAllowsNewPhysicalCounterWithoutResettingOldCounter(){
        long meter=device(meteredService());meters.reading(Map.of("meterId",meter,"observedAt","2026-01-01T00:00:00+07:00","readingValue",100));
        assertThrows(RuntimeException.class,()->meters.reading(Map.of("meterId",meter,"observedAt","2026-01-02T00:00:00+07:00","readingValue",1)));
        @SuppressWarnings("unchecked") var result=(Map<String,Object>)meters.replace(meter,Map.of("replacedAt","2026-01-03T00:00:00+07:00","closingIndex",110,"newMeterCode","NEW-"+suffix,"openingIndex",0,"reason","Broken old meter replaced"));
        @SuppressWarnings("unchecked") var fresh=(Map<String,Object>)result.get("newMeter");assertNotEquals(meter,id(fresh,"id"));assertNotNull(db.one("select retired_at from meters where id=?",meter).get("retired_at"));
    }
    @Test void adjustmentRestoresOriginalMoneyWithoutCreatingCashOrDoubleReversal()throws Exception{
        receipt(contract,1000,"ADVANCE","ADJ-RC-"+suffix,true);long line=bill(1000);var paid=payment(advance,line,1000,"ADJ-PAY-"+suffix);
        long d=finance.createDocument(Map.of("contractId",contract,"code","ADJ-"+suffix,"documentType","ADJUSTMENT","relatedDocumentId",db.one("select document_id from billing_items where id=?",line).get("document_id"),"reason","Confirmed reduction"));
        long credit=finance.line(d,Map.of("direction","CREDIT","adjustsItemId",line,"name","Credit","unitPrice",200));finance.issue(d);
        equalMoney("-200",finance.document(d).get("total_amount"));
        @SuppressWarnings("unchecked") var adjustments=(List<Map<String,Object>>)finance.document(id(db.one("select document_id from billing_items where id=?",line),"document_id")).get("adjustments");assertEquals(1,adjustments.size());equalMoney("200",adjustments.getFirst().get("subtotal"));
        finance.entry(Map.of("code","RELEASE-"+suffix,"entryType","ADJUSTMENT_RELEASE","creditItemId",credit,"originalEntryId",paid.get("id"),"toFundId",advance,"amount",200,"reason","Return overpayment to original source"));
        equalMoney("200",access.fund(advance).get("balance"));equalMoney("800",db.one("select * from v_item_balances where id=?",line).get("paid_amount"));
        byte[] pdf=api.invoicePdf(id(db.one("select document_id from billing_items where id=?",line),"document_id")).getBody();
        try(var rendered=org.apache.pdfbox.Loader.loadPDF(pdf)){String text=new org.apache.pdfbox.text.PDFTextStripper().getText(rendered);assertTrue(text.contains("ADJ-"+suffix));assertTrue(text.contains("800"));}
        assertThrows(RuntimeException.class,()->finance.entry(Map.of("code","BAD-REV-"+suffix,"entryType","REVERSAL","originalEntryId",paid.get("id"),"amount",1000,"reason","Already released part")));
        equalMoney("1000",db.jdbc.queryForObject("select sum(amount) from cash_transactions where cash_account_id=?",BigDecimal.class,account));
    }
    @Test void finalUnmeasuredUtilityNeedsExplicitHoldAndReview(){
        long service=meteredService();device(service);db.insert("contract_service_subscriptions","contract_id,service_item_id,effective_from",contract,service,START);
        receipt(contract,1000,"DEPOSIT","FINAL-"+suffix,true);leases.possess(contract,Map.of("takenAt","2026-01-01T10:00:00+07:00"));leases.checkout(contract,Map.of("billingEnd","2026-02-01","returnedAt","2026-02-01T10:00:00+07:00"));
        assertThrows(RuntimeException.class,()->settlements.approve(contract,Map.of("reason","Not yet measured")));
        db.insert("fund_holds","fund_account_id,amount,reason,review_on,created_by",deposit,300,"Final meter verification pending",LocalDate.now().plusDays(3),actor);
        settlements.approve(contract,Map.of("reason","Provisional with explicit hold","pendingUtilityReason","Waiting for final reading"));
        equalMoney("300",access.fund(deposit).get("held_amount"));assertEquals(1,db.jdbc.queryForObject("select count(*) from billing_documents where contract_id=? and document_type='MONTHLY' and status='ISSUED'",Integer.class,contract));
    }
    @Test void adjustmentPermissionLimitUsesActualDocumentAmount(){
        long d=finance.createDocument(Map.of("contractId",contract,"documentType","COMPENSATION","code","LIMIT-"+suffix));finance.line(d,Map.of("name","Damage","unitPrice",500));
        long staff=db.insert("users","username,password_hash,role","limited-"+suffix,"unused","STAFF");long assignment=db.insert("employee_assignments","user_id,room_id",staff,room);
        db.insert("staff_permission_grants","assignment_id,action_code,amount_limit,granted_by",assignment,"ADJUST",100,actor);as(staff,"STAFF");
        assertThrows(RuntimeException.class,()->finance.issue(d));assertEquals("DRAFT",finance.document(d).get("status"));
    }
    @Test void reservationRefundAlsoRequiresRefundPermission(){
        long reservation=db.insert("reservations","room_id,tenant_id,reservation_code,start_date,end_date,expires_at,status",room,person,"RES-"+suffix,END,END.plusDays(3),Input49.now().plusDays(1),"HELD");
        long fund=db.insert("fund_accounts","fund_code,fund_type,reservation_id,beneficiary_tenant_id","RES-F-"+suffix,"RESERVATION",reservation,person);
        var received=finance.receive(Map.of("cashAccountId",account,"code","RES-RC-"+suffix,"amount",100,"occurredAt","2026-01-01T10:00:00+07:00","confirmed",true));long unallocated=id(db.one("select id from fund_accounts where unapplied_receipt_id=?",received.get("id")),"id");
        finance.entry(Map.of("code","RES-CLASS-"+suffix,"entryType","TRANSFER","fromFundId",unallocated,"toFundId",fund,"amount",100));
        long staff=db.insert("users","username,password_hash,role","res-staff-"+suffix,"unused","STAFF");db.insert("employee_assignments","user_id,room_id",staff,room);as(staff,"STAFF");
        assertThrows(RuntimeException.class,()->finance.refund(fund,Map.of("cashAccountId",account,"code","RES-REF-"+suffix,"amount",100,"paid",true,"occurredAt","2026-01-02T10:00:00+07:00","reason","No refund delegation")));
    }
    void race(long a,long b,long line)throws Exception{
        try(var pool=Executors.newFixedThreadPool(2)){var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var results=new ArrayList<Future<Boolean>>();
            for(long f:List.of(a,b))results.add(pool.submit(()->{as(actor,"ADMIN");ready.countDown();go.await();try{payment(f,line,800,"RACE-P-"+UUID.randomUUID());return true;}catch(RuntimeException e){return false;}finally{SecurityContextHolder.clearContext();}}));
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();int successes=0;for(var r:results)if(r.get(15,TimeUnit.SECONDS))successes++;assertEquals(1,successes);
        }equalMoney("800",db.one("select * from v_item_balances where id=?",line).get("paid_amount"));
    }
}
