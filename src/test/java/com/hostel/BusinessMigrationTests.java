package com.hostel;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class BusinessMigrationTests {
    MigrationTests fixture;
    @BeforeEach void prepare(){fixture=new MigrationTests();fixture.database();fixture.legacy();fixture.cutover();}
    @AfterEach void cleanup(){if(fixture!=null)fixture.cleanup();}
    @Test void populatedV10UpgradesThroughV13WithoutInventingCashOrPaymentIntent(){
        Flyway.configure().dataSource(fixture.url,fixture.user,fixture.password).target("13").load().migrate();
        assertEquals(49,fixture.db.queryForObject("select count(*) from pg_tables where schemaname='public' and tablename<>'flyway_schema_history'",Integer.class));
        assertEquals(0,new BigDecimal("920").compareTo(fixture.db.queryForObject("select balance_due from v_document_balances where id=1",BigDecimal.class)));
        assertEquals(0,new BigDecimal("100").compareTo(fixture.db.queryForObject("select sum(amount) from cash_transactions where direction='IN'",BigDecimal.class)));
        assertEquals(0,fixture.db.queryForObject("select count(*) from cash_transactions where payment_document_id is not null",Integer.class));
        assertEquals(0,fixture.db.queryForObject("select count(*) from room_possessions",Integer.class));
        assertEquals(0,fixture.db.queryForObject("select count(*) from contract_terms where signed_at is not null",Integer.class));
        assertEquals(1,fixture.db.queryForObject("select count(*) from flyway_schema_history where version='13' and success",Integer.class));
        assertThrows(Exception.class,()->fixture.db.update("update model_v10.contracts set terms='tampered' where id=1"));
        // Re-running migrations validates checksums and preserves both archive and ledger.
        assertEquals(0,Flyway.configure().dataSource(fixture.url,fixture.user,fixture.password).target("13").load().migrate().migrationsExecuted);
    }
}
