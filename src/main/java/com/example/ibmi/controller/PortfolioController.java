package com.example.ibmi.controller;

import com.example.ibmi.dto.ApiResponse;
import com.example.ibmi.dto.EnqueueRequest;
import com.example.ibmi.dto.PortfolioDto;
import com.example.ibmi.dto.TradeOrderDto;
import com.example.ibmi.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/portfolios")
    public ResponseEntity<ApiResponse<List<PortfolioDto>>> getAllPortfolios() {
        List<PortfolioDto> portfolios = portfolioService.getAllActivePortfolios();
        return ResponseEntity.ok(new ApiResponse<>(
                portfolios,
                "DB2 for i SELECT via ACTIVE_PORTFOLIOS SQL View — " +
                "equivalent of READ opcode on a DDS Logical File with SELECT(STATUS='A')"
        ));
    }

    @GetMapping("/portfolios/{id}")
    public ResponseEntity<ApiResponse<PortfolioDto>> getPortfolioById(
            @PathVariable String id) {
        Optional<PortfolioDto> portfolio = portfolioService.getPortfolioById(id);
        if (portfolio.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ApiResponse<>(
                portfolio.get(),
                "DB2 for i JDBC keyed read — CHAIN opcode equivalent. " +
                "JdbcTemplate.query() -> AS400JDBCDriver -> DB2 for i SELECT WHERE PORTF_ID=?"
        ));
    }

    @PutMapping("/portfolios/{id}/value")
    public ResponseEntity<ApiResponse<String>> updatePortfolioValue(
            @PathVariable String id,
            @RequestParam BigDecimal newValue) {
        boolean updated = portfolioService.updatePortfolioValue(id, newValue);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ApiResponse<>(
                "Portfolio " + id + " updated to " + newValue,
                "@Transactional in Java = STRCMTCTL + COMMIT on IBM i. " +
                "DB2 for i automatically journals the change."
        ));
    }

    @GetMapping("/orders/pending")
    public ResponseEntity<ApiResponse<List<TradeOrderDto>>> getPendingOrders() {
        List<TradeOrderDto> orders = portfolioService.getPendingOrders();
        return ResponseEntity.ok(new ApiResponse<>(
                orders,
                "DB2 for i SELECT WHERE STATUS='PEND' — equivalent of " +
                "DECLARE CURSOR FOR SELECT ... WHERE STATUS='PEND' in ORDRBATCH"
        ));
    }

    @PostMapping("/orders/enqueue")
    public ResponseEntity<ApiResponse<String>> enqueueOrder(
            @RequestBody EnqueueRequest request) {
        boolean queued = portfolioService.enqueueOrder(request);
        if (!queued) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>("Failed to enqueue order", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(
                "Order " + request.getOrderId() + " enqueued",
                "*DTAQ write via JT400 DataQueue.write() — SNDDTAQ equivalent."
        ));
    }

    @GetMapping("/orders/dequeue")
    public ResponseEntity<ApiResponse<TradeOrderDto>> dequeueOrder(
            @RequestParam(defaultValue = "5") int waitSeconds) {
        Optional<TradeOrderDto> order = portfolioService.dequeueOrder(waitSeconds);
        if (order.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(
                    null,
                    "*DTAQ read timed out — no entries within " + waitSeconds + "s."
            ));
        }
        return ResponseEntity.ok(new ApiResponse<>(
                order.get(),
                "*DTAQ read via JT400 DataQueue.read(" + waitSeconds + ") — RCVDTAQ equivalent."
        ));
    }

    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkEligibility(
            @RequestParam String portfolioId,
            @RequestParam String isin) {
        Map<String, String> result = portfolioService.checkEligibility(portfolioId, isin);
        return ResponseEntity.ok(new ApiResponse<>(
                result,
                "JT400 ProgramCall — calling *PGM with AS400Text EBCDIC conversion."
        ));
    }

    @GetMapping("/job-info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getJobInfo() {
        Map<String, String> info = portfolioService.getJobInfo();
        return ResponseEntity.ok(new ApiResponse<>(
                info,
                "QUSRJOBI system API via JT400 Job class — job name/user/number triple."
        ));
    }

    @GetMapping("/system/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        boolean ok = portfolioService.pingIbmi();
        return ResponseEntity.ok(new ApiResponse<>(
                ok ? "IBM i connection alive — PUB400 responding" : "Connection check failed",
                "CommandCall.run('DSPLIBL') — QCMDEXC from Java."
        ));
    }
}
