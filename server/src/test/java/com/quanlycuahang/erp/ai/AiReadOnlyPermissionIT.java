package com.quanlycuahang.erp.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prompt #11 - CHUNG MINH tang an toan bat buoc cua module AI Assistant: connection AI (role
 * Postgres {@code fruithouse_ai_readonly}, V30) KHONG THE INSERT/UPDATE/DELETE tren bat ky bang
 * nao, KHONG THE SELECT truc tiep tren bang goc (chi qua 4 view whitelist), va 1 mau "prompt
 * injection" (chuoi gia lap AI bi lua "muon" chay lenh pha hoai) KHONG co hieu ung ghi nao — dung
 * bean {@code aiReadOnlyJdbcTemplate} THAT SU cua ung dung (khong phai tu tao ket noi rieng), de
 * xac nhan chinh xac cau hinh dang chay, khong phai 1 cau hinh gia lap khac.
 *
 * <p>Dung {@code .rootCause().hasMessageContaining(...)} thay vi kiem tra message cua chinh
 * exception: Spring dich loi SQLSTATE class "42" (Syntax Error or Access Rule Violation - gom CA
 * loi cu phap LAN loi "permission denied", ma Postgres tra ve "42501") thanh {@code
 * BadSqlGrammarException} voi message rieng "bad SQL grammar [...]", KHONG phai message that tu
 * Postgres - message that ("permission denied for table ...") chi nam o {@code getRootCause()}.
 */
class AiReadOnlyPermissionIT extends AbstractIntegrationTest {

  @Autowired
  @Qualifier("aiReadOnlyJdbcTemplate")
  private JdbcTemplate aiReadOnlyJdbcTemplate;

  @Autowired private OrderRepository orderRepository;

  @Test
  void cannotInsertIntoAnyTable() {
    assertThatThrownBy(
            () ->
                aiReadOnlyJdbcTemplate.update(
                    "INSERT INTO orders (order_number, branch_id, cashier_id, tenant_id) VALUES"
                        + " ('HACK-INSERT', 1, 1, 1)"))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
  }

  @Test
  void cannotUpdateAnyTable() {
    assertThatThrownBy(() -> aiReadOnlyJdbcTemplate.update("UPDATE products SET sell_price = 0"))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
  }

  @Test
  void cannotDeleteFromAnyTable() {
    assertThatThrownBy(() -> aiReadOnlyJdbcTemplate.update("DELETE FROM orders"))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
  }

  @Test
  void cannotSelectBaseTablesDirectly() {
    assertThatThrownBy(() -> aiReadOnlyJdbcTemplate.queryForList("SELECT * FROM orders"))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
    assertThatThrownBy(() -> aiReadOnlyJdbcTemplate.queryForList("SELECT * FROM users"))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");
  }

  @Test
  void canSelectWhitelistedViews() {
    // Khong can du lieu that - chi can xac nhan lenh SELECT tren view duoc CHAP THUAN (khong nem
    // loi permission denied), khac han voi bang goc o test tren.
    List<Map<String, Object>> rows =
        aiReadOnlyJdbcTemplate.queryForList(
            "SELECT set_config('app.current_tenant_id', '1', false)");
    assertThat(rows).hasSize(1);

    assertThat(aiReadOnlyJdbcTemplate.queryForList("SELECT * FROM v_ai_revenue")).isNotNull();
    assertThat(aiReadOnlyJdbcTemplate.queryForList("SELECT * FROM v_ai_top_products")).isNotNull();
    assertThat(aiReadOnlyJdbcTemplate.queryForList("SELECT * FROM v_ai_inventory_summary"))
        .isNotNull();
    assertThat(aiReadOnlyJdbcTemplate.queryForList("SELECT * FROM v_ai_debt_aging")).isNotNull();
  }

  @Test
  void promptInjectionSampleHasNoWriteEffect() {
    // Mau "prompt injection": gia su AI bi lua tra ve 1 chuoi CO Y DINH pha hoai thay vi 1 gia tri
    // tham so binh thuong (kich ban thuc te: neu 1 bug tuong lai vo tinh dua chuoi nay vao 1 cau
    // lenh). Truyen NHU 1 CAU LENH RIENG (khong phai bind param bi trich xuat, de mo phong dung
    // tinh huong xau nhat: ke ca khi 1 doan code nao do CHAY THANG chuoi nay nhu 1 lenh SQL) - tang
    // quyen Postgres van tu choi vi role khong co GRANT ghi tren bat ky bang nao, dung y roadmap:
    // "ke ca khi prompt injection thanh cong, DB tu choi lenh ghi o tang quyen".
    String maliciousInput = "'; DELETE FROM orders; --";
    long ordersBefore = orderRepository.count();

    assertThatThrownBy(
            () ->
                aiReadOnlyJdbcTemplate.update(
                    "DELETE FROM orders WHERE order_number = '" + maliciousInput + "'"))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("permission denied");

    // Dem lai qua JPA (session thuong cua test, khong phai AI) - xac nhan KHONG co dong nao trong
    // bang orders bi xoa boi cau lenh tren (bat ke tenant nao, vi @Filter khong duoc bat trong
    // test nay nen count() la tren TOAN BO bang).
    assertThat(orderRepository.count()).isEqualTo(ordersBefore);
  }
}
