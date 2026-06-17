package com.example.ibmi.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ibmi.model.Portfolio;
import com.example.ibmi.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@JdbcTest
@ActiveProfiles("test")
@Sql({"/schema-h2.sql", "/data-test.sql"})
@Import(PortfolioRepositoryUnitTest.TestConfig.class)
class PortfolioRepositoryUnitTest {

    @Configuration
    static class TestConfig {
        @Bean
        public String ibmiLibrary() {
            return "";
        }

        @Bean
        public PortfolioRepository portfolioRepository(JdbcTemplate jdbc, String ibmiLibrary) {
            return new PortfolioRepository(jdbc, ibmiLibrary);
        }
    }

    @Autowired private PortfolioRepository repo;

    @Test
    @DisplayName("TC-U-01: findById returns portfolio for existing PF001")
    void findById_knownId_returnsPortfolio() {
        // Arrange
        String portfolioId = "PF001";

        // Act
        Optional<Portfolio> result = repo.findById(portfolioId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getOwner()).isEqualTo("Richard Papen");
        assertThat(result.get().getCurrency()).isEqualTo("USD");
        assertThat(result.get().getStatus()).isEqualTo("A");
    }

    @Test
    @DisplayName("TC-U-02: findById returns empty Optional for non-existent ID")
    void findById_unknownId_returnsEmpty() {
        // Arrange
        String unknownId = "XXXXX";

        // Act
        Optional<Portfolio> result = repo.findById(unknownId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("TC-U-03: findAllActive returns only STATUS=A portfolios")
    void findAllActive_returnsOnlyActivePortfolios() {
        // Arrange — seed data has 2 active (PF001, PF002) and 1 inactive (PF003)

        // Act
        List<Portfolio> result = repo.findAllActive();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Portfolio::getStatus).containsOnly("A");
        assertThat(result).extracting(Portfolio::getPortfId).doesNotContain("PF003");
    }

    @Test
    @DisplayName("TC-U-04: updateValue persists new total value to DB")
    void updateValue_validPortfolio_updatesValue() {
        // Arrange
        String portfolioId = "PF001";
        BigDecimal newValue = new BigDecimal("200000.00");

        // Act
        boolean updated = repo.updateValue(portfolioId, newValue);

        // Assert
        assertThat(updated).isTrue();
        Optional<Portfolio> after = repo.findById(portfolioId);
        assertThat(after).isPresent();
        assertThat(after.get().getTotalValue()).isEqualByComparingTo(newValue);
    }

    @Test
    @DisplayName("TC-U-05: updateValue returns false for non-existent portfolio")
    void updateValue_unknownPortfolio_returnsFalse() {
        // Arrange
        String unknownId = "ZZZZ";
        BigDecimal anyValue = new BigDecimal("1000.00");

        // Act
        boolean updated = repo.updateValue(unknownId, anyValue);

        // Assert
        assertThat(updated).isFalse();
    }

    @Test
    @DisplayName("TC-U-06: findPendingOrders returns only PEND status orders")
    void findPendingOrders_returnsOnlyPendingOrders() {
        // Arrange — seed data has 3 PEND orders

        // Act
        var result = repo.findPendingOrders();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(o -> "PEND".equals(o.getStatus()));
    }

    @Test
    @DisplayName("TC-U-07: processOrder transitions PEND order to PROC")
    void processOrder_pendingOrder_updatesStatus() {
        // Arrange
        String orderId = "ORD-2026-001";

        // Act
        boolean processed = repo.processOrder(orderId);

        // Assert
        assertThat(processed).isTrue();
        var remaining = repo.findPendingOrders();
        assertThat(remaining).extracting(o -> o.getOrderId()).doesNotContain("ORD-2026-001");
    }
}
