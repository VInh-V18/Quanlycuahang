package com.quanlycuahang.erp.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.common.metrics.BusinessMetrics;
import com.quanlycuahang.erp.reconciliation.dto.ReconciliationFindingResponse;
import com.quanlycuahang.erp.reconciliation.dto.ReconciliationRunResponse;
import com.quanlycuahang.erp.reconciliation.entity.ReconciliationFinding;
import com.quanlycuahang.erp.reconciliation.entity.ReconciliationRun;
import com.quanlycuahang.erp.reconciliation.repository.ReconciliationFindingRepository;
import com.quanlycuahang.erp.reconciliation.repository.ReconciliationRunRepository;
import com.quanlycuahang.erp.system.entity.Tenant;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Doi soat toan ven du lieu (Prompt #6, P1; phep (h) them qua audit production readiness
 * 2026-07-17) — 8 phep doi soat (a-h), MOI phep la 1 cau SQL aggregate DUY NHAT loc theo tenant_id
 * thu cong (JdbcTemplate di thang qua DataSource, KHONG qua Hibernate Session/@Filter — giong 12
 * file native-query o Prompt #4, phai tu them dieu kien tenant_id), KHONG nap tung ban ghi vao Java
 * roi so sanh (yeu cau hieu nang cua roadmap: tenant 20k SKU van phai xong duoi 30 giay — 1
 * round-trip DB/phep, khong phai N vong lap).
 *
 * <p>2 diem dieu chinh cong thuc so voi mo ta roadmap (da kiem chung lai voi code that, ghi ro o
 * day thay vi lam theo mo ta khong chinh xac):
 *
 * <ul>
 *   <li><b>(b) Cong no</b>: roadmap viet "debts.remaining vs (debts.amount - SUM(debt_payments))" —
 *       nhung schema nay debts.amount CHINH LA so du con lai (khong co cot "remaining" rieng), va
 *       debts.original_amount la so goc. Quan trong hon: ReturnService giam truc tiep debts.amount
 *       khi tra hang co ghi no (KHONG ghi 1 dong debt_payments rieng — xem Javadoc DebtRepository,
 *       ngoai pham vi FH-12), nen dang thuc "amount == original_amount - SUM(debt_payments)" se BAO
 *       SAI (false positive) cho MOI khoan no tung bi giam boi tra hang, du du lieu hoan toan dung.
 *       Doi lai thanh kiem tra BAT BIEN luon phai dung bat ke nguon giam nao (thanh toan qua
 *       debt_payments HAY tra hang truc tiep): amount phai nam trong [0, original_amount], VA so
 *       tien da ghi qua debt_payments khong duoc VUOT QUA phan da giam (original_amount - amount) —
 *       neu SUM(debt_payments) > original_amount - amount nghia la co giao dich thanh toan "ma"
 *       khong duoc phan anh vao amount, chac chan la loi that bat ke tra hang co xay ra hay khong.
 *   <li><b>(c) Don hang</b>: roadmap viet "orders.total vs SUM(order_items.line_total) + phi - CK"
 *       — nhung CK dong VA CK don deu da duoc phan bo VAO TUNG line_total roi (xem
 *       OrderPricingService/OrderValidationService), khong con 1 cot CK rieng o muc don de tru lai
 *       lan 2. Cong thuc dung (khop chinh xac OrderService.createOrder): total_amount =
 *       SUM(order_items.line_total) + rounding_adjustment + shipping_fee.
 * </ul>
 */
@Service
public class ReconciliationService {

  private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

  private final JdbcTemplate jdbcTemplate;
  private final ReconciliationRunRepository runRepository;
  private final ReconciliationFindingRepository findingRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final EntityManager entityManager;
  private final BusinessMetrics businessMetrics;

  public ReconciliationService(
      JdbcTemplate jdbcTemplate,
      ReconciliationRunRepository runRepository,
      ReconciliationFindingRepository findingRepository,
      UserRepository userRepository,
      ObjectMapper objectMapper,
      EntityManager entityManager,
      BusinessMetrics businessMetrics) {
    this.jdbcTemplate = jdbcTemplate;
    this.runRepository = runRepository;
    this.findingRepository = findingRepository;
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
    this.entityManager = entityManager;
    this.businessMetrics = businessMetrics;
  }

  @Transactional
  public ReconciliationRunResponse runForTenant(
      Long tenantId, String triggerType, Long triggeredByUserId) {
    long startNanos = System.nanoTime();
    Tenant tenantRef = entityManager.getReference(Tenant.class, tenantId);
    ReconciliationRun run = new ReconciliationRun();
    run.setTenant(tenantRef);
    run.setTriggerType(triggerType);
    run.setStartedAt(OffsetDateTime.now());
    run.setStatus(ReconciliationRun.STATUS_RUNNING);
    if (triggeredByUserId != null) {
      userRepository.findById(triggeredByUserId).ifPresent(run::setTriggeredBy);
    }
    run = runRepository.save(run);

    List<ReconciliationFinding> findings = new ArrayList<>();
    try {
      findings.addAll(checkInventoryMismatch(tenantId, run));
      findings.addAll(checkDebtMismatch(tenantId, run));
      findings.addAll(checkOrderTotalMismatch(tenantId, run));
      findings.addAll(checkInvoiceMissingOrDuplicate(tenantId, run));
      findings.addAll(checkReturnOverQuantity(tenantId, run));
      findings.addAll(checkOrphanedReferences(tenantId, run));
      findings.addAll(checkShiftDiscrepancyMismatch(tenantId, run));
      findings.addAll(checkCrossTenantReferences(tenantId, run));

      findingRepository.saveAll(findings);
      run.setFindingsCount(findings.size());
      run.setStatus(ReconciliationRun.STATUS_COMPLETED);
    } catch (RuntimeException ex) {
      // Khong de loi 1 phep lam mat toan bo ket qua cac phep da chay xong - ghi nhan that bai
      // ro rang thay vi de request nem 500 chung chung, van tra ve nhung finding da tim duoc
      // truoc do (van huu ich hon la khong co gi).
      log.error("Doi soat toan ven du lieu that bai giua chung cho tenant {}", tenantId, ex);
      run.setStatus(ReconciliationRun.STATUS_FAILED);
      run.setFindingsCount(findings.size());
    }
    run.setFinishedAt(OffsetDateTime.now());
    run.setDurationMs((System.nanoTime() - startNanos) / 1_000_000);
    run = runRepository.save(run);

    // Cap nhat gauge Prometheus ngay sau khi chay xong (Prompt #8, P2 quan sat) - khong doi den
    // luc FE goi /open-count moi thay so moi, huu ich nhat cho job dem (khong ai "hoi" gauge o
    // giua dem, nhung so lieu van phai dung khi ai do xem dashboard Grafana sang hom sau).
    businessMetrics.setReconciliationFindingsOpen(
        tenantId, findingRepository.countByStatus("open"));

    return toResponse(run, findings);
  }

  private ReconciliationFinding newFinding(
      ReconciliationRun run,
      Long tenantId,
      String checkType,
      String severity,
      String entityType,
      Long entityId,
      Map<String, Object> details) {
    ReconciliationFinding finding = new ReconciliationFinding();
    finding.setTenant(entityManager.getReference(Tenant.class, tenantId));
    finding.setRun(run);
    finding.setCheckType(checkType);
    finding.setSeverity(severity);
    finding.setEntityType(entityType);
    finding.setEntityId(entityId);
    try {
      finding.setDetails(objectMapper.writeValueAsString(details));
    } catch (Exception ex) {
      finding.setDetails("{}");
    }
    finding.setStatus(ReconciliationFinding.STATUS_OPEN);
    return finding;
  }

  /**
   * (a) Ton kho: inventory.stock phai bang tong inventory_transactions.quantity cua dung
   * (product_id, branch_id) — ton dau luon la 0 (initializeIfAbsent luon INSERT stock=0).
   */
  private List<ReconciliationFinding> checkInventoryMismatch(Long tenantId, ReconciliationRun run) {
    String sql =
        "SELECT i.id, i.product_id, i.branch_id, i.stock, COALESCE(SUM(t.quantity), 0) AS computed "
            + "FROM inventory i "
            + "LEFT JOIN inventory_transactions t ON t.product_id = i.product_id "
            + "  AND t.branch_id = i.branch_id AND t.tenant_id = i.tenant_id AND t.deleted_at IS NULL "
            + "WHERE i.tenant_id = ? AND i.deleted_at IS NULL "
            + "GROUP BY i.id, i.product_id, i.branch_id, i.stock "
            + "HAVING i.stock <> COALESCE(SUM(t.quantity), 0)";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Map<String, Object> details = new LinkedHashMap<>();
          details.put("productId", rs.getLong("product_id"));
          details.put("branchId", rs.getLong("branch_id"));
          details.put("inventoryStock", rs.getBigDecimal("stock"));
          details.put("computedFromTransactions", rs.getBigDecimal("computed"));
          return newFinding(
              run, tenantId, "INVENTORY_MISMATCH", "high", "inventory", rs.getLong("id"), details);
        },
        tenantId);
  }

  /**
   * (b) Cong no: amount phai trong [0, original_amount], va SUM(debt_payments) khong duoc vuot qua
   * phan da giam (original_amount - amount) — xem giai trinh dieu chinh cong thuc o Javadoc dau
   * class.
   */
  private List<ReconciliationFinding> checkDebtMismatch(Long tenantId, ReconciliationRun run) {
    String sql =
        "SELECT d.id, d.amount, d.original_amount, COALESCE(SUM(p.amount), 0) AS paid "
            + "FROM debts d "
            + "LEFT JOIN debt_payments p ON p.debt_id = d.id AND p.tenant_id = d.tenant_id AND p.deleted_at IS NULL "
            + "WHERE d.tenant_id = ? AND d.deleted_at IS NULL "
            + "GROUP BY d.id, d.amount, d.original_amount "
            + "HAVING d.amount < 0 OR d.amount > d.original_amount "
            + "   OR COALESCE(SUM(p.amount), 0) > (d.original_amount - d.amount)";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Map<String, Object> details = new LinkedHashMap<>();
          details.put("amount", rs.getBigDecimal("amount"));
          details.put("originalAmount", rs.getBigDecimal("original_amount"));
          details.put("sumDebtPayments", rs.getBigDecimal("paid"));
          return newFinding(
              run, tenantId, "DEBT_MISMATCH", "high", "debt", rs.getLong("id"), details);
        },
        tenantId);
  }

  /**
   * (c) Don hang: total_amount = SUM(order_items.line_total) + rounding_adjustment + shipping_fee
   * (cong thuc dung cua OrderService.createOrder - xem Javadoc dau class).
   */
  private List<ReconciliationFinding> checkOrderTotalMismatch(
      Long tenantId, ReconciliationRun run) {
    String sql =
        "SELECT o.id, o.total_amount, o.rounding_adjustment, o.shipping_fee, "
            + "  COALESCE(SUM(oi.line_total), 0) AS items_total "
            + "FROM orders o "
            + "LEFT JOIN order_items oi ON oi.order_id = o.id AND oi.tenant_id = o.tenant_id AND oi.deleted_at IS NULL "
            + "WHERE o.tenant_id = ? AND o.deleted_at IS NULL "
            + "GROUP BY o.id, o.total_amount, o.rounding_adjustment, o.shipping_fee "
            + "HAVING o.total_amount <> COALESCE(SUM(oi.line_total), 0) + o.rounding_adjustment + o.shipping_fee";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          BigDecimal expected =
              rs.getBigDecimal("items_total")
                  .add(rs.getBigDecimal("rounding_adjustment"))
                  .add(rs.getBigDecimal("shipping_fee"));
          Map<String, Object> details = new LinkedHashMap<>();
          details.put("totalAmount", rs.getBigDecimal("total_amount"));
          details.put("expectedTotal", expected);
          return newFinding(
              run,
              tenantId,
              "ORDER_TOTAL_MISMATCH",
              "critical",
              "order",
              rs.getLong("id"),
              details);
        },
        tenantId);
  }

  /** (d) Hoa don: moi order != 'draft' phai co DUNG 1 invoice (thieu HOAC du >1). */
  private List<ReconciliationFinding> checkInvoiceMissingOrDuplicate(
      Long tenantId, ReconciliationRun run) {
    List<ReconciliationFinding> findings = new ArrayList<>();

    String missingSql =
        "SELECT o.id FROM orders o "
            + "WHERE o.tenant_id = ? AND o.deleted_at IS NULL AND o.status <> 'draft' "
            + "AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.order_id = o.id AND i.tenant_id = o.tenant_id AND i.deleted_at IS NULL)";
    findings.addAll(
        jdbcTemplate.query(
            missingSql,
            (rs, rowNum) ->
                newFinding(
                    run,
                    tenantId,
                    "INVOICE_MISSING",
                    "critical",
                    "order",
                    rs.getLong("id"),
                    Map.of("reason", "Không có hóa đơn tương ứng")),
            tenantId));

    String duplicateSql =
        "SELECT i.order_id, COUNT(*) AS cnt FROM invoices i "
            + "WHERE i.tenant_id = ? AND i.deleted_at IS NULL "
            + "GROUP BY i.order_id HAVING COUNT(*) > 1";
    findings.addAll(
        jdbcTemplate.query(
            duplicateSql,
            (rs, rowNum) ->
                newFinding(
                    run,
                    tenantId,
                    "INVOICE_DUPLICATE",
                    "critical",
                    "order",
                    rs.getLong("order_id"),
                    Map.of("invoiceCount", rs.getInt("cnt"))),
            tenantId));

    return findings;
  }

  /**
   * (e) Tra hang: tong so luong da tra (return_items) khong duoc vuot qua so luong da mua
   * (order_items.quantity), VA cot cache order_items.returned_quantity phai khop dung tong do.
   */
  private List<ReconciliationFinding> checkReturnOverQuantity(
      Long tenantId, ReconciliationRun run) {
    String sql =
        "SELECT oi.id, oi.quantity, oi.returned_quantity, COALESCE(SUM(ri.quantity), 0) AS returned_total "
            + "FROM order_items oi "
            + "LEFT JOIN return_items ri ON ri.order_item_id = oi.id AND ri.tenant_id = oi.tenant_id AND ri.deleted_at IS NULL "
            + "WHERE oi.tenant_id = ? AND oi.deleted_at IS NULL "
            + "GROUP BY oi.id, oi.quantity, oi.returned_quantity "
            + "HAVING COALESCE(SUM(ri.quantity), 0) > oi.quantity "
            + "   OR oi.returned_quantity <> COALESCE(SUM(ri.quantity), 0)";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Map<String, Object> details = new LinkedHashMap<>();
          details.put("quantity", rs.getBigDecimal("quantity"));
          details.put("returnedQuantityColumn", rs.getBigDecimal("returned_quantity"));
          details.put("sumReturnItems", rs.getBigDecimal("returned_total"));
          return newFinding(
              run,
              tenantId,
              "RETURN_OVER_QUANTITY",
              "high",
              "order_item",
              rs.getLong("id"),
              details);
        },
        tenantId);
  }

  /**
   * (f) Tham chieu da hinh mo coi: debts.reference_id/inventory_transactions.reference_id tro toi
   * ban ghi khong ton tai hoac khac tenant (khong co FK that vi 2 cot nay dung chung cho nhieu loai
   * bang dich khac nhau tuy reference_type).
   */
  private List<ReconciliationFinding> checkOrphanedReferences(
      Long tenantId, ReconciliationRun run) {
    List<ReconciliationFinding> findings = new ArrayList<>();

    String debtsSql =
        "SELECT d.id, d.reference_type, d.reference_id FROM debts d "
            + "WHERE d.tenant_id = ? AND d.deleted_at IS NULL AND d.reference_id IS NOT NULL "
            + "AND ("
            + "  (d.reference_type = 'order' AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.id = d.reference_id AND o.tenant_id = d.tenant_id AND o.deleted_at IS NULL))"
            + "  OR (d.reference_type = 'purchase_order' AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.id = d.reference_id AND po.tenant_id = d.tenant_id AND po.deleted_at IS NULL))"
            + ")";
    findings.addAll(
        jdbcTemplate.query(
            debtsSql,
            (rs, rowNum) ->
                newFinding(
                    run,
                    tenantId,
                    "ORPHANED_REFERENCE",
                    "medium",
                    "debt",
                    rs.getLong("id"),
                    Map.of(
                        "referenceType", rs.getString("reference_type"),
                        "referenceId", rs.getLong("reference_id"))),
            tenantId));

    String txSql =
        "SELECT t.id, t.reference_type, t.reference_id FROM inventory_transactions t "
            + "WHERE t.tenant_id = ? AND t.deleted_at IS NULL AND t.reference_id IS NOT NULL "
            + "AND ("
            + "  (t.reference_type = 'order' AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.id = t.reference_id AND o.tenant_id = t.tenant_id AND o.deleted_at IS NULL))"
            + "  OR (t.reference_type = 'purchase_order' AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.id = t.reference_id AND po.tenant_id = t.tenant_id AND po.deleted_at IS NULL))"
            + "  OR (t.reference_type = 'stock_take' AND NOT EXISTS (SELECT 1 FROM stock_takes st WHERE st.id = t.reference_id AND st.tenant_id = t.tenant_id AND st.deleted_at IS NULL))"
            + ")";
    findings.addAll(
        jdbcTemplate.query(
            txSql,
            (rs, rowNum) ->
                newFinding(
                    run,
                    tenantId,
                    "ORPHANED_REFERENCE",
                    "medium",
                    "inventory_transaction",
                    rs.getLong("id"),
                    Map.of(
                        "referenceType", rs.getString("reference_type"),
                        "referenceId", rs.getLong("reference_id"))),
            tenantId));

    return findings;
  }

  /**
   * (g) Ca lam viec: doi voi ca DA DONG, tinh lai tien mat du kien tu dau (giong cong thuc
   * ShiftService.computeExpectedCash) va so voi (actual_cash - discrepancy) da luu luc dong ca —
   * lech nghia la co du lieu da doi sau khi ca dong (vd giao dich bi gan lai shift_id).
   */
  private List<ReconciliationFinding> checkShiftDiscrepancyMismatch(
      Long tenantId, ReconciliationRun run) {
    String sql =
        "WITH shift_calc AS ("
            + "  SELECT s.id, s.opening_cash, s.actual_cash, s.discrepancy,"
            + "    COALESCE((SELECT SUM(op.amount) FROM order_payments op JOIN orders o ON o.id = op.order_id"
            + "              WHERE o.shift_id = s.id AND op.method = 'cash' AND op.tenant_id = s.tenant_id AND op.deleted_at IS NULL), 0) AS cash_sales,"
            + "    COALESCE((SELECT SUM(r.total_refund) FROM returns r JOIN orders o ON o.id = r.order_id"
            + "              WHERE o.branch_id = s.branch_id AND r.refund_method = 'cash' AND r.tenant_id = s.tenant_id"
            + "              AND r.created_at >= s.opened_at AND r.created_at < s.closed_at), 0) AS cash_refunds,"
            + "    COALESCE((SELECT SUM(ct.amount) FROM cash_transactions ct WHERE ct.shift_id = s.id AND ct.type = 'cash_in' AND ct.tenant_id = s.tenant_id AND ct.deleted_at IS NULL), 0) AS cash_in,"
            + "    COALESCE((SELECT SUM(ct.amount) FROM cash_transactions ct WHERE ct.shift_id = s.id AND ct.type = 'cash_out' AND ct.tenant_id = s.tenant_id AND ct.deleted_at IS NULL), 0) AS cash_out"
            + "  FROM shifts s"
            + "  WHERE s.tenant_id = ? AND s.status = 'closed' AND s.deleted_at IS NULL"
            + ")"
            + "SELECT id, opening_cash, actual_cash, discrepancy, cash_sales, cash_refunds, cash_in, cash_out,"
            + "  (opening_cash + cash_sales - cash_refunds + cash_in - cash_out) AS recomputed_expected"
            + " FROM shift_calc"
            + " WHERE (actual_cash - discrepancy) <> (opening_cash + cash_sales - cash_refunds + cash_in - cash_out)";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Map<String, Object> details = new LinkedHashMap<>();
          details.put("actualCash", rs.getBigDecimal("actual_cash"));
          details.put("storedDiscrepancy", rs.getBigDecimal("discrepancy"));
          details.put("recomputedExpectedCash", rs.getBigDecimal("recomputed_expected"));
          return newFinding(
              run,
              tenantId,
              "SHIFT_DISCREPANCY_MISMATCH",
              "medium",
              "shift",
              rs.getLong("id"),
              details);
        },
        tenantId);
  }

  /**
   * (h) Tham chieu CHEO TENANT tren FK THAT (khac voi (f) chi soi cap reference_type/reference_id
   * da hinh) — vd orders.customer_id tro sang 1 customer thuoc tenant KHAC. Ve ly thuyet KHONG BAO
   * GIO xay ra nho TenantAwareRepositoryImpl + Hibernate @Filter (da xac nhan bang 8 test tich hop
   * that trong TenantIsolationIT), nhung day la lop phong thu THU HAI o tang du lieu — phat hien
   * qua audit production readiness (2026-07-17): du an nay tung co 2 loi ro ri tenant nghiem trong
   * o qua khu (xem bo nho "Hibernate @Filter gotchas"), nen 1 lop kiem tra doc lap o tang DB, chay
   * dinh ky, la phong ngua hop ly cho tuong lai neu co lo hong tuong tu tai xuat hien. Nghiem trong
   * hon ORPHANED_REFERENCE (medium) vi day la RO RI DU LIEU giua 2 khach hang, khong chi du lieu mo
   * coi — luon gan "critical".
   */
  private List<ReconciliationFinding> checkCrossTenantReferences(
      Long tenantId, ReconciliationRun run) {
    String sql =
        "SELECT 'order' AS entity_type, o.id AS entity_id, 'customer_id' AS fk_column, o.customer_id AS fk_value "
            + "FROM orders o JOIN customers c ON c.id = o.customer_id "
            + "WHERE o.tenant_id = ? AND o.deleted_at IS NULL AND o.customer_id IS NOT NULL AND c.tenant_id <> o.tenant_id "
            + "UNION ALL "
            + "SELECT 'order', o.id, 'branch_id', o.branch_id "
            + "FROM orders o JOIN branches b ON b.id = o.branch_id "
            + "WHERE o.tenant_id = ? AND o.deleted_at IS NULL AND b.tenant_id <> o.tenant_id "
            + "UNION ALL "
            + "SELECT 'order', o.id, 'voucher_id', o.voucher_id "
            + "FROM orders o JOIN vouchers v ON v.id = o.voucher_id "
            + "WHERE o.tenant_id = ? AND o.deleted_at IS NULL AND o.voucher_id IS NOT NULL AND v.tenant_id <> o.tenant_id "
            + "UNION ALL "
            + "SELECT 'order_item', oi.id, 'product_id', oi.product_id "
            + "FROM order_items oi JOIN products p ON p.id = oi.product_id "
            + "WHERE oi.tenant_id = ? AND oi.deleted_at IS NULL AND p.tenant_id <> oi.tenant_id "
            + "UNION ALL "
            + "SELECT 'debt', d.id, 'customer_id', d.customer_id "
            + "FROM debts d JOIN customers c ON c.id = d.customer_id "
            + "WHERE d.tenant_id = ? AND d.deleted_at IS NULL AND d.customer_id IS NOT NULL AND c.tenant_id <> d.tenant_id "
            + "UNION ALL "
            + "SELECT 'debt', d.id, 'supplier_id', d.supplier_id "
            + "FROM debts d JOIN suppliers s ON s.id = d.supplier_id "
            + "WHERE d.tenant_id = ? AND d.deleted_at IS NULL AND d.supplier_id IS NOT NULL AND s.tenant_id <> d.tenant_id "
            + "UNION ALL "
            + "SELECT 'purchase_order', po.id, 'supplier_id', po.supplier_id "
            + "FROM purchase_orders po JOIN suppliers s ON s.id = po.supplier_id "
            + "WHERE po.tenant_id = ? AND po.deleted_at IS NULL AND s.tenant_id <> po.tenant_id "
            + "UNION ALL "
            + "SELECT 'inventory', i.id, 'product_id', i.product_id "
            + "FROM inventory i JOIN products p ON p.id = i.product_id "
            + "WHERE i.tenant_id = ? AND i.deleted_at IS NULL AND p.tenant_id <> i.tenant_id";
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          Map<String, Object> details = new LinkedHashMap<>();
          details.put("fkColumn", rs.getString("fk_column"));
          details.put("fkValue", rs.getLong("fk_value"));
          return newFinding(
              run,
              tenantId,
              "CROSS_TENANT_REFERENCE",
              "critical",
              rs.getString("entity_type"),
              rs.getLong("entity_id"),
              details);
        },
        tenantId,
        tenantId,
        tenantId,
        tenantId,
        tenantId,
        tenantId,
        tenantId,
        tenantId);
  }

  private ReconciliationRunResponse toResponse(
      ReconciliationRun run, List<ReconciliationFinding> findings) {
    ReconciliationRunResponse response = new ReconciliationRunResponse();
    response.setId(run.getId());
    response.setTriggerType(run.getTriggerType());
    response.setStatus(run.getStatus());
    response.setFindingsCount(run.getFindingsCount());
    response.setDurationMs(run.getDurationMs());
    response.setStartedAt(run.getStartedAt());
    response.setFinishedAt(run.getFinishedAt());
    response.setFindings(findings.stream().map(this::toFindingResponse).toList());
    return response;
  }

  private ReconciliationFindingResponse toFindingResponse(ReconciliationFinding finding) {
    ReconciliationFindingResponse response = new ReconciliationFindingResponse();
    response.setId(finding.getId());
    response.setCheckType(finding.getCheckType());
    response.setSeverity(finding.getSeverity());
    response.setEntityType(finding.getEntityType());
    response.setEntityId(finding.getEntityId());
    response.setDetails(finding.getDetails());
    response.setStatus(finding.getStatus());
    return response;
  }
}
