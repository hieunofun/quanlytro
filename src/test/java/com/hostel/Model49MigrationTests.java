package com.hostel;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Populated V9 -> V10 fixture, then the real forward migration to V11. */
class Model49MigrationTests {
    MigrationTests previous;
    @BeforeEach void seed(){previous=new MigrationTests();previous.database();previous.legacy();previous.cutover();}
    @AfterEach void close(){if(previous!=null)previous.cleanup();}
    void migrate(){Flyway.configure().dataSource(previous.url,previous.user,previous.password).target("11").load().migrate();}
    int number(String sql){return previous.db.queryForObject(sql,Integer.class);}
    @Test void populatedLedgerPreservesDebtAndActualCashWithoutInventingDeposit(){
        migrate();assertEquals(49,number("select count(*) from pg_tables where schemaname='public' and tablename<>'flyway_schema_history'"));
        assertEquals(17,number("select count(*) from pg_tables where schemaname='model_v10'"));
        assertEquals(920,number("select balance_due from v_document_balances where id=1"));
        assertEquals(100,number("select sum(amount) from cash_transactions where direction='IN'"));
        assertEquals(0,number("select balance from v_fund_balances where fund_type='DEPOSIT'"));
        assertEquals("PENDING",previous.db.queryForObject("select reconciliation_status from fund_accounts where fund_type='DEPOSIT'",String.class));
        assertEquals(0,number("select count(*) from room_possessions"));assertEquals(0,number("select count(*) from contract_terms where signed_at is not null"));
        assertEquals(2,number("select count(*) from contract_residencies"));assertEquals(2,number("select count(*) from file_attachments"));
        assertThrows(Exception.class,()->previous.db.update("update model_v10.contracts set terms='changed' where id=1"));
    }
    @Test void historicDepositOffsetRemainsAllocationRatherThanNewCash()throws Exception{
        try(var c=java.sql.DriverManager.getConnection(previous.url,previous.user,previous.password)){c.setAutoCommit(false);try(var s=c.createStatement()){
            s.executeUpdate("update contracts set status='TERMINATED',actual_checkout_date='2026-02-01' where id=1");
            s.executeUpdate("insert into checkout_records(contract_id,deposit_snapshot,outstanding_snapshot,damage_amount,settled_by) values(1,1000,920,50,1)");c.commit();
        }}
        previous.db.update("insert into payment_transactions(invoice_id,transaction_code,amount,payment_method,received_by) values(1,'OFFSET',200,'DEPOSIT_OFFSET',1)");
        migrate();assertEquals(720,number("select balance_due from v_document_balances where id=1"));assertEquals(300,number("select paid_amount from v_document_balances where id=1"));
        assertEquals(100,number("select sum(amount) from cash_transactions where direction='IN'"));assertEquals(0,number("select balance from v_fund_balances where fund_type='DEPOSIT'"));
    }
    @Test void anyImportFailureRollsBackArchiveAndKeepsV10Readable(){
        // Simulate a damaged historical image reference; V11 must fail atomically, not drop it.
        previous.db.execute("alter table maintenance_images drop constraint maintenance_images_uploaded_by_fkey");
        previous.db.execute("alter table maintenance_images disable trigger user");
        previous.db.update("update maintenance_images set uploaded_by=999999 where id=(select min(id) from maintenance_images)");
        assertThrows(Exception.class,this::migrate);
        assertEquals(17,number("select count(*) from pg_tables where schemaname='public' and tablename<>'flyway_schema_history'"));
        assertEquals(0,number("select count(*) from pg_namespace where nspname='model_v10'"));assertEquals(920,number("select balance_due from v_invoice_balances where id=1"));
    }
}
