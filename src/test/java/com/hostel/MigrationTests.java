package com.hostel;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class MigrationTests {
 String name,url;JdbcTemplate db,server;
 String user=System.getenv().getOrDefault("DB_USERNAME","postgres"),password=System.getenv().getOrDefault("DB_PASSWORD","postgres");
 @BeforeEach void database(){String source=System.getenv().getOrDefault("DB_URL","jdbc:postgresql://localhost:5432/hostel_test");String prefix=source.substring(0,source.lastIndexOf('/')+1);name="hostel_migration_"+UUID.randomUUID().toString().replace("-","").substring(0,12);server=new JdbcTemplate(new DriverManagerDataSource(prefix+"postgres",user,password));server.execute("create database "+name);url=prefix+name;db=new JdbcTemplate(new DriverManagerDataSource(url,user,password));Flyway.configure().dataSource(url,user,password).target("9").load().migrate();}
 @AfterEach void cleanup(){if(server!=null&&name!=null&&name.matches("hostel_migration_[a-f0-9]{12}"))server.execute("drop database "+name);}
 void legacy(){
  db.update("insert into users(id,username,password_hash,full_name,phone,role) values(1,'fixture','hash','Owner','0900000001','ADMIN')");
  db.update("insert into properties(id,property_code,name,address) values(1,'P','House','Address')");
  db.update("insert into floors(id,property_id,floor_number,name) values(1,1,2,'Second')");
  db.update("insert into rooms(id,property_id,floor_id,room_code,base_price,max_occupants) values(1,1,1,'201',1000,4)");
  db.update("insert into tenant_profiles(id,full_name,identity_number,phone) values(1,'Tenant','123456789','0900000002')");
  db.update("insert into contracts(id,contract_code,room_id,primary_tenant_id,start_date,end_date,rental_price,deposit_amount,payment_cycle_day,initial_electric_index,initial_water_index) values(1,'C1',1,1,'2026-01-01','2027-01-01',1000,1000,5,0,0)");
  db.update("insert into contract_members(id,contract_id,full_name,identity_number,phone,relationship,effective_from) values(1,1,'Friend','987654321','0900000003','Friend','2026-01-01'),(2,1,'Child',null,null,'Child','2026-01-01')");
  db.update("insert into service_price_configs(property_id,service_item_id,unit_price,effective_from) select 1,id,2,'2026-01-01' from service_items where service_code='ELEC'");
  db.update("insert into meter_readings(id,room_id,contract_id,billing_period,meter_type,old_index,new_index,consumption,reading_date,recorded_by) values(1,1,1,'2026-01','ELECTRICITY',0,10,10,'2026-01-31',1)");
  db.update("insert into invoices(id,invoice_code,contract_id,billing_period,issue_date,due_date,total_amount,paid_amount,balance_due,payment_status,status) values(1,'I1',1,'2026-01','2026-02-01','2026-02-05',1020,100,920,'PARTIALLY_PAID','ISSUED')");
  db.update("insert into invoice_items(id,invoice_id,item_name_snapshot,unit_price_snapshot,quantity,subtotal) values(1,1,'Rent',1000,1,1000)");
  db.update("insert into invoice_items(id,invoice_id,service_item_id,item_name_snapshot,unit_price_snapshot,quantity,old_index_snapshot,new_index_snapshot,subtotal) select 2,1,id,'Electricity',2,10,0,10,20 from service_items where service_code='ELEC'");
  db.update("insert into payment_transactions(id,invoice_id,transaction_code,amount,payment_method,received_by) values(1,1,'PAY',100,'CASH',1)");
  db.update("insert into maintenance_requests(id,contract_id,property_id,room_id,submitted_by,title,description,priority,image_urls,expected_date) values(1,1,1,1,1,'Repair','Details','NORMAL',E'https://example.test/1.png\\nhttps://example.test/2.png','2026-03-01')");
 }
 void cutover(){Flyway.configure().dataSource(url,user,password).load().migrate();}
 @Test void existingDataIsArchivedNormalizedAndReconciled(){legacy();cutover();assertEquals(17,db.queryForObject("select count(*)::int from pg_tables where schemaname='public' and tablename<>'flyway_schema_history'",Integer.class));assertEquals(29,db.queryForObject("select count(*)::int from pg_tables where schemaname='prototype_v9'",Integer.class));assertEquals(2,db.queryForObject("select floor_number::int from rooms where id=1",Integer.class));assertEquals(3,db.queryForObject("select count(*)::int from tenant_profiles",Integer.class));assertEquals(2,db.queryForObject("select count(*)::int from maintenance_images",Integer.class));assertEquals(920,db.queryForObject("select balance_due::int from v_invoice_balances where id=1",Integer.class));assertEquals(100,db.queryForObject("select cash_received::int from v_invoice_balances where id=1",Integer.class));assertEquals(1L,db.queryForObject("select meter_reading_id from invoice_items where id=2",Long.class));assertThrows(Exception.class,()->db.update("update invoice_items set unit_price_snapshot=1 where id=1"));assertEquals(2,db.queryForObject("select count(*)::int from prototype_v9.contract_members",Integer.class));assertTrue(db.queryForObject("select max(id)<nextval(pg_get_serial_sequence('tenant_profiles','id')) from tenant_profiles",Boolean.class));}
 @Test void incompatibleLedgerRollsBackWholeCutover(){legacy();db.update("update invoice_items set subtotal=21 where id=2");db.update("update invoices set total_amount=1021,balance_due=921 where id=1");assertThrows(Exception.class,this::cutover);assertEquals(29,db.queryForObject("select count(*)::int from pg_tables where schemaname='public' and tablename<>'flyway_schema_history'",Integer.class));assertEquals(0,db.queryForObject("select count(*)::int from pg_namespace where nspname='prototype_v9'",Integer.class));assertEquals(921,db.queryForObject("select balance_due::int from invoices where id=1",Integer.class));}
 @Test void oldRoomAssignmentsNeverExpandToWholeProperty(){legacy();db.update("insert into users(id,username,password_hash,full_name,phone,role) values(2,'staff','hash','Staff','0900000004','STAFF')");db.update("insert into room_employee_assignments(user_id,room_id) values(2,1)");cutover();assertFalse(db.queryForObject("select can_manage_property(2,1)",Boolean.class));assertEquals(1,db.queryForObject("select count(*)::int from prototype_v9.room_employee_assignments",Integer.class));}
 @Test void localImagesKeepTheirPrivateFileReference(){legacy();String image="c43507bf-f8a4-46d5-9fb1-d15f68a0884f";db.update("insert into uploaded_images(id,maintenance_request_id,original_name,content_type,extension,size_bytes,uploaded_by) values(?::uuid,1,'picture.png','image/png','png',10,1)",image);db.update("update maintenance_requests set image_urls=? where id=1","/api/uploads/"+image);cutover();assertEquals("/api/uploads/"+image+".png",db.queryForObject("select url from maintenance_images where maintenance_request_id=1",String.class));}
}
