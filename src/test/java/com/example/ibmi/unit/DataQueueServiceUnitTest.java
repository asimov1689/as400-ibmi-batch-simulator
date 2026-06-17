package com.example.ibmi.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ibmi.model.TradeOrder;
import com.example.ibmi.service.ibmi.DataQueueService;
import com.ibm.as400.access.AS400;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataQueueServiceUnitTest {

    @Mock private AS400 mockAs400;

    private DataQueueService service;

    @BeforeEach
    void setUp() {
        service = new DataQueueService(mockAs400, "CODELIVER1", "ORDERQ");
    }

    @Test
    @DisplayName("TC-U-10: enqueueOrder handles AS400 connection failure gracefully")
    void enqueueOrder_connectionFails_returnsFalse() {
        // Arrange
        TradeOrder order = new TradeOrder();
        order.setOrderId("ORD-TEST-001");
        order.setPortfId("PF001");
        order.setIsin("TSH000000001");
        order.setQuantity(new BigDecimal("100"));
        order.setPrice(new BigDecimal("182.50"));
        order.setStatus("PEND");

        // Act
        boolean result = service.enqueueOrder(order);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("TC-U-11: dequeueOrder returns empty Optional on connection failure")
    void dequeueOrder_connectionFails_returnsEmpty() {
        // Arrange — mocked AS400 will cause DataQueue.read() to fail

        // Act
        var result = service.dequeueOrder(1);

        // Assert
        assertThat(result).isEmpty();
    }
}
