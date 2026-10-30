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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.hostel.service.Database.id;

@SpringBootTest(properties={"spring.flyway.target=${OPENKOS_FLYWAY_TARGET:13}","spring.datasource.url=${OPENKOS_DB_URL:jdbc:postgresql://127.0.0.1:55449/hostel_openkos_test}"})
@ActiveProfiles({"test","model49"})
class OpenKosWorkflowTests {
    @Autowired Database db;@Autowired Leases49 leases;@Autowired Finance49 finance;@Autowired Billing49 billing;
    @Autowired Meters49 meters;@Autowired Access49 access;@Autowired Settlements49 settlements;@Autowired Api49 api;
    @Autowired Operations49 operations;@Autowired PlatformTransactionManager manager;
    long actor,property,room,person,contract,meter,service,account,advance,deposit;String suffix;
    static final String TAKEN="2026-01-10T09:00:00+07:00";
    void as(long user,String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(Long.toString(user),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
    <T>T tx(java.util.function.Supplier<T> body){return new TransactionTemplate(manager).execute(s->body.get());}
    void equalMoney(String expected,Object actual){assertEquals(0,new BigDecimal(expected).compareTo((BigDecimal)actual));}
    @BeforeEach void seed(){
        suffix=UUID.randomUUID().toString().substring(0,10);actor=db.insert("users","username,password_hash,role","ok-"+suffix,"unused","ADMIN");as(actor,"ADMIN");
        property=db.insert("properties","property_code,name,address","OK-"+suffix,"Workflow fixture","Local test");room=db.insert("rooms","property_id,room_code,max_occupants,opened_on",property,"101",3,LocalDate.of(2026,1,1));
        person=db.insert("tenant_profiles","full_name","Fixture tenant");account=db.insert("cash_accounts","property_id,account_code,name,account_type",property,"OK-CASH-"+suffix,"Cash","CASH");
        contract=id(leases.create(Map.of("roomId",room,"tenantId",person,"code","OK-C-"+suffix,"startDate","2026-01-01","endDate","2026-02-01","rentalPrice",0,"deposit",2000000,"signedAt","2025-12-30T10:00:00+07:00")),"id");
        db.jdbc.update("delete from contract_service_subscriptions where contract_id=?",contract);
        leases.possess(contract,Map.of("takenAt",TAKEN,"reason","Authorized fixture handover before full deposit"));
        service=db.insert("service_items","service_code,name,unit_name,calculation_type","OK-E-"+suffix,"Electricity","kWh","METER_BASED");
        db.insert("service_price_configs","property_id,service_item_id,effective_from,unit_price",property,service,LocalDate.of(2026,1,1),3500);
        meter=db.insert("meters","room_id,service_item_id,meter_code,installed_at,recorded_by",room,service,"OK-M-"+suffix,Input49.time("2026-01-01"),actor);
        advance=finance.ensureFund(contract,"ADVANCE");deposit=finance.ensureFund(contract,"DEPOSIT");
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    Map<String,Object> usage(String start,String end,int from,int to){return meters.usage(Map.of("meterId",meter,"contractId",contract,"startAt",start,"endAt",end,"oldIndex",from,"newIndex",to));}
    Map<String,Object> receive(int amount,String kind,String code){return finance.receive(Map.of("cashAccountId",account,"contractId",contract,"fundType",kind,"amount",amount,"code",code,"occurredAt","2026-01-11T10:00:00+07:00","confirmed",true));}
    long document(int amount){return tx(()->{long d=finance.createDocument(Map.of("contractId",contract,"code","OK-B-"+UUID.randomUUID(),"documentType","COMPENSATION"));finance.line(d,Map.of("name","Test charge","unitPrice",amount));finance.issue(d);return d;});}
    @Test void ok03_rejectsUsageBeforePhysicalHandover(){
        assertThrows(RuntimeException.class,()->usage("2026-01-01T00:00:00+07:00","2026-01-11T00:00:00+07:00",100,120));
        assertEquals(0,db.jdbc.queryForObject("select count(*) from consumption_segments where contract_id=?",Integer.class,contract));
    }
    @Test void ok04_rejectsUsageAcrossPhysicalReturn(){
        leases.checkout(contract,Map.of("billingEnd","2026-01-20","returnedAt","2026-01-20T09:00:00+07:00"));
        assertThrows(RuntimeException.class,()->usage("2026-01-19T09:00:00+07:00","2026-01-21T09:00:00+07:00",100,120));
        assertThrows(RuntimeException.class,()->usage("2026-01-20T09:00:00+07:00","2026-01-21T09:00:00+07:00",100,120));
    }
    @Test void ok03_databaseWriterAlsoCannotAssignBeforeHandover(){
        long a=id((Map<String,Object>)meters.reading(Map.of("meterId",meter,"observedAt","2026-01-01T00:00:00+07:00","readingValue",100)),"id");
        long b=id((Map<String,Object>)meters.reading(Map.of("meterId",meter,"observedAt","2026-01-11T00:00:00+07:00","readingValue",120)),"id");
        assertThrows(RuntimeException.class,()->db.insert("consumption_segments","start_reading_id,end_reading_id,contract_id,responsibility,recorded_by",a,b,contract,"TENANT",actor));
    }
    @Test void ok05_rejectsOverlapAndMismatchedOpeningPoint(){
        var first=usage(TAKEN,"2026-01-15T09:00:00+07:00",100,150);
        assertThrows(RuntimeException.class,()->usage("2026-01-14T09:00:00+07:00","2026-01-16T09:00:00+07:00",140,160));
        assertThrows(RuntimeException.class,()->usage("2026-01-15T09:00:00+07:00","2026-01-20T09:00:00+07:00",151,200));
        equalMoney("50",first.get("consumption"));
    }
    @Test void ok09_overpaymentRetryKeepsOneReceiptAndAdvance(){
        long d=document(3620000);var request=Map.<String,Object>of("code","OK-PAY-"+suffix,"amount",4000000,"cashAccountId",account,"occurredAt","2026-01-11T10:00:00+07:00","method","CASH");
        billing.pay(d,request);billing.pay(d,request);equalMoney("380000",access.fund(advance).get("balance"));equalMoney("0",finance.document(d).get("balance_due"));
        equalMoney("4000000",db.jdbc.queryForObject("select sum(amount) from cash_transactions where cash_account_id=? and direction='IN'",BigDecimal.class,account));
    }
    @Test void ok10_missingDepositNeedsDelegationButDoesNotInventMoney(){
        receive(1500000,"DEPOSIT","OK-DEP-"+suffix);equalMoney("1500000",access.fund(deposit).get("balance"));equalMoney("2000000",access.contract(contract).get("deposit_amount"));
        long staff=db.insert("users","username,password_hash,role","ok-staff-"+suffix,"unused","STAFF");db.insert("employee_assignments","user_id,room_id",staff,room);as(staff,"STAFF");
        assertThrows(RuntimeException.class,()->access.action(room,"WAIVE_DEPOSIT",new BigDecimal("500000")));
    }
    @Test void ok14_expiredPaperDoesNotReleasePhysicalRoom(){
        assertEquals("RENTED",access.room(room).get("display_status"));
        long other=db.insert("tenant_profiles","full_name","New renter");long next=id(leases.create(Map.of("roomId",room,"tenantId",other,"code","OK-NEXT-"+suffix,"startDate","2026-02-01","endDate","2027-02-01","rentalPrice",0,"deposit",0,"signedAt","2026-01-30T10:00:00+07:00")),"id");
        assertThrows(RuntimeException.class,()->leases.possess(next,Map.of("takenAt","2026-02-01T09:00:00+07:00")));
    }
    @Test void ok15_emptyRoomAndCommonRepairStayScopedAndDoNotCreateDebt(){
        long empty=db.insert("rooms","property_id,room_code,max_occupants,opened_on",property,"EMPTY",2,LocalDate.of(2026,1,1));
        var m=(Map<String,Object>)operations.create(Map.of("roomId",empty,"title","Vacant repair","description","Leak"));
        var common=(Map<String,Object>)operations.create(Map.of("propertyId",property,"title","Pump","description","Common area"));
        operations.progress(id(m,"id"),Map.of("status","ACCEPTED"));operations.progress(id(m,"id"),Map.of("status","IN_PROGRESS"));operations.cost(id(m,"id"),Map.of("description","Repair cost","amount",100000));operations.progress(id(m,"id"),Map.of("status","COMPLETED"));operations.progress(id(m,"id"),Map.of("status","IN_PROGRESS","note","Recurred"));
        assertEquals(0,db.jdbc.queryForObject("select count(*) from billing_documents where contract_id=?",Integer.class,contract));
        long staff=db.insert("users","username,password_hash,role","ok-limited-"+suffix,"unused","STAFF");db.insert("employee_assignments","user_id,room_id",staff,room);as(staff,"STAFF");
        assertThrows(RuntimeException.class,()->operations.detail(id(m,"id")));assertThrows(RuntimeException.class,()->operations.detail(id(common,"id")));
    }
}
