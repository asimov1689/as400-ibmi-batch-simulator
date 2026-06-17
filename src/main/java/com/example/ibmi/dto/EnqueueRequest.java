package com.example.ibmi.dto;

import java.math.BigDecimal;

public class EnqueueRequest {

    private String orderId;
    private String portfId;
    private String isin;
    private BigDecimal quantity;
    private BigDecimal price;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String v) {
        this.orderId = v;
    }

    public String getPortfId() {
        return portfId;
    }

    public void setPortfId(String v) {
        this.portfId = v;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String v) {
        this.isin = v;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal v) {
        this.quantity = v;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal v) {
        this.price = v;
    }
}
