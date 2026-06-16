package com.example.ibmi.unit;

import com.example.ibmi.controller.PortfolioController;
import com.example.ibmi.dto.PortfolioDto;
import com.example.ibmi.service.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @Test
    @DisplayName("TC-U-30: GET /portfolios returns 200 with active portfolios")
    void getAllPortfolios_returns200() throws Exception {
        // Arrange
        PortfolioDto dto = new PortfolioDto(
                "PF001", "Richard Papen", "USD",
                new BigDecimal("150000.00"), "A", LocalDate.now());
        org.mockito.Mockito.when(portfolioService.getAllActivePortfolios())
                .thenReturn(List.of(dto));

        // Act + Assert
        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].portfId").value("PF001"))
                .andExpect(jsonPath("$.data[0].currency").value("USD"))
                .andExpect(jsonPath("$.ibmiConcept").exists());
    }

    @Test
    @DisplayName("TC-U-31: GET /portfolios/{id} returns 404 when not found")
    void getPortfolioById_notFound_returns404() throws Exception {
        // Arrange
        org.mockito.Mockito.when(portfolioService.getPortfolioById("XXXXX"))
                .thenReturn(Optional.empty());

        // Act + Assert
        mockMvc.perform(get("/api/v1/portfolios/XXXXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-U-32: GET /portfolios/{id} returns 200 with portfolio data")
    void getPortfolioById_found_returns200() throws Exception {
        // Arrange
        PortfolioDto dto = new PortfolioDto(
                "PF001", "Richard Papen", "USD",
                new BigDecimal("150000.00"), "A", LocalDate.now());
        org.mockito.Mockito.when(portfolioService.getPortfolioById("PF001"))
                .thenReturn(Optional.of(dto));

        // Act + Assert
        mockMvc.perform(get("/api/v1/portfolios/PF001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portfId").value("PF001"))
                .andExpect(jsonPath("$.data.owner").value("Richard Papen"))
                .andExpect(jsonPath("$.ibmiConcept").exists());
    }

    @Test
    @DisplayName("TC-U-33: GET /system/ping returns 200 with connectivity status")
    void ping_returns200() throws Exception {
        // Arrange
        org.mockito.Mockito.when(portfolioService.pingIbmi()).thenReturn(true);

        // Act + Assert
        mockMvc.perform(get("/api/v1/system/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("IBM i connection alive — PUB400 responding"));
    }
}
