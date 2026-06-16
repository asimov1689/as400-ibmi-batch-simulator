package com.example.ibmi.model;

import java.math.BigDecimal;

public class TradeOrder {

    private String orderId;
    private String portfId;
    private String isin;
    private BigDecimal quantity;
    private BigDecimal price;
    private String status;

    public String getOrderId()                  { return orderId; }
    public void setOrderId(String v)            { this.orderId = v; }
    public String getPortfId()                  { return portfId; }
    public void setPortfId(String v)            { this.portfId = v; }
    public String getIsin()                     { return isin; }
    public void setIsin(String v)               { this.isin = v; }
    public BigDecimal getQuantity()             { return quantity; }
    public void setQuantity(BigDecimal v)       { this.quantity = v; }
    public BigDecimal getPrice()                { return price; }
    public void setPrice(BigDecimal v)          { this.price = v; }
    public String getStatus()                   { return status; }
    public void setStatus(String v)             { this.status = v; }
}
