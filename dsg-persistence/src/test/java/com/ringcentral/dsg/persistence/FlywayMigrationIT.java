package com.ringcentral.dsg.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class FlywayMigrationIT {

    private static final int EXPECTED_TABLE_COUNT = 31;

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("dsb")
            .withUsername("dsg")
            .withPassword("dsg_dev");

    @Test
    void migrationsApplyAllTablesAndSeedData() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement stmt = conn.createStatement()) {

            ResultSet tables = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' "
                            + "AND table_name <> 'flyway_schema_history'");
            tables.next();
            assertEquals(EXPECTED_TABLE_COUNT, tables.getInt(1),
                    "DSB should have 31 tables (wiki + account_directory_oauth + default_attribute_mapping)");

            ResultSet defaults = stmt.executeQuery("SELECT COUNT(*) FROM default_attribute_mapping");
            defaults.next();
            assertEquals(30, defaults.getInt(1));

            ResultSet dirs = stmt.executeQuery("SELECT COUNT(*) FROM directory_type");
            dirs.next();
            assertEquals(4, dirs.getInt(1));

            ResultSet states = stmt.executeQuery("SELECT COUNT(*) FROM job_state");
            states.next();
            assertEquals(10, states.getInt(1));

            ResultSet oauth = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() "
                            + "AND table_name = 'account_directory_oauth' "
                            + "AND column_name = 'client_secret_enc'");
            oauth.next();
            assertTrue(oauth.getInt(1) >= 1, "OAuth extension table must exist");
        }
    }
}
