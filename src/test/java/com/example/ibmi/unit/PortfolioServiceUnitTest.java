package com.example.ibmi.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ibmi.dto.PortfolioDto;
import com.example.ibmi.model.Portfolio;
import com.example.ibmi.repository.PortfolioRepository;
import com.example.ibmi.service.PortfolioService;
import com.example.ibmi.service.ibmi.CommandExecutorService;
import com.example.ibmi.service.ibmi.DataQueueService;
import com.example.ibmi.service.ibmi.ProgramCallService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceUnitTest {

    @Mock private PortfolioRepository portfolioRepo;
    @Mock private DataQueueService dataQueueService;
    @Mock private ProgramCallService programCallService;
    @Mock private CommandExecutorService commandExecutorService;

    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service =
                new PortfolioService(
                        portfolioRepo, dataQueueService,
                        programCallService, commandExecutorService);
    }

    @Test
    @DisplayName("TC-U-20: getAllActivePortfolios maps entities to DTOs")
    void getAllActivePortfolios_returnsDtoList() {
        // Arrange
        Portfolio p = new Portfolio();
        p.setPortfId("PF001");
        p.setOwner("Richard Papen");
        p.setCurrency("USD");
        p.setTotalValue(new BigDecimal("150000.00"));
        p.setStatus("A");
        p.setLastUpd(LocalDate.now());
        when(portfolioRepo.findAllActive()).thenReturn(List.of(p));

        // Act
        List<PortfolioDto> result = service.getAllActivePortfolios();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPortfId()).isEqualTo("PF001");
        assertThat(result.get(0).getOwner()).isEqualTo("Richard Papen");
        assertThat(result.get(0).getTotalValue()).isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    @DisplayName("TC-U-21: getPortfolioById returns empty when not found")
    void getPortfolioById_notFound_returnsEmpty() {
        // Arrange
        when(portfolioRepo.findById("XXXXX")).thenReturn(Optional.empty());

        // Act
        Optional<PortfolioDto> result = service.getPortfolioById("XXXXX");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("TC-U-22: pingIbmi delegates to CommandExecutorService")
    void pingIbmi_delegatesToCommandExecutor() {
        // Arrange
        when(commandExecutorService.execute("DSPLIBL OUTPUT(*PRINT)")).thenReturn(true);

        // Act
        boolean result = service.pingIbmi();

        // Assert
        assertThat(result).isTrue();
    }
}
