package com.example.ibmi.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full HTTP -> Spring Boot -> JT400 -> IBM i round-trips. Requires a live PUB400
 * connection. Run with: mvn test -Pintegration
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("vscode")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BatchSettlementSystemTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @Order(1)
    @DisplayName("TC-S-01: /system/ping confirms IBM i connectivity end-to-end")
    void ping_liveIBMi_returns200() throws Exception {
        // Arrange — application started with live PUB400 connection

        // Act + Assert
        mockMvc.perform(get("/api/v1/system/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ibmiConcept").exists())
                .andExpect(
                        jsonPath("$.data")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "IBM i connection alive")));
    }

    @Test
    @Order(2)
    @DisplayName("TC-S-02: /portfolios returns active portfolios via ACTIVE_PORTFOLIOS view")
    void getAllPortfolios_liveIBMi_returnsActivePortfolios() throws Exception {
        // Arrange — ACTIVE_PORTFOLIOS view must exist (DOC 1 Layer 1)

        // Act + Assert
        MvcResult result =
                mockMvc.perform(get("/api/v1/portfolios"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.ibmiConcept").exists())
                        .andExpect(jsonPath("$.data").isArray())
                        .andReturn();

        // Assert
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("PF001");
        assertThat(body).contains("USD");
        assertThat(body).doesNotContain("PF003");
    }

    @Test
    @Order(3)
    @DisplayName("TC-S-03: /portfolios/PF001 returns single portfolio via keyed DB2 read")
    void getPortfolioById_PF001_returns200() throws Exception {
        // Arrange
        String portfolioId = "PF001";

        // Act + Assert
        mockMvc.perform(get("/api/v1/portfolios/" + portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portfId").value("PF001"))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(
                        jsonPath("$.ibmiConcept")
                                .value(org.hamcrest.Matchers.containsString("CHAIN")));
    }

    @Test
    @Order(4)
    @DisplayName("TC-S-04: enqueue + dequeue round-trip via IBM i *DTAQ")
    void enqueueDequeue_roundTrip_succeeds() throws Exception {
        // Arrange
        String orderJson =
                """
                {
                  "orderId":  "ORD-SYSTEST-001",
                  "portfId":  "PF001",
                  "isin":     "TSH000000001",
                  "quantity": 10,
                  "price":    182.50
                }
                """;

        // Act — enqueue
        mockMvc.perform(
                        post("/api/v1/orders/enqueue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.ibmiConcept")
                                .value(org.hamcrest.Matchers.containsString("SNDDTAQ")));

        // Act — dequeue (wait up to 10 seconds)
        MvcResult dequeueResult =
                mockMvc.perform(get("/api/v1/orders/dequeue?waitSeconds=10"))
                        .andExpect(status().isOk())
                        .andReturn();

        // Assert
        String body = dequeueResult.getResponse().getContentAsString();
        assertThat(body).contains("ORD-SYSTEST-001");
        assertThat(body).contains("RCVDTAQ");
    }

    @Test
    @Order(5)
    @DisplayName("TC-S-05: /job-info returns real IBM i QUSRJOBI job name/user/number")
    void getJobInfo_liveIBMi_returnsJobDetails() throws Exception {
        // Arrange — requires live PUB400 connection

        // Act + Assert
        mockMvc.perform(get("/api/v1/job-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobName").exists())
                .andExpect(jsonPath("$.data.jobUser").exists())
                .andExpect(jsonPath("$.data.jobNumber").exists())
                .andExpect(
                        jsonPath("$.ibmiConcept")
                                .value(org.hamcrest.Matchers.containsString("QUSRJOBI")));
    }
}
