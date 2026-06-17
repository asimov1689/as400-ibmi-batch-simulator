package com.example.ibmi.integration;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the IBM i native test programs (RPGLE, CL, SQLRPGLE) via QCMDEXC.
 * A passing CALL means the program completed without sending an *ESCAPE message.
 * Run with: mvn test -Pintegration
 */
@SpringBootTest
@ActiveProfiles("vscode")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IBMiNativeTestSuiteTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    @Order(1)
    @DisplayName("TC-N-01: UTEST01 — RPGLE unit tests for PORTFSVC (6 AAA test cases)")
    void utest01_rpgleUnitTests_pass() {
        assertThatCode(() ->
                jdbc.execute("CALL QSYS2.QCMDEXC('CALL PGM(CODELIVER1/UTEST01)')"))
                .as("UTEST01 should complete without *ESCAPE")
                .doesNotThrowAnyException();
    }

    @Test
    @Order(2)
    @DisplayName("TC-N-02: ITEST01 — CL integration tests for PORTFINQ + PORTFCBL (3 AAA test cases)")
    void itest01_clIntegrationTests_pass() {
        assertThatCode(() ->
                jdbc.execute("CALL QSYS2.QCMDEXC('CALL PGM(CODELIVER1/ITEST01)')"))
                .as("ITEST01 should complete without *ESCAPE")
                .doesNotThrowAnyException();
    }

    @Test
    @Order(3)
    @DisplayName("TC-N-03: STEST01 — RPGLE system test for ORDRBATCH batch settlement (4 AAA test cases)")
    void stest01_rpgleSystemTest_pass() {
        assertThatCode(() ->
                jdbc.execute("CALL QSYS2.QCMDEXC('CALL PGM(CODELIVER1/STEST01)')"))
                .as("STEST01 should complete without *ESCAPE")
                .doesNotThrowAnyException();
    }
}
