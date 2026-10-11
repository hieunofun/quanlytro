package com.hostel;
import com.hostel.dto.Requests.*;
import com.hostel.security.Access;
import com.hostel.service.*;
import com.hostel.controller.CatalogController;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.hostel.service.Database.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
class BusinessWorkflowTests {
    @Autowired Database db;@Autowired ContractService contracts;@Autowired MeterService meters;@Autowired InvoiceService invoices;@Autowired Access access;@Autowired CatalogController catalog;
    @Autowired com.hostel.controller.ReportController reports;
    long admin,tenantUser,tenant,property,room,contract;
    static BigDecimal b(String value) {return new BigDecimal(value);}
    void login(long user,String role) {SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(Long.toString(user),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
    @BeforeEach void seed() {
        String key=UUID.randomUUID().toString();
        admin=db.insert("users","username,password_hash,full_name,phone,role","a"+key,"unused","Admin",key.substring(0,18),"ADMIN");
        tenantUser=db.insert("users","username,password_hash,full_name,phone,role","t"+key,"unused","Tenant","T"+key.substring(0,18),"TENANT");
        login(admin,"ADMIN");
        property=db.insert("properties","property_code,name,address",key.substring(0,20),"Demo","HCM");
        long floor=db.insert("floors","property_id,floor_number,name",property,2,"Tầng 2");
        room=db.insert("rooms","property_id,floor_id,room_code,base_price,max_occupants",property,floor,"203",b("3000000"),2);
        tenant=db.insert("tenant_profiles","user_id,full_name,identity_number,phone",tenantUser,"Khách thuê","123456789012","0900000000");
        var c=(Map<?,?>)contracts.create(new Contract(key,room,tenant,LocalDate.of(2026,1,1),LocalDate.of(2027,1,1),b("3000000"),b("3000000"),5,b("100"),b("120")));
        contract=((Number)c.get("id")).longValue();
        for(var s:db.jdbc.queryForList("select * from service_items where is_mandatory")) {
            String type=s.get("service_type").toString();
            db.insert("service_price_configs","property_id,service_item_id,unit_price,effective_from",property,s.get("id"),b(type.equals("WATER")?"20000":type.equals("ELECTRICITY")?"3500":"30000"),LocalDate.of(2026,1,1));
        }
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    void readings() {meters.record(new Reading(contract,"2026-01","ELECTRICITY",b("200"),LocalDate.of(2026,1,31)));meters.record(new Reading(contract,"2026-01","WATER",b("128"),LocalDate.of(2026,1,31)));}
    long draft() {readings();return id(invoices.create(new Invoice(contract,"2026-01",LocalDate.of(2026,2,1),LocalDate.of(2026,2,5))),"id");}
    @Test void invoiceFormulaAndPartialPayment() {
        long id=draft();var detail=invoices.issue(id);assertEquals(0,money(detail,"total_amount").compareTo(b("3540000")));
        var paid=invoices.pay(id,new Payment("PAY-"+UUID.randomUUID(),b("2000000"),"CASH","Demo"));
        assertEquals("PARTIALLY_PAID",paid.get("payment_status"));assertEquals(0,money(paid,"balance_due").compareTo(b("1540000")));
    }
    @Test void sqlDatesBecomeCalendarDatesInApiMaps() {
        var invoice=invoices.detail(draft());
        assertEquals(LocalDate.of(2026,2,1),invoice.get("issue_date"));
        assertEquals(LocalDate.of(2026,2,5),invoice.get("due_date"));
    }
    @Test void reportsGroupQuarterAndMeasureHistoricalOccupancy() {
        long invoiceId=draft();invoices.issue(invoiceId);
        var quarters=(List<Map<String,Object>>)reports.quarterly();
        assertTrue(quarters.stream().anyMatch(q -> "2026-Q1".equals(q.get("quarter")) && money(q,"billed").compareTo(b("3540000"))>=0));
        var history=(List<Map<String,Object>>)reports.occupancyHistory();
        assertFalse(history.isEmpty());assertTrue(history.stream().anyMatch(row -> "Demo".equals(row.get("name"))));
    }
    @Test void invoiceSnapshotsSurvivePriceChange() {
        long id=draft();invoices.issue(id);BigDecimal before=money(invoices.detail(id),"total_amount");
        db.jdbc.update("update service_price_configs set unit_price=unit_price*2 where property_id=?",property);
        assertEquals(before,money(invoices.detail(id),"total_amount"));assertThrows(Exception.class,()->invoices.removeDraft(id));
    }
    @Test void duplicateInvoiceRejected() {draft();assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->invoices.create(new Invoice(contract,"2026-01",LocalDate.of(2026,2,1),LocalDate.of(2026,2,5))));}
    @Test void meterCannotGoBackwards() {assertThrows(Exception.class,()->meters.record(new Reading(contract,"2026-01","WATER",b("119"),LocalDate.of(2026,1,31))));}
    @Test void correctionHasHistoryAndCannotChangeBilledReading() {
        var m=meters.record(new Reading(contract,"2026-01","WATER",b("150"),LocalDate.of(2026,1,31)));
        meters.edit(id(m,"id"),b("128"),"Nhập nhầm");assertEquals(1,db.jdbc.queryForObject("select count(*)::int from meter_reading_history where meter_reading_id=?",Integer.class,id(m,"id")));
        meters.record(new Reading(contract,"2026-01","ELECTRICITY",b("200"),LocalDate.of(2026,1,31)));invoices.create(new Invoice(contract,"2026-01",LocalDate.of(2026,2,1),LocalDate.of(2026,2,5)));
        assertThrows(Exception.class,()->meters.edit(id(m,"id"),b("129"),"Too late"));
    }
    @Test void anomalyBlocksIssueUntilReviewed() {
        meters.record(new Reading(contract,"2026-01","WATER",b("128"),LocalDate.of(2026,1,31)));
        meters.record(new Reading(contract,"2026-02","WATER",b("137"),LocalDate.of(2026,2,28)));
        meters.record(new Reading(contract,"2026-03","WATER",b("147"),LocalDate.of(2026,3,31)));
        var m=meters.record(new Reading(contract,"2026-04","WATER",b("174"),LocalDate.of(2026,4,30)));assertEquals(true,m.get("has_anomaly"));
        meters.record(new Reading(contract,"2026-04","ELECTRICITY",b("200"),LocalDate.of(2026,4,30)));
        long id=id(invoices.create(new Invoice(contract,"2026-04",LocalDate.of(2026,5,1),LocalDate.of(2026,5,5))),"id");
        assertThrows(Exception.class,()->invoices.issue(id));db.jdbc.update("update consumption_alerts set status='CONFIRMED' where meter_reading_id=?",id(m,"id"));assertEquals("ISSUED",invoices.issue(id).get("status"));
    }
    @Test void tenantCannotReadOtherContractOrCreateInvoice() {
        login(tenantUser,"TENANT");assertEquals(contract,id(access.contract(contract),"id"));assertThrows(Exception.class,()->invoices.create(new Invoice(contract,"2026-01",LocalDate.of(2026,2,1),LocalDate.of(2026,2,5))));
        long stranger=db.insert("users","username,password_hash,full_name,phone,role","x"+UUID.randomUUID(),"unused","Other",UUID.randomUUID().toString().substring(0,18),"TENANT");
        login(stranger,"TENANT");assertThrows(Exception.class,()->access.contract(contract));
    }
    @Test void staffRequiresAssignedProperty() {
        long staff=db.insert("users","username,password_hash,full_name,phone,role","s"+UUID.randomUUID(),"unused","Staff",UUID.randomUUID().toString().substring(0,18),"STAFF");
        login(staff,"STAFF");assertThrows(Exception.class,()->access.contract(contract));db.insert("employee_assignments","user_id,property_id",staff,property);assertEquals(contract,id(access.contract(contract),"id"));
    }
    @Test void overpaymentRejected() {long id=draft();invoices.issue(id);assertThrows(Exception.class,()->invoices.pay(id,new Payment("EXCESS",b("9999999"),"CASH",null)));}
    @Test void checkoutAllocatesDepositAndReleasesRoom() {
        long id=draft();invoices.issue(id);invoices.pay(id,new Payment("PARTIAL-"+UUID.randomUUID(),b("2000000"),"CASH",null));
        var result=(Map<String,Object>)contracts.checkout(contract,new Checkout(LocalDate.of(2026,2,1),b("200"),b("128"),b("100000"),"Demo"));
        assertEquals(0,money(result,"actual_refund_amount").compareTo(b("1360000")));assertEquals("AVAILABLE",db.one("select status from rooms where id=?",room).get("status"));assertEquals("PAID",invoices.detail(id).get("payment_status"));
    }
    @Test void roomCannotHaveTwoActiveContracts() {assertThrows(Exception.class,()->contracts.create(new Contract("SECOND",room,tenant,LocalDate.of(2026,2,1),LocalDate.of(2027,2,1),b("3000000"),b("0"),5,b("0"),b("0"))));}
    @Test void prorationForMidMonthMoveIn() {
        db.jdbc.update("update contracts set start_date='2026-01-16' where id=?",contract);long id=draft();
        var lines=(List<Map<String,Object>>)invoices.detail(id).get("items");assertEquals(0,money(lines.getFirst(),"subtotal").compareTo(b("1548387.10")));
    }
    @Test void perPersonServiceUsesMembershipDates() {
        long parking=db.jdbc.queryForObject("select id from service_items where service_code='PARKING'",Long.class);
        db.insert("service_price_configs","property_id,service_item_id,unit_price,effective_from",property,parking,b("31000"),LocalDate.of(2026,1,1));
        contracts.member(contract,new Member("Bạn cùng phòng","123","0901","Bạn",LocalDate.of(2026,1,16)));
        long invoiceId=draft();var items=(List<Map<String,Object>>)invoices.detail(invoiceId).get("items");
        var fee=items.stream().filter(i->i.get("item_name_snapshot").equals("Gửi xe máy")).findFirst().orElseThrow();
        assertEquals(0,money(fee,"subtotal").compareTo(b("47000")));
    }

    @Autowired com.hostel.controller.ContractController contractApi;
    @Autowired com.hostel.controller.MaintenanceController maintenance;
    @Autowired BillingCalendar calendar;
    @Test void individualRoomAssignmentRestrictsListsReportsAndWrites() {
        long sibling=db.insert("rooms","property_id,floor_id,room_code,base_price,max_occupants",property,db.one("select floor_id from rooms where id=?",room).get("floor_id"),"204",b("2000000"),2);
        long staff=db.insert("users","username,password_hash,full_name,phone,role","room"+UUID.randomUUID(),"unused","Staff",UUID.randomUUID().toString().substring(0,18),"STAFF");
        long request=id((Map<String,Object>)maintenance.create(new com.hostel.controller.MaintenanceController.Request(contract,"Hỏng khóa","Kiểm tra","LOCK","NORMAL",null)),"id");
        db.insert("room_employee_assignments","user_id,room_id",staff,room);login(staff,"STAFF");
        assertEquals(contract,id(access.contract(contract),"id"));
        assertEquals(1,((List<?>)catalog.rooms()).size());assertEquals(1,((Number)((Map<?,?>)reports.dashboard("2026-01")).get("total_rooms")).intValue());
        var occupancy=(List<Map<String,Object>>)reports.occupancy();assertEquals(1,((Number)occupancy.getFirst().get("total_rooms")).intValue());
        assertEquals(1,((List<?>)maintenance.list()).size());assertEquals(request,id(access.maintenance(request),"id"));
        assertThrows(Exception.class,()->access.room(sibling));assertThrows(Exception.class,()->access.property(property));
        assertThrows(Exception.class,()->catalog.policy(property,new CatalogController.Policy(31,1,false,b("0"))));
        assertFalse(access.canManageRoom(staff,sibling));assertTrue(access.canManageRoom(staff,room));
        db.jdbc.update("update room_employee_assignments set is_active=false where user_id=?",staff);
        assertThrows(Exception.class,()->access.contract(contract));assertTrue(((List<?>)catalog.rooms()).isEmpty());
    }
    @Test void violationRequiresReasonHasWorkflowAndTenantCannotResolve() {
        var v=(Map<String,Object>)contractApi.violation(contract,new com.hostel.controller.ContractController.Violation("Vi phạm giờ yên tĩnh"));long violation=id(v,"id");
        assertEquals("OPEN",v.get("status"));
        assertEquals("HANDLING",((Map<?,?>)contractApi.resolve(contract,violation,new com.hostel.controller.ContractController.Resolution("HANDLING","Đã trao đổi với người thuê"))).get("status"));
        login(tenantUser,"TENANT");assertEquals(1,((List<?>)contracts.detail(contract).get("violations")).size());
        assertThrows(Exception.class,()->contractApi.resolve(contract,violation,new com.hostel.controller.ContractController.Resolution("RESOLVED","Đã khắc phục")));
        login(admin,"ADMIN");contractApi.resolve(contract,violation,new com.hostel.controller.ContractController.Resolution("RESOLVED","Đã khắc phục"));
        assertThrows(Exception.class,()->contractApi.resolve(contract,violation,new com.hostel.controller.ContractController.Resolution("HANDLING","Mở lại")));
    }
    @Test void calendarClampsFebruaryAndMovesDueDateAfterIssue() {
        catalog.policy(property,new CatalogController.Policy(31,10,false,b("0")));
        var dates=calendar.dates(contract,"2026-02");
        assertEquals(LocalDate.of(2026,2,28),dates.get("cutoffDate"));assertEquals(LocalDate.of(2026,3,10),dates.get("issueDate"));assertEquals(LocalDate.of(2026,4,5),dates.get("dueDate"));
    }
    @Test void billingDatesAreAppliedAndCustomInconsistentDatesRejected() {
        catalog.policy(property,new CatalogController.Policy(31,3,false,b("0")));
        assertThrows(Exception.class,()->meters.record(new Reading(contract,"2026-01","WATER",b("128"),LocalDate.of(2026,1,30))));
        readings();assertThrows(Exception.class,()->invoices.create(new Invoice(contract,"2026-01",LocalDate.of(2026,2,1),null)));
        var i=invoices.create(new Invoice(contract,"2026-01",null,null));assertEquals(LocalDate.of(2026,2,3),i.get("issue_date"));assertEquals(LocalDate.of(2026,2,5),i.get("due_date"));
    }
    @Test void futureInvoiceCannotBeIssuedBeforeScheduledDate() {
        var next=java.time.YearMonth.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusMonths(1);db.jdbc.update("update contracts set end_date=? where id=?",next.plusMonths(2).atDay(1),contract);
        meters.record(new Reading(contract,next.toString(),"ELECTRICITY",b("200"),next.atEndOfMonth()));meters.record(new Reading(contract,next.toString(),"WATER",b("128"),next.atEndOfMonth()));
        long invoice=id(invoices.create(new Invoice(contract,next.toString(),null,null)),"id");assertThrows(Exception.class,()->invoices.issue(invoice));
    }
    @Test void midMonthCheckoutUsesFinalReadingAndProratesOutsideNormalCalendar() {
        var result=(Map<String,Object>)contracts.checkout(contract,new Checkout(LocalDate.of(2026,1,16),b("120"),b("122"),b("0"),"Trả giữa tháng"));
        assertEquals("COMPLETED",result.get("status"));var i=db.one("select * from invoices where contract_id=?",contract);
        assertEquals(true,i.get("is_settlement"));assertEquals(LocalDate.of(2026,1,15),i.get("issue_date"));assertEquals("AVAILABLE",db.one("select status from rooms where id=?",room).get("status"));
    }
    @Test void reportsUseReceiptMonthAndExcludeInternalDepositOffsets() {
        long invoice=draft();invoices.issue(invoice);String code="CASH-"+UUID.randomUUID();
        invoices.pay(invoice,new Payment(code,b("2000000"),"CASH",null));
        db.jdbc.update("update payment_transactions set payment_date='2026-02-03T12:00:00+07' where transaction_code=?",code);
        invoices.pay(invoice,new Payment("OFFSET-"+UUID.randomUUID(),b("1540000"),"DEPOSIT_OFFSET","Cấn cọc"));
        var january=(Map<String,Object>)reports.dashboard("2026-01");var february=(Map<String,Object>)reports.dashboard("2026-02");
        assertEquals(0,money(january,"collected").compareTo(b("0")));assertEquals(0,money(january,"credited").compareTo(b("3540000")));
        assertEquals(0,money(february,"collected").compareTo(b("2000000")));
        var revenue=(List<Map<String,Object>>)reports.revenue();assertTrue(revenue.stream().anyMatch(r->"2026-02".equals(r.get("billing_period")) && money(r,"collected").compareTo(b("2000000"))==0));
    }
    @Test void successiveTenantsCanBeBilledSeparatelyInOneMonth() {
        contracts.checkout(contract,new Checkout(LocalDate.of(2026,1,16),b("120"),b("122"),b("0"),"First tenant"));
        long next=id((Map<String,Object>)contracts.create(new Contract("TURNOVER-"+UUID.randomUUID(),room,tenant,LocalDate.of(2026,1,16),LocalDate.of(2027,1,1),b("3000000"),b("3000000"),5,b("120"),b("122"))),"id");
        var electric=meters.record(new Reading(next,"2026-01","ELECTRICITY",b("125"),LocalDate.of(2026,1,31)));
        var water=meters.record(new Reading(next,"2026-01","WATER",b("123"),LocalDate.of(2026,1,31)));
        assertEquals(0,money(electric,"consumption").compareTo(b("5")));assertEquals(0,money(water,"consumption").compareTo(b("1")));
        var invoice=invoices.create(new Invoice(next,"2026-01",null,null));invoices.issue(id(invoice,"id"));
        var rent=(List<Map<String,Object>>)invoice.get("items");assertEquals(0,money(rent.getFirst(),"subtotal").compareTo(b("1548387.10")));
        assertEquals(2,db.jdbc.queryForObject("select count(*)::int from meter_readings where room_id=? and billing_period='2026-01' and meter_type='WATER'",Integer.class,room));
    }
    @Test void duplicateMeterReadingRemainsRejectedWithinSameContract() {
        meters.record(new Reading(contract,"2026-01","WATER",b("128"),LocalDate.of(2026,1,31)));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->meters.record(new Reading(contract,"2026-01","WATER",b("129"),LocalDate.of(2026,1,31))));
    }
    @Test void departedMemberCannotBeMovedToAnotherMonthAfterInvoiceIssued() {
        long parking=db.jdbc.queryForObject("select id from service_items where service_code='PARKING'",Long.class);
        db.insert("service_price_configs","property_id,service_item_id,unit_price,effective_from",property,parking,b("31000"),LocalDate.of(2026,1,1));
        long member=id((Map<String,Object>)contracts.member(contract,new Member("Member","123","0901","Friend",LocalDate.of(2026,1,1))),"id");
        contractApi.depart(contract,member,new com.hostel.controller.ContractController.Departure(LocalDate.of(2026,1,16)));
        long invoice=draft();invoices.issue(invoice);
        assertThrows(Exception.class,()->contractApi.depart(contract,member,new com.hostel.controller.ContractController.Departure(LocalDate.of(2026,2,16))));
        assertEquals(LocalDate.of(2026,1,16),db.one("select effective_to from contract_members where id=?",member).get("effective_to"));
        var items=(List<Map<String,Object>>)invoices.detail(invoice).get("items");
        assertEquals(0,money(items.stream().filter(i->"Gửi xe máy".equals(i.get("item_name_snapshot"))).findFirst().orElseThrow(),"subtotal").compareTo(b("46000")));
    }
    @Test void departureCannotBeRecordedForAnEndedContract() {
        long member=id((Map<String,Object>)contracts.member(contract,new Member("Member",null,null,null,LocalDate.of(2026,1,1))),"id");
        db.jdbc.update("update contracts set status='TERMINATED' where id=?",contract);
        assertThrows(Exception.class,()->contractApi.depart(contract,member,new com.hostel.controller.ContractController.Departure(LocalDate.of(2026,1,16))));
        assertNull(db.one("select effective_to from contract_members where id=?",member).get("effective_to"));
    }
    @Test void checkoutExposesUtilitiesAndRefundWithoutDeductingUtilitiesTwice() {
        var result=(Map<String,Object>)contracts.checkout(contract,new Checkout(LocalDate.of(2026,1,16),b("120"),b("122"),b("100000"),"Settlement"));
        assertEquals(0,money(result,"final_utility_amount").compareTo(b("110000")));
        BigDecimal expected=b("3000000").subtract(money(result,"unpaid_invoices_amount")).subtract(b("100000"));
        assertEquals(0,money(result,"actual_refund_amount").compareTo(expected));
        assertEquals(result,contracts.detail(contract).get("checkout"));
        login(tenantUser,"TENANT");assertEquals(result,contracts.detail(contract).get("checkout"));
    }
    Room roomRequest(int capacity) {
        return new Room(property,id(db.one("select floor_id from rooms where id=?",room),"floor_id"),"203",b("3000000"),capacity,b("25"));
    }
    @Test void roomCapacityIgnoresMembersWhoAlreadyLeft() {
        LocalDate today=LocalDate.now();db.jdbc.update("update contracts set start_date=?,end_date=? where id=?",today.minusMonths(2),today.plusYears(1),contract);
        long member=id((Map<String,Object>)contracts.member(contract,new Member("Departed",null,null,null,today.minusMonths(1))),"id");
        contractApi.depart(contract,member,new com.hostel.controller.ContractController.Departure(today.minusDays(1)));
        catalog.roomUpdate(room,roomRequest(1));assertEquals(1,((Number)db.one("select max_occupants from rooms where id=?",room).get("max_occupants")).intValue());
    }
    @Test void roomCapacityProtectsFutureArrivalsButCountsOnlyOverlappingMembers() {
        LocalDate today=LocalDate.now();db.jdbc.update("update contracts set start_date=?,end_date=? where id=?",today.minusMonths(1),today.plusYears(1),contract);
        long member=id((Map<String,Object>)contracts.member(contract,new Member("Future one",null,null,null,today.plusDays(10))),"id");
        contractApi.depart(contract,member,new com.hostel.controller.ContractController.Departure(today.plusDays(20)));
        contracts.member(contract,new Member("Future two",null,null,null,today.plusDays(21)));
        catalog.roomUpdate(room,roomRequest(2));assertThrows(Exception.class,()->catalog.roomUpdate(room,roomRequest(1)));
    }
    @Test void anomalyBaselineSumsSuccessiveTenantsWithinEachHistoricalMonth() {
        contracts.checkout(contract,new Checkout(LocalDate.of(2026,1,16),b("120"),b("122"),b("0"),"First"));
        long next=id((Map<String,Object>)contracts.create(new Contract("BASELINE-"+UUID.randomUUID(),room,tenant,LocalDate.of(2026,1,16),LocalDate.of(2026,2,1),b("3000000"),b("3000000"),5,b("120"),b("122"))),"id");
        meters.record(new Reading(next,"2026-01","WATER",b("132"),LocalDate.of(2026,1,31)));
        meters.record(new Reading(next,"2026-01","ELECTRICITY",b("140"),LocalDate.of(2026,1,31)));
        contracts.checkout(next,new Checkout(LocalDate.of(2026,2,1),b("140"),b("132"),b("0"),"Second"));
        long third=id((Map<String,Object>)contracts.create(new Contract("BASELINE-"+UUID.randomUUID(),room,tenant,LocalDate.of(2026,2,1),LocalDate.of(2027,1,1),b("3000000"),b("3000000"),5,b("140"),b("132"))),"id");
        var reading=meters.record(new Reading(third,"2026-02","WATER",b("148"),LocalDate.of(2026,2,28)));
        assertEquals(false,reading.get("has_anomaly")); // 16 vs total January 12: +33.33%, below 50%.
    }
}
