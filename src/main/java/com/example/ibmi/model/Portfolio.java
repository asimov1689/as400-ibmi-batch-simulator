package com.example.ibmi.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Portfolio {

    private String portfId;
    private String owner;
    private String currency;
    private BigDecimal totalValue;
    private String status;
    private LocalDate lastUpd;

    public String getPortfId()                  { return portfId; }
    public void setPortfId(String v)            { this.portfId = v; }
    public String getOwner()                    { return owner; }
    public void setOwner(String v)              { this.owner = v; }
    public String getCurrency()                 { return currency; }
    public void setCurrency(String v)           { this.currency = v; }
    public BigDecimal getTotalValue()           { return totalValue; }
    public void setTotalValue(BigDecimal v)     { this.totalValue = v; }
    public String getStatus()                   { return status; }
    public void setStatus(String v)             { this.status = v; }
    public LocalDate getLastUpd()               { return lastUpd; }
    public void setLastUpd(LocalDate v)         { this.lastUpd = v; }
}
