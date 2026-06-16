package com.example.ibmi.service.ibmi;

import com.example.ibmi.model.TradeOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataQueueService {

    private static final Logger log = LoggerFactory.getLogger(DataQueueService.class);
    private static final int ENTRY_LENGTH = 200;

    private final AS400 as400;
    private final String library;
    private final String queueName;
    private final ObjectMapper mapper;

    public DataQueueService(
            AS400 as400,
            String ibmiLibrary,
            @Value("${ibmi.order-queue:ORDERQ}") String queueName) {
        this.as400 = as400;
        this.library = ibmiLibrary;
        this.queueName = queueName;
        this.mapper = new ObjectMapper();
    }

    public boolean enqueueOrder(TradeOrder order) {
        try {
            DataQueue queue =
                    new DataQueue(
                            as400, String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));

            String json = mapper.writeValueAsString(order);
            byte[] payload = new byte[ENTRY_LENGTH];
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(jsonBytes, 0, payload, 0, Math.min(jsonBytes.length, ENTRY_LENGTH));

            queue.write(payload);
            log.debug("Enqueued order {} to *DTAQ {}", order.getOrderId(), queueName);
            return true;
        } catch (Exception e) {
            log.error("Failed to enqueue order to *DTAQ {}: {}", queueName, e.getMessage());
            return false;
        }
    }

    public Optional<TradeOrder> dequeueOrder(int waitSeconds) {
        try {
            DataQueue queue =
                    new DataQueue(
                            as400, String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));

            DataQueueEntry entry = queue.read(waitSeconds);
            if (entry == null) {
                return Optional.empty();
            }

            byte[] raw = entry.getData();
            int len = raw.length;
            while (len > 0 && raw[len - 1] == 0) len--;
            String json = new String(Arrays.copyOf(raw, len), StandardCharsets.UTF_8).trim();

            TradeOrder order = mapper.readValue(json, TradeOrder.class);
            log.debug("Dequeued order {} from *DTAQ {}", order.getOrderId(), queueName);
            return Optional.of(order);
        } catch (Exception e) {
            log.error("Failed to dequeue from *DTAQ {}: {}", queueName, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean createQueueIfAbsent() {
        try {
            DataQueue queue =
                    new DataQueue(
                            as400, String.format("/QSYS.LIB/%s.LIB/%s.DTAQ", library, queueName));
            queue.create(ENTRY_LENGTH);
            log.info("Created *DTAQ {}/{}", library, queueName);
            return true;
        } catch (Exception e) {
            log.debug("*DTAQ {}/{} may already exist: {}", library, queueName, e.getMessage());
            return false;
        }
    }
}
