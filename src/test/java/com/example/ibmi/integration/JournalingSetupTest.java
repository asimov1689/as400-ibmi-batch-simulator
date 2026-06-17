package com.example.ibmi.integration;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("vscode")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JournalingSetupTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    @Order(0)
    @DisplayName("Setup: create ORDERQ data queue if absent")
    void createDataQueue() {
        try {
            jdbc.execute(
                    "CALL QSYS2.QCMDEXC('CRTDTAQ DTAQ(CODELIVER1/ORDERQ) MAXLEN(200) TEXT(''Trade order queue'')')");
            System.out.println("Created ORDERQ *DTAQ");
        } catch (Exception e) {
            System.out.println("ORDERQ may already exist: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Setup: find system names and start journaling")
    void setupJournaling() {
        // Find the system table name for TRADE_ORDERS
        List<Map<String, Object>> rows =
                jdbc.queryForList(
                        "SELECT SYSTEM_TABLE_NAME, TABLE_NAME FROM QSYS2.SYSTABLES "
                                + "WHERE TABLE_SCHEMA = 'CODELIVER1' AND TABLE_NAME IN ('PORTFOLIO', 'TRADE_ORDERS')");
        for (Map<String, Object> row : rows) {
            System.out.println(
                    "Table: "
                            + row.get("TABLE_NAME")
                            + " -> System name: "
                            + row.get("SYSTEM_TABLE_NAME"));
        }

        // Start journaling on each table using system name
        for (Map<String, Object> row : rows) {
            String sysName = ((String) row.get("SYSTEM_TABLE_NAME")).trim();
            try {
                jdbc.execute(
                        String.format(
                                "CALL QSYS2.QCMDEXC('STRJRNPF FILE(CODELIVER1/%s) JRN(CODELIVER1/QSQJRN)')",
                                sysName));
                System.out.println("Journaling started for " + sysName);
            } catch (Exception e) {
                System.out.println(sysName + " journaling: " + e.getMessage());
            }
        }
    }
}
