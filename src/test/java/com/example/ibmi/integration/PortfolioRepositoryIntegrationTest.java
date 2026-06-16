package com.example.ibmi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import com.example.ibmi.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Requires: IBMI_HOST, IBMI_USER, IBMI_PASSWORD env vars set. Requires: CODELIVER1.PORTFOLIO table
 * exists (DOC 1 Layer 1 complete). Run with: mvn test -Pintegration
 */
@SpringBootTest
@TestPropertySource(
        properties = {"spring.datasource.driver-class-name=com.ibm.as400.access.AS400JDBCDriver"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PortfolioRepositoryIntegrationTest {

    @Autowired private PortfolioRepository repo;

    @Test
    @Order(1)
    @DisplayName("TC-I-01: findById reads PF001 from live CODELIVER1.PORTFOLIO on PUB400")
    void findById_liveDB2_returnsPF001() {
        // Arrange
        String portfolioId = "PF001";

        // Act
        Optional<Portfolio> result = repo.findById(portfolioId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getPortfId()).isEqualTo("PF001");
        assertThat(result.get().getCurrency()).isEqualTo("USD");
        assertThat(result.get().getStatus()).isEqualTo("A");
        assertThat(result.get().getTotalValue()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @Order(2)
    @DisplayName("TC-I-02: findAllActive reads ACTIVE_PORTFOLIOS view on live PUB400")
    void findAllActive_liveDB2_returnsOnlyActive() {
        // Arrange — PF003 is STATUS='I', should not appear

        // Act
        List<Portfolio> portfolios = repo.findAllActive();

        // Assert
        assertThat(portfolios).isNotEmpty();
        assertThat(portfolios).allMatch(p -> "A".equals(p.getStatus()));
        assertThat(portfolios).extracting(Portfolio::getPortfId).doesNotContain("PF003");
    }

    @Test
    @Order(3)
    @DisplayName("TC-I-03: findPendingOrders reads PEND orders from TRADE_ORDERS")
    void findPendingOrders_liveDB2_returnsPendingOrders() {
        // Arrange — seed data has 3 PEND orders

        // Act
        List<TradeOrder> orders = repo.findPendingOrders();

        // Assert
        assertThat(orders).allMatch(o -> "PEND".equals(o.getStatus()));
    }

    @Test
    @Order(4)
    @DisplayName("TC-I-04: updateValue persists to live DB2 for i with @Transactional")
    void updateValue_liveDB2_persistsValue() {
        // Arrange
        String portfolioId = "PF001";
        BigDecimal originalValue =
                repo.findById(portfolioId).map(Portfolio::getTotalValue).orElse(BigDecimal.ZERO);
        BigDecimal updatedValue = originalValue.add(new BigDecimal("5000.00"));

        // Act
        boolean updated = repo.updateValue(portfolioId, updatedValue);

        // Assert
        assertThat(updated).isTrue();
        Optional<Portfolio> after = repo.findById(portfolioId);
        assertThat(after).isPresent();
        assertThat(after.get().getTotalValue()).isEqualByComparingTo(updatedValue);

        // Cleanup — restore original value
        repo.updateValue(portfolioId, originalValue);
    }
}
