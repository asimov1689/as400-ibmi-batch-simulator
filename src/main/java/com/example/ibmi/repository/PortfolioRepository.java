package com.example.ibmi.repository;

import com.example.ibmi.model.Portfolio;
import com.example.ibmi.model.TradeOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioRepository {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRepository.class);

    private final JdbcTemplate jdbc;
    private final String library;

    public PortfolioRepository(JdbcTemplate jdbcTemplate, String ibmiLibrary) {
        this.jdbc = jdbcTemplate;
        this.library = (ibmiLibrary == null || ibmiLibrary.isBlank()) ? "" : ibmiLibrary;
    }

    private String table(String name) {
        return library.isEmpty() ? name : library + "." + name;
    }

    private final RowMapper<Portfolio> portfolioMapper =
            (rs, rowNum) -> {
                Portfolio p = new Portfolio();
                p.setPortfId(rs.getString("PORTF_ID").trim());
                p.setOwner(rs.getString("OWNER").trim());
                p.setCurrency(rs.getString("CURRENCY").trim());
                p.setTotalValue(rs.getBigDecimal("TOTAL_VALUE"));
                p.setStatus(rs.getString("STATUS").trim());
                p.setLastUpd(
                        rs.getDate("LAST_UPD") != null
                                ? rs.getDate("LAST_UPD").toLocalDate()
                                : null);
                return p;
            };

    private final RowMapper<TradeOrder> orderMapper =
            (rs, rowNum) -> {
                TradeOrder o = new TradeOrder();
                o.setOrderId(rs.getString("ORDER_ID").trim());
                o.setPortfId(rs.getString("PORTF_ID").trim());
                o.setIsin(rs.getString("ISIN").trim());
                o.setQuantity(rs.getBigDecimal("QUANTITY"));
                o.setPrice(rs.getBigDecimal("PRICE"));
                o.setStatus(rs.getString("STATUS").trim());
                return o;
            };

    public Optional<Portfolio> findById(String portfolioId) {
        String sql =
                "SELECT PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD "
                        + "FROM "
                        + table("PORTFOLIO")
                        + " WHERE PORTF_ID = ?";

        try {
            List<Portfolio> rows =
                    jdbc.query(sql, portfolioMapper, String.format("%-10s", portfolioId));
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            log.error("findById failed for {}: {}", portfolioId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Portfolio> findAllActive() {
        String sql =
                "SELECT PORTF_ID, OWNER, CURRENCY, TOTAL_VALUE, STATUS, LAST_UPD "
                        + "FROM "
                        + table("ACTIVE_PORTFOLIOS")
                        + " ORDER BY PORTF_ID";

        return jdbc.query(sql, portfolioMapper);
    }

    public boolean updateValue(String portfolioId, BigDecimal newValue) {
        String sql =
                "UPDATE "
                        + table("PORTFOLIO")
                        + " SET TOTAL_VALUE = ?, LAST_UPD = CURRENT_DATE WHERE PORTF_ID = ?";

        int rows = jdbc.update(sql, newValue, String.format("%-10s", portfolioId));
        return rows > 0;
    }

    public List<TradeOrder> findPendingOrders() {
        String sql =
                "SELECT ORDER_ID, PORTF_ID, ISIN, QUANTITY, PRICE, STATUS "
                        + "FROM "
                        + table("TRADE_ORDERS")
                        + " WHERE STATUS = 'PEND' ORDER BY ORDER_DT, ORDER_ID";

        return jdbc.query(sql, orderMapper);
    }

    public boolean processOrder(String orderId) {
        String sql =
                "UPDATE "
                        + table("TRADE_ORDERS")
                        + " SET STATUS = 'PROC', PROCESS_DT = CURRENT_DATE "
                        + "WHERE ORDER_ID = ? AND STATUS = 'PEND'";

        int rows = jdbc.update(sql, String.format("%-20s", orderId));
        return rows > 0;
    }
}
