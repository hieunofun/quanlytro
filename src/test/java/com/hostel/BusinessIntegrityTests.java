package com.hostel;

import com.hostel.service.Database;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** HTTP + real PostgreSQL scenarios: money conservation, source history and permissions. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.flyway.target=13","spring.datasource.url=${DB49_URL:jdbc:postgresql://127.0.0.1:55449/hostel_test49}","app.upload-directory=.runtime/implementation-49/http-uploads"})
@ActiveProfiles({"test","model49"})
class BusinessIntegrityTests {
    @Autowired Database db;
    @Autowired PasswordEncoder passwords;
    @Value("${local.server.port}") int port;
    final ObjectMapper json=new ObjectMapper();
    final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    String suffix,token;long actor,property,room,tenant,contract,account,advance,deposit;
    static final String START="2026-01-01T00:00:00+07:00";

    @BeforeEach void fixture()throws Exception{
        suffix=UUID.randomUUID().toString().substring(0,12);
        actor=db.insert("users","username,password_hash,role","business-"+suffix,passwords.encode("BusinessTest#2026"),"ADMIN");
        token=login("business-"+suffix);
        property=id(post("/properties",Map.of("code","B-"+suffix,"name","Business test","address","Fixture")));
        room=id(post("/rooms",Map.of("propertyId",property,"code","101","maxOccupants",4,"price",0,"openedOn","2026-01-01")));
        tenant=id(post("/tenants",Map.of("fullName","Tenant "+suffix,"identityNumber","ID-"+suffix)));
        contract=id(post("/contracts",Map.of("code","C-"+suffix,"roomId",room,"tenantId",tenant,"startDate","2026-01-01","endDate","2027-01-01","rentalPrice",0,"deposit",2000000,"signedAt","2025-12-30T10:00:00+07:00")));
        db.jdbc.update("delete from contract_service_subscriptions where contract_id=?",contract);
        account=((Number)db.one("select id from cash_accounts where property_id=?",property).get("id")).longValue();
        advance=fund("ADVANCE");deposit=fund("DEPOSIT");
    }
    long id(Map<String,Object> m){return ((Number)m.get("id")).longValue();}
    long fund(String kind){return ((Number)db.one("select id from fund_accounts where contract_id=? and fund_type=?",contract,kind).get("id")).longValue();}
    String login(String username)throws Exception{return ok(send("/auth/login","POST",Map.of("username",username,"password","BusinessTest#2026"),null)).get("accessToken").toString();}
    HttpResponse<String> send(String path,String method,Object body,String auth)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api"+path)).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json");
        if(auth!=null)b.header("Authorization","Bearer "+auth);
        return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    @SuppressWarnings("unchecked") Map<String,Object> ok(HttpResponse<String> r){assertEquals(200,r.statusCode(),r.body());return json.readValue(r.body(),Map.class);}
    Map<String,Object> post(String path,Object body)throws Exception{return ok(send(path,"POST",body,token));}
    Map<String,Object> get(String path)throws Exception{return ok(send(path,"GET",null,token));}
    void money(String expected,Object actual){assertEquals(0,new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())),"Actual amount: "+actual);}
    Map<String,Object> payBody(String code,int amount){return Map.of("code",code,"amount",amount,"cashAccountId",account,"occurredAt","2026-02-02T10:00:00+07:00","method","CASH");}
    long bill(int amount)throws Exception{
        long d=id(post("/billing-documents",Map.of("contractId",contract,"code","DOC-"+UUID.randomUUID(),"documentType","COMPENSATION","items",List.of(Map.of("name","Confirmed damage","itemKind","DAMAGE","unitPrice",amount)))));
        post("/billing-documents/"+d+"/issue",Map.of());return d;
    }
    void possess()throws Exception{post("/contracts/"+contract+"/possessions",Map.of("takenAt",START,"reason","Owner approved handover with outstanding deposit"));}
    void receive(int amount,String kind,String code)throws Exception{post("/cash-transactions",Map.of("cashAccountId",account,"contractId",contract,"amount",amount,"fundType",kind,"code",code,"confirmed",true,"occurredAt","2026-01-01T10:00:00+07:00"));}
    long device()throws Exception{
        long service=db.insert("service_items","service_code,name,unit_name,calculation_type","ELEC-B-"+suffix,"Electricity","kWh","METER_BASED");
        db.insert("service_price_configs","property_id,service_item_id,effective_from,unit_price",property,service,LocalDate.of(2026,1,1),3500);
        return id(post("/catalog/meters",Map.of("roomId",room,"serviceItemId",service,"meterCode","M-"+suffix,"installedAt",START)));
    }
    long meteredBill()throws Exception{
        possess();long meter=device();
        var segment=post("/meter-readings",Map.of("meterId",meter,"contractId",contract,"startAt",START,"endAt","2026-02-01T00:00:00+07:00","oldIndex",0,"newIndex",50));
        long d=id(post("/billing-documents",Map.of("contractId",contract,"code","METER-DOC-"+suffix,"documentType","FINAL_CHARGE","items",List.of(Map.of("name","Electricity","itemKind","UTILITY","unit","kWh","unitPrice",3500,"calculationBasis","METER","segmentId",segment.get("id"),"serviceFrom","2026-01-01","serviceTo","2026-02-01")))));
        post("/billing-documents/"+d+"/issue",Map.of());return d;
    }
    @SuppressWarnings("unchecked") long firstLine(long doc)throws Exception{return id(((List<Map<String,Object>>)get("/billing-documents/"+doc).get("items")).getFirst());}
    String staff(boolean assigned)throws Exception{
        String name="staff-b-"+suffix;
        long user=db.insert("users","username,password_hash,role",name,passwords.encode("BusinessTest#2026"),"STAFF");
        if(assigned)db.insert("employee_assignments","user_id,room_id",user,room);
        return login(name);
    }

    @Test void overpaymentRetryConservesActualCashAndKeepsAdvance()throws Exception{
        long d=bill(3620000);var body=payBody("PAY-"+suffix,4000000);
        post("/invoices/"+d+"/payments",body);post("/invoices/"+d+"/payments",body);
        money("0",get("/billing-documents/"+d).get("balance_due"));money("380000",get("/fund-accounts/"+advance).get("balance"));
        money("4000000",db.jdbc.queryForObject("select sum(amount) from cash_transactions where cash_account_id=?",BigDecimal.class,account));
        assertEquals(d,((Number)db.one("select payment_document_id from cash_transactions where transaction_code=?","PAY-"+suffix).get("payment_document_id")).longValue());
    }
    @Test void paymentKeyCannotBeReusedForAnotherInvoiceOrChangedAmount()throws Exception{
        long a=bill(1000),b=bill(1000);var body=payBody("SAME-"+suffix,1000);post("/invoices/"+a+"/payments",body);
        assertEquals(400,send("/invoices/"+b+"/payments","POST",body,token).statusCode());
        assertEquals(400,send("/invoices/"+a+"/payments","POST",payBody("SAME-"+suffix,1200),token).statusCode());
        money("1000",get("/billing-documents/"+b).get("balance_due"));
    }
    @Test void paymentRetryWithoutExplicitTimestampDoesNotCreateSecondReceipt()throws Exception{
        long d=bill(1000);var body=Map.of("code","NO-TIME-"+suffix,"amount",1000,"cashAccountId",account);
        post("/invoices/"+d+"/payments",body);post("/invoices/"+d+"/payments",body);
        assertEquals(1,db.jdbc.queryForObject("select count(*) from cash_transactions where cash_account_id=?",Integer.class,account));
    }
    @Test void bankReferenceCannotBeCountedTwiceWithDifferentRequestKeys()throws Exception{
        long d=bill(1000);var body=new HashMap<String,Object>(payBody("BANK-A-"+suffix,600));body.put("method","BANK_TRANSFER_MANUAL");body.put("externalReference","BANK-"+suffix);
        post("/invoices/"+d+"/payments",body);body.put("code","BANK-B-"+suffix);
        assertEquals(409,send("/invoices/"+d+"/payments","POST",body,token).statusCode());
        money("400",get("/billing-documents/"+d).get("balance_due"));money("600",db.jdbc.queryForObject("select sum(amount) from cash_transactions where cash_account_id=?",BigDecimal.class,account));
    }
    @Test void cashWithExtraDecimalsOrFutureDateIsRejectedBeforePosting()throws Exception{
        long d=bill(1000);var body=new HashMap<String,Object>(payBody("PRECISION-"+suffix,1));body.put("amount",new BigDecimal("1.001"));
        assertEquals(400,send("/invoices/"+d+"/payments","POST",body,token).statusCode());
        body.put("amount",100);body.put("occurredAt",OffsetDateTime.now().plusDays(1).toString());assertEquals(400,send("/invoices/"+d+"/payments","POST",body,token).statusCode());
        assertEquals(0,db.jdbc.queryForObject("select count(*) from cash_transactions where cash_account_id=?",Integer.class,account));
    }
    @Test void paidMeterCorrectionUsesOriginalRateAndRestoresFundsWithoutRefund()throws Exception{
        long d=meteredBill(),line=firstLine(d);post("/invoices/"+d+"/payments",payBody("METER-PAY-"+suffix,175000));
        db.insert("service_price_configs","property_id,service_item_id,effective_from,unit_price",property,db.one("select service_item_id from meters where room_id=?",room).get("service_item_id"),LocalDate.of(2026,2,2),9900);
        var body=Map.of("code","CORR-"+suffix,"correctedClosingIndex",40,"reason","Verified original meter photo");
        var correction=post("/billing-items/"+line+"/meter-corrections",body);
        assertEquals(id(correction),id(post("/billing-items/"+line+"/meter-corrections",body)));
        money("-35000",correction.get("total_amount"));var original=get("/billing-documents/"+d);
        money("140000",original.get("total_amount"));money("140000",original.get("paid_amount"));money("0",original.get("balance_due"));
        money("35000",get("/fund-accounts/"+advance).get("balance"));
        money("50",db.one("select new_index from v_billing_lines where id=?",line).get("new_index"));
        money("40",db.one("select r.reading_value from meter_readings r join meters m on m.id=r.meter_id where m.room_id=? and r.observed_at='2026-02-01T00:00:00+07:00' and not exists(select 1 from meter_readings n where n.supersedes_reading_id=r.id)",room).get("reading_value"));
        assertEquals(0,db.jdbc.queryForObject("select count(*) from cash_transactions where cash_account_id=? and direction='OUT'",Integer.class,account));
        assertEquals(400,send("/billing-items/"+line+"/meter-corrections","POST",Map.of("code","CORR-"+suffix,"correctedClosingIndex",45,"reason","Verified original meter photo"),token).statusCode());
    }
    @Test void correctionAfterEarlierCorrectionPostsOnlyTheRemainingDelta()throws Exception{
        long d=meteredBill(),line=firstLine(d);
        post("/billing-items/"+line+"/meter-corrections",Map.of("code","C1-"+suffix,"correctedClosingIndex",40,"reason","First verified correction"));
        var next=post("/billing-items/"+line+"/meter-corrections",Map.of("code","C2-"+suffix,"correctedClosingIndex",45,"reason","Final verified correction"));
        money("17500",next.get("total_amount"));money("157500",get("/billing-documents/"+d).get("total_amount"));
    }
    @Test void meterCorrectionRequiresAdjustmentPermissionAndRoomScope()throws Exception{
        long line=firstLine(meteredBill());var body=Map.of("code","DENIED-"+suffix,"correctedClosingIndex",40,"reason","Must be authorized");
        assertEquals(403,send("/billing-items/"+line+"/meter-corrections","POST",body,staff(true)).statusCode());
        String outside="outside-"+suffix;db.insert("users","username,password_hash,role",outside,passwords.encode("BusinessTest#2026"),"STAFF");
        assertEquals(404,send("/billing-items/"+line+"/meter-corrections","POST",body,login(outside)).statusCode());
    }
    @Test void monthlyCreationIsIdempotentAndCalendarClampsShortMonths()throws Exception{
        ok(send("/properties/"+property+"/billing-policy","PUT",Map.of("invoiceIssueDay",31,"meterCutoffDay",31),token));
        // A new signed version is the supported way to change a payment day.
        post("/contracts/"+contract+"/renew",Map.of("effectiveFrom","2026-02-01","endDate","2027-01-01","signedAt","2026-01-20T10:00:00+07:00","reason","February terms"));
        var calendar=get("/contracts/"+contract+"/billing-calendar?period=2026-02");assertEquals("2026-02-28",calendar.get("issueDate"));
        var body=Map.of("contractId",contract,"period","2026-02");var first=post("/invoices",body);var second=post("/invoices",body);
        assertEquals(id(first),id(second));assertEquals(calendar.get("issueDate"),first.get("issue_date"));assertEquals(calendar.get("dueDate"),first.get("due_date"));
        assertEquals(400,send("/invoices","POST",Map.of("contractId",contract,"period","2028-01"),token).statusCode());
    }
    @Test void cutoffIncludesLastDayMeasurementsWithoutUsingLaterReadings()throws Exception{
        possess();long meter=device();Object service=db.one("select service_item_id from meters where id=?",meter).get("service_item_id");
        db.insert("contract_service_subscriptions","contract_id,service_item_id,effective_from",contract,service,LocalDate.of(2026,1,1));
        db.jdbc.update("update properties set meter_cutoff_day=31 where id=?",property);
        post("/meter-readings",Map.of("meterId",meter,"contractId",contract,"startAt",START,"endAt","2026-01-31T18:00:00+07:00","oldIndex",0,"newIndex",10));
        post("/meter-readings",Map.of("meterId",meter,"contractId",contract,"startAt","2026-01-31T18:00:00+07:00","endAt","2026-02-02T18:00:00+07:00","oldIndex",10,"newIndex",20));
        var invoice=post("/invoices",Map.of("contractId",contract,"period","2026-02","issueDate","2026-02-01"));money("35000",invoice.get("total_amount"));
        assertEquals("2026-01-31",get("/contracts/"+contract+"/billing-calendar?period=2026-02").get("cutoffDate"));
    }
    @Test void backdatedReadingCannotExceedAnExistingLaterReadingEvenViaSql()throws Exception{
        long meter=device();post("/meter-points",Map.of("meterId",meter,"observedAt","2026-01-10T00:00:00+07:00","readingValue",100));
        assertEquals(400,send("/meter-points","POST",Map.of("meterId",meter,"observedAt","2026-01-05T00:00:00+07:00","readingValue",150),token).statusCode());
        assertThrows(RuntimeException.class,()->db.insert("meter_readings","meter_id,observed_at,reading_value,recorded_by",meter,OffsetDateTime.parse("2026-01-05T00:00:00+07:00"),150,actor));
    }
    @Test void measurementCannotPrecedeInstallationOrOccurInFuture()throws Exception{
        long meter=device();
        for(String at:List.of("2025-12-31T23:00:00+07:00",OffsetDateTime.now().plusDays(1).toString()))
            assertEquals(400,send("/meter-points","POST",Map.of("meterId",meter,"observedAt",at,"readingValue",100),token).statusCode());
    }
    @Test void assignedUsagePreventsCheckoutThatWouldRewriteItsResponsibility()throws Exception{
        meteredBill();assertEquals(409,send("/contracts/"+contract+"/checkout","POST",Map.of("billingEnd","2026-01-16","returnedAt","2026-01-16T10:00:00+07:00"),token).statusCode());
        assertEquals("ACTIVE",get("/contracts/"+contract).get("status"));assertEquals(0,db.jdbc.queryForObject("select count(*) from checkout_records where contract_id=?",Integer.class,contract));
    }
    @Test void draftOrNeverHandedOverTenancyCannotBecomePhysicalCheckout()throws Exception{
        assertEquals(400,send("/contracts/"+contract+"/checkout","POST",Map.of("billingEnd","2026-02-01","returnedAt","2026-02-01T00:00:00+07:00"),token).statusCode());
    }
    @Test void intakeCannotBypassExistingTenantScopeWithAnExtraPayload()throws Exception{
        String staffToken=staff(true);long foreign=db.insert("tenant_profiles","full_name,identity_number","Private profile","PRIVATE-"+suffix);
        var body=new HashMap<String,Object>(Map.of("code","BYPASS-"+suffix,"roomId",room,"tenantId",foreign,"tenant",Map.of(),"startDate","2027-01-01","endDate","2028-01-01","rentalPrice",1000));
        assertEquals(400,send("/contracts","POST",body,staffToken).statusCode());
        body.remove("tenantId");body.put("tenant",Map.of("fullName","New tenant","identityNumber","PRIVATE-"+suffix));
        var duplicate=send("/contracts","POST",body,staffToken);assertEquals(400,duplicate.statusCode());assertFalse(duplicate.body().contains("Private profile"));
        body.put("tenant",Map.of("fullName","New verified tenant","identityNumber","NEW-"+suffix));ok(send("/contracts","POST",body,staffToken));
    }
    @Test void damageExceedingDepositRemainsDebtAndApprovalDoesNotInventRefund()throws Exception{
        receive(1500000,"DEPOSIT","DEP-"+suffix);possess();bill(2000000);
        post("/contracts/"+contract+"/checkout",Map.of("billingEnd","2026-02-01","returnedAt","2026-02-01T00:00:00+07:00"));
        post("/contracts/"+contract+"/checkout/approve",Map.of("reason","Confirmed damage from handover","useDeposit",true));
        var checkout=get("/contracts/"+contract+"/checkout");money("500000",checkout.get("debt_remaining"));money("0",checkout.get("actual_refund_amount"));
        var statement=get("/contracts/"+contract+"/settlement");assertFalse(((List<?>)statement.get("charges")).isEmpty());assertTrue(((List<?>)statement.get("refunds")).isEmpty());
    }
    @Test void failedRefundDoesNotChangeBalanceAndHoldProtectsRemainingMoney()throws Exception{
        receive(1000,"DEPOSIT","HOLD-"+suffix);
        var h=post("/fund-accounts/"+deposit+"/holds",Map.of("amount",200,"reason","Final water reconciliation","reviewOn",LocalDate.now().plusDays(3).toString()));
        var body=new HashMap<String,Object>(Map.of("cashAccountId",account,"amount",800,"code","REF-"+suffix,"paid",false,"occurredAt","2026-02-01T10:00:00+07:00","reason","Actual refund"));
        assertEquals(400,send("/fund-accounts/"+deposit+"/refund","POST",body,token).statusCode());money("1000",get("/fund-accounts/"+deposit).get("balance"));
        body.put("paid",true);post("/fund-accounts/"+deposit+"/refund",body);post("/fund-accounts/"+deposit+"/refund",body);
        money("200",get("/fund-accounts/"+deposit).get("balance"));money("0",get("/fund-accounts/"+deposit).get("available"));
        post("/fund-holds/"+id(h)+"/release",Map.of("reason","Final water reconciled"));money("200",get("/fund-accounts/"+deposit).get("available"));
    }
    @Test void concurrentMonthlyRequestsReturnOneInvoice()throws Exception{
        var body=Map.of("contractId",contract,"period","2026-01");
        try(var pool=Executors.newFixedThreadPool(2)){
            var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var futures=new ArrayList<Future<Map<String,Object>>>();
            for(int i=0;i<2;i++)futures.add(pool.submit(()->{ready.countDown();assertTrue(go.await(10,TimeUnit.SECONDS));return post("/invoices",body);}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();assertEquals(id(futures.get(0).get(30,TimeUnit.SECONDS)),id(futures.get(1).get(30,TimeUnit.SECONDS)));
        }
        assertEquals(1,db.jdbc.queryForObject("select count(*) from billing_documents where contract_id=? and document_type='MONTHLY'",Integer.class,contract));
    }
    @Test void concurrentlyIssuingOneDraftIsIdempotent()throws Exception{
        long d=id(post("/billing-documents",Map.of("contractId",contract,"code","ISSUE-"+suffix,"documentType","COMPENSATION","items",List.of(Map.of("name","Damage","unitPrice",1000)))));
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->post("/billing-documents/"+d+"/issue",Map.of()));var b=pool.submit(()->post("/billing-documents/"+d+"/issue",Map.of()));
            assertEquals(id(a.get(30,TimeUnit.SECONDS)),id(b.get(30,TimeUnit.SECONDS)));
        }
        assertEquals(1,db.jdbc.queryForObject("select count(*) from audit_events where action='ISSUE' and resource_id=?",Integer.class,Long.toString(d)));
    }
    @Test void concurrentRefundsCannotSpendTheSameDepositTwice()throws Exception{
        receive(1000,"DEPOSIT","RACE-DEP-"+suffix);
        try(var pool=Executors.newFixedThreadPool(2)){
            var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var futures=new ArrayList<Future<Integer>>();
            for(int i=0;i<2;i++){final int n=i;futures.add(pool.submit(()->{
                ready.countDown();assertTrue(go.await(10,TimeUnit.SECONDS));
                return send("/fund-accounts/"+deposit+"/refund","POST",Map.of("cashAccountId",account,"amount",800,"code","RACE-REF-"+suffix+"-"+n,"paid",true,"occurredAt","2026-02-01T10:00:00+07:00","reason","Verified outgoing refund"),token).statusCode();
            }));}
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();var statuses=new ArrayList<Integer>();for(var f:futures)statuses.add(f.get(30,TimeUnit.SECONDS));Collections.sort(statuses);assertEquals(List.of(200,400),statuses);
        }
        money("200",get("/fund-accounts/"+deposit).get("balance"));money("800",db.jdbc.queryForObject("select sum(amount) from cash_transactions where cash_account_id=? and direction='OUT'",BigDecimal.class,account));
    }
    @Test void correctionRestoresEachOriginalSourceRatherThanMintingAdvance()throws Exception{
        long d=meteredBill(),line=firstLine(d);receive(100000,"DEPOSIT","MIX-DEP-"+suffix);receive(75000,"ADVANCE","MIX-ADV-"+suffix);
        post("/fund-entries",Map.of("code","MIX-P1-"+suffix,"entryType","PAYMENT","fromFundId",deposit,"debitItemId",line,"amount",100000));
        post("/fund-entries",Map.of("code","MIX-P2-"+suffix,"entryType","PAYMENT","fromFundId",advance,"debitItemId",line,"amount",75000));
        post("/billing-items/"+line+"/meter-corrections",Map.of("code","MIX-C-"+suffix,"correctedClosingIndex",20,"reason","Verified usage correction"));
        money("75000",get("/fund-accounts/"+advance).get("balance"));money("30000",get("/fund-accounts/"+deposit).get("balance"));
        money("70000",get("/billing-documents/"+d).get("paid_amount"));
        assertEquals(0,db.jdbc.queryForObject("select count(*) from cash_transactions where cash_account_id=? and direction='OUT'",Integer.class,account));
    }
    @Test void crossTenancyDocumentAndHoldReferencesAreRejectedByDatabase()throws Exception{
        long foreignTenant=db.insert("tenant_profiles","full_name","Other tenant");
        long other=id(post("/contracts",Map.of("code","OTHER-"+suffix,"roomId",room,"tenantId",foreignTenant,"startDate","2027-01-01","endDate","2028-01-01","rentalPrice",0,"deposit",0)));
        long foreignDoc=id(post("/billing-documents",Map.of("contractId",other,"code","OTHER-D-"+suffix,"documentType","COMPENSATION","items",List.of(Map.of("name","Other charge","unitPrice",100)))));
        long foreignLine=firstLine(foreignDoc);receive(1000,"DEPOSIT","SCOPED-"+suffix);
        assertEquals(400,send("/billing-documents","POST",Map.of("contractId",contract,"code","BAD-LINK-"+suffix,"documentType","ADJUSTMENT","relatedDocumentId",foreignDoc),token).statusCode());
        assertThrows(RuntimeException.class,()->db.insert("billing_documents","contract_id,document_code,document_type,related_document_id,issue_date,due_date",contract,"BAD-SQL-"+suffix,"ADJUSTMENT",foreignDoc,LocalDate.now(),LocalDate.now()));
        assertEquals(409,send("/fund-accounts/"+deposit+"/holds","POST",Map.of("billingItemId",foreignLine,"amount",100,"reason","Invalid foreign evidence","reviewOn",LocalDate.now().plusDays(3).toString()),token).statusCode());
        money("1000",get("/fund-accounts/"+deposit).get("available"));
    }
    @Test void oneMeterSegmentCannotBeChargedOnTwoOriginalDocuments()throws Exception{
        long original=meteredBill();Object segment=db.one("select segment_id from billing_items where document_id=?",original).get("segment_id");
        var duplicate=Map.of("contractId",contract,"code","DUP-SOURCE-"+suffix,"documentType","FINAL_CHARGE","items",List.of(Map.of("name","Duplicate electricity","itemKind","UTILITY","unit","kWh","unitPrice",3500,"calculationBasis","METER","segmentId",segment,"serviceFrom","2026-01-01","serviceTo","2026-02-01")));
        assertEquals(409,send("/billing-documents","POST",duplicate,token).statusCode());
        assertEquals(1,db.jdbc.queryForObject("select count(*) from billing_items where segment_id=? and adjusts_item_id is null",Integer.class,segment));
        assertEquals(0,db.jdbc.queryForObject("select count(*) from billing_documents where document_code=?",Integer.class,"DUP-SOURCE-"+suffix));
    }
    @Test void receiptRetryCannotChangeMethodTimestampOrBankReference()throws Exception{
        long d=bill(1000);var original=new HashMap<String,Object>(payBody("RETRY-"+suffix,1000));original.put("externalReference","REF-A-"+suffix);post("/invoices/"+d+"/payments",original);
        for(var change:List.of(Map.entry("method","OTHER"),Map.entry("occurredAt","2026-02-03T10:00:00+07:00"),Map.entry("externalReference","REF-B-"+suffix))){
            var changed=new HashMap<>(original);changed.put(change.getKey(),change.getValue());assertEquals(400,send("/invoices/"+d+"/payments","POST",changed,token).statusCode());
        }
        assertEquals(1,db.jdbc.queryForObject("select count(*) from cash_transactions where cash_account_id=?",Integer.class,account));
    }
    @Test void equivalentUtcEventTimeIsStoredAsTheSameVietnameseInstant()throws Exception{
        long meter=device();var reading=post("/meter-points",Map.of("meterId",meter,"observedAt","2025-12-31T17:00:00Z","readingValue",100));
        assertEquals(Instant.parse("2025-12-31T17:00:00Z"),OffsetDateTime.parse(reading.get("observed_at").toString()).toInstant());
    }
    @Test void nextIntervalUsesCorrectedOpeningWithoutRewritingBilledHistory()throws Exception{
        long d=meteredBill(),line=firstLine(d);post("/billing-items/"+line+"/meter-corrections",Map.of("code","NEXT-CORR-"+suffix,"correctedClosingIndex",40,"reason","Verified boundary reading"));
        Object meter=db.one("select id from meters where room_id=?",room).get("id");
        var next=post("/meter-readings",Map.of("meterId",meter,"contractId",contract,"startAt","2026-02-01T00:00:00+07:00","endAt","2026-03-01T00:00:00+07:00","oldIndex",40,"newIndex",60));
        money("20",next.get("consumption"));money("50",db.one("select new_index from v_billing_lines where id=?",line).get("new_index"));
    }
    @Test void correctingPointWithDependentIntervalIsRejectedAtomically()throws Exception{
        long d=meteredBill(),line=firstLine(d);Object meter=db.one("select id from meters where room_id=?",room).get("id");
        post("/meter-readings",Map.of("meterId",meter,"contractId",contract,"startAt","2026-02-01T00:00:00+07:00","endAt","2026-03-01T00:00:00+07:00","oldIndex",50,"newIndex",60));
        String code="DEPENDENT-"+suffix;
        assertEquals(400,send("/billing-items/"+line+"/meter-corrections","POST",Map.of("code",code,"correctedClosingIndex",40,"reason","Requires dependent-period reconciliation"),token).statusCode());
        assertEquals(0,db.jdbc.queryForObject("select count(*) from billing_documents where document_code=?",Integer.class,code));money("175000",get("/billing-documents/"+d).get("total_amount"));
    }
    @Test void staffNeedsExplicitDelegationToHandOverWithMissingActualDeposit()throws Exception{
        receive(1500000,"DEPOSIT","SHORT-DEP-"+suffix);String staffToken=staff(true);
        var handover=Map.of("takenAt",START,"reason","Owner authorized remaining 500000 later");
        assertEquals(403,send("/contracts/"+contract+"/possessions","POST",handover,staffToken).statusCode());
        long assignment=((Number)db.one("select a.id from employee_assignments a join users u on u.id=a.user_id where u.username=?","staff-b-"+suffix).get("id")).longValue();
        db.insert("staff_permission_grants","assignment_id,action_code,amount_limit,granted_by",assignment,"WAIVE_DEPOSIT",500000,actor);
        ok(send("/contracts/"+contract+"/possessions","POST",handover,staffToken));money("1500000",get("/fund-accounts/"+deposit).get("balance"));
        assertEquals(1,db.jdbc.queryForObject("select count(*) from room_possessions where contract_id=?",Integer.class,contract));
    }
    @Test void disputedDamageCannotBeIssuedOrSilentlyDeducted()throws Exception{
        receive(1000,"DEPOSIT","DISP-DEP-"+suffix);
        long doc=id(post("/billing-documents",Map.of("contractId",contract,"code","DISPUTE-"+suffix,"documentType","COMPENSATION","items",List.of(Map.of("name","Contested damage","itemKind","DAMAGE","unitPrice",200,"verificationStatus","DISPUTED","basisNote","Tenant disputes handover evidence")))));
        long line=firstLine(doc);assertEquals(409,send("/billing-documents/"+doc+"/issue","POST",Map.of(),token).statusCode());
        assertEquals(409,send("/fund-entries","POST",Map.of("code","DISP-PAY-"+suffix,"entryType","PAYMENT","fromFundId",deposit,"debitItemId",line,"amount",200),token).statusCode());
        money("1000",get("/fund-accounts/"+deposit).get("balance"));
    }
}
