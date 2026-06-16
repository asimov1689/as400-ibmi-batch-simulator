package com.example.ibmi.service;

import com.example.ibmi.dto.EnqueueRequest;
import com.example.ibmi.dto.PortfolioDto;
import com.example.ibmi.dto.TradeOrderDto;
import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import com.example.ibmi.repository.PortfolioRepository;
import com.example.ibmi.service.ibmi.CommandExecutorService;
import com.example.ibmi.service.ibmi.DataQueueService;
import com.example.ibmi.service.ibmi.ProgramCallService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepo;
    private final DataQueueService dataQueueService;
    private final ProgramCallService programCallService;
    private final CommandExecutorService commandExecutorService;

    public PortfolioService(
            PortfolioRepository portfolioRepo,
            DataQueueService dataQueueService,
            ProgramCallService programCallService,
            CommandExecutorService commandExecutorService) {
        this.portfolioRepo = portfolioRepo;
        this.dataQueueService = dataQueueService;
        this.programCallService = programCallService;
        this.commandExecutorService = commandExecutorService;
    }

    public List<PortfolioDto> getAllActivePortfolios() {
        return portfolioRepo.findAllActive().stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<PortfolioDto> getPortfolioById(String id) {
        return portfolioRepo.findById(id).map(this::toDto);
    }

    public boolean updatePortfolioValue(String id, BigDecimal newValue) {
        return portfolioRepo.updateValue(id, newValue);
    }

    public List<TradeOrderDto> getPendingOrders() {
        return portfolioRepo.findPendingOrders().stream()
                .map(this::toOrderDto)
                .toList();
    }

    public boolean enqueueOrder(EnqueueRequest request) {
        TradeOrder order = new TradeOrder();
        order.setOrderId(request.getOrderId());
        order.setPortfId(request.getPortfId());
        order.setIsin(request.getIsin());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus("PEND");
        return dataQueueService.enqueueOrder(order);
    }

    public Optional<TradeOrderDto> dequeueOrder(int waitSeconds) {
        return dataQueueService.dequeueOrder(waitSeconds).map(this::toOrderDto);
    }

    public Map<String, String> checkEligibility(String portfolioId, String isin) {
        return programCallService.checkEligibility(portfolioId, isin);
    }

    public Map<String, String> getJobInfo() {
        return programCallService.getJobInfo();
    }

    public boolean pingIbmi() {
        return commandExecutorService.execute("DSPLIBL OUTPUT(*PRINT)");
    }

    private PortfolioDto toDto(Portfolio p) {
        return new PortfolioDto(
                p.getPortfId(), p.getOwner(), p.getCurrency(),
                p.getTotalValue(), p.getStatus(), p.getLastUpd());
    }

    private TradeOrderDto toOrderDto(TradeOrder o) {
        return new TradeOrderDto(
                o.getOrderId(), o.getPortfId(), o.getIsin(),
                o.getQuantity(), o.getPrice(), o.getStatus());
    }
}
