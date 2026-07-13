package com.quanlycuahang.erp.ai.service;

import com.quanlycuahang.erp.ai.service.AiQueryRow.DailySalesRow;
import com.quanlycuahang.erp.ai.service.AiQueryRow.DebtAgingRow;
import com.quanlycuahang.erp.ai.service.AiQueryRow.InventoryRow;
import com.quanlycuahang.erp.ai.service.AiQueryRow.RevenueRow;
import com.quanlycuahang.erp.ai.service.AiQueryRow.TopProductRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Doc du lieu qua 4 view whitelist (V30) BANG pool ket noi RIENG cua role {@code
 * fruithouse_ai_readonly} (xem AiReadOnlyDataSourceConfig) - day la NOI DUY NHAT trong toan bo
 * module AI thuc su cham vao du lieu that; {@link com.quanlycuahang.erp.ai.provider.AiProvider}
 * (AI) khong bao gio truy van truc tiep, chi CHON 1 trong 4 phuong thuc duoi day qua tool-calling
 * (xem AiAssistantService), roi Backend THAT SU chay va tra ket qua that lai cho AI tong hop.
 *
 * <p><b>SET GUC tenant + truy van PHAI cung 1 connection</b>: dung {@link ConnectionCallback} de
 * dam bao {@code set_config('app.current_tenant_id', ...)} va cau SELECT view chay tren CUNG 1
 * connection (JdbcTemplate mac dinh co the lay connection KHAC nhau cho 2 loi goi rieng le tu pool)
 * - neu khong, view co the loc theo tenant CU con sot lai tren connection tai su dung tu lan truoc,
 * hoac khong loc gi ca.
 */
@Service
public class AiQueryService {

  private final JdbcTemplate aiReadOnlyJdbcTemplate;

  public AiQueryService(@Qualifier("aiReadOnlyJdbcTemplate") JdbcTemplate aiReadOnlyJdbcTemplate) {
    this.aiReadOnlyJdbcTemplate = aiReadOnlyJdbcTemplate;
  }

  public List<RevenueRow> getRevenue(Long tenantId, int lookbackDays) {
    return runInTenantConnection(
        tenantId,
        "SELECT day, branch_id, revenue, order_count FROM v_ai_revenue"
            + " WHERE day >= now() - make_interval(days => ?) ORDER BY day",
        ps -> ps.setInt(1, lookbackDays),
        rs ->
            new RevenueRow(
                rs.getDate("day").toLocalDate(),
                (Long) rs.getObject("branch_id"),
                rs.getBigDecimal("revenue"),
                rs.getLong("order_count")));
  }

  public List<TopProductRow> getTopProducts(Long tenantId, int limit) {
    return runInTenantConnection(
        tenantId,
        "SELECT product_id, product_name, sku, branch_id, quantity_sold, revenue FROM"
            + " v_ai_top_products ORDER BY quantity_sold DESC LIMIT ?",
        ps -> ps.setInt(1, limit),
        rs ->
            new TopProductRow(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getString("sku"),
                (Long) rs.getObject("branch_id"),
                rs.getBigDecimal("quantity_sold"),
                rs.getBigDecimal("revenue")));
  }

  public List<InventoryRow> getLowStockInventory(Long tenantId, Long branchId) {
    String sql =
        "SELECT product_id, product_name, sku, branch_id, stock, min_stock FROM"
            + " v_ai_inventory_summary WHERE stock <= min_stock"
            + (branchId != null ? " AND branch_id = ?" : "")
            + " ORDER BY (stock - min_stock) ASC";
    return runInTenantConnection(
        tenantId,
        sql,
        ps -> {
          if (branchId != null) {
            ps.setLong(1, branchId);
          }
        },
        rs ->
            new InventoryRow(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getString("sku"),
                (Long) rs.getObject("branch_id"),
                rs.getBigDecimal("stock"),
                rs.getBigDecimal("min_stock")));
  }

  public List<DebtAgingRow> getDebtAging(Long tenantId, String direction) {
    return runInTenantConnection(
        tenantId,
        "SELECT direction, aging_bucket, total_amount, debt_count FROM v_ai_debt_aging WHERE"
            + " direction = ? ORDER BY aging_bucket",
        ps -> ps.setString(1, direction),
        rs ->
            new DebtAgingRow(
                rs.getString("direction"),
                rs.getString("aging_bucket"),
                rs.getBigDecimal("total_amount"),
                rs.getLong("debt_count")));
  }

  /**
   * Doanh so ban THEO TUNG NGAY cua 1 chi nhanh (Prompt #12, V32) - dung cho AiForecastService gui
   * sang Python ML service de tinh du bao nhap hang (IsolationForest loc ngay ban bat thuong truoc
   * khi tinh toc do trung binh).
   */
  public List<DailySalesRow> getDailySales(Long tenantId, Long branchId, int lookbackDays) {
    return runInTenantConnection(
        tenantId,
        "SELECT product_id, branch_id, sale_day, quantity_sold FROM v_ai_daily_sales"
            + " WHERE branch_id = ? AND sale_day >= now() - make_interval(days => ?)"
            + " ORDER BY product_id, sale_day",
        ps -> {
          ps.setLong(1, branchId);
          ps.setInt(2, lookbackDays);
        },
        rs ->
            new DailySalesRow(
                rs.getLong("product_id"),
                (Long) rs.getObject("branch_id"),
                rs.getDate("sale_day").toLocalDate(),
                rs.getBigDecimal("quantity_sold")));
  }

  @FunctionalInterface
  private interface ParamBinder {
    void bind(PreparedStatement ps) throws java.sql.SQLException;
  }

  @FunctionalInterface
  private interface RowMapper<T> {
    T map(ResultSet rs) throws java.sql.SQLException;
  }

  private <T> List<T> runInTenantConnection(
      Long tenantId, String sql, ParamBinder binder, RowMapper<T> mapper) {
    return aiReadOnlyJdbcTemplate.execute(
        (ConnectionCallback<List<T>>)
            (Connection con) -> {
              try (PreparedStatement setTenant =
                  con.prepareStatement("SELECT set_config('app.current_tenant_id', ?, false)")) {
                setTenant.setString(1, String.valueOf(tenantId));
                setTenant.execute();
              }
              List<T> results = new ArrayList<>();
              try (PreparedStatement statement = con.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet rs = statement.executeQuery()) {
                  while (rs.next()) {
                    results.add(mapper.map(rs));
                  }
                }
              }
              return results;
            });
  }
}
