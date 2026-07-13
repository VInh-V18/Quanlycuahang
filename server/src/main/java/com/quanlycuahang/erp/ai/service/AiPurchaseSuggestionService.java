package com.quanlycuahang.erp.ai.service;

import com.quanlycuahang.erp.ai.dto.PurchaseSuggestionResponse;
import com.quanlycuahang.erp.ai.repository.AiPurchaseSuggestionRepository;
import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Goi y nhap hang (Prompt #11, tinh nang dot 1b: "tu toc do ban 30 ngay + ton hien tai + dinh muc
 * ton toi thieu → danh sach SP nen nhap kem so luong goi y").
 *
 * <p><b>CO CHU DICH KHONG goi AI/LLM cho tinh nang nay</b> - du roadmap dat ten "Goi y tu AI", ban
 * chat day la 1 CONG THUC XAC DINH (deterministic): toc do ban trung binh/ngay (30 ngay gan nhat) x
 * so ngay muon phu (30 ngay) so voi ton hien tai va dinh muc toi thieu. Day KHONG phai bai toan can
 * suy luan ngon ngu tu nhien - goi 1 LLM de lam phep tinh so hoc nay se: (1) cham hon (round-trip
 * mang toi API ngoai) va TON PHI khong can thiet, (2) co rui ro LLM tinh sai so (hallucination) o
 * dung cho phep tinh tien/so luong - vi pham nguyen tac "Backend la nguon chan ly duy nhat cho tinh
 * tien" cua du an. Nut "Goi y tu AI" o FE la ten hien thi/UX, KHONG phai cam ket cong nghe ben
 * duoi; dieu nay duoc ghi ro trong PROJECT_STATE.md de tranh hieu nham sau nay.
 *
 * <p>Cong thuc: target_stock = MAX(dinh_muc_toi_thieu, toc_do_ban_ngay x 30), goi_y = target_stock
 * - ton_hien_tai (chi tra ve khi > 0).
 */
@Service
public class AiPurchaseSuggestionService {

  private static final int LOOKBACK_DAYS = 30;
  private static final int TARGET_COVER_DAYS = 30;

  private final AiPurchaseSuggestionRepository repository;
  private final BranchAccessGuard branchAccessGuard;

  public AiPurchaseSuggestionService(
      AiPurchaseSuggestionRepository repository, BranchAccessGuard branchAccessGuard) {
    this.repository = repository;
    this.branchAccessGuard = branchAccessGuard;
  }

  @Transactional(readOnly = true)
  public List<PurchaseSuggestionResponse> suggest(Long branchId) {
    branchAccessGuard.assertAccess(branchId);
    Long tenantId = TenantContext.get();
    List<Object[]> rows = repository.findVelocityDataForBranch(branchId, tenantId);

    List<PurchaseSuggestionResponse> suggestions = new ArrayList<>();
    for (Object[] row : rows) {
      Long productId = ((Number) row[0]).longValue();
      String productName = (String) row[1];
      String sku = (String) row[2];
      BigDecimal currentStock = (BigDecimal) row[3];
      BigDecimal minStock = (BigDecimal) row[4];
      BigDecimal qtySold30d = (BigDecimal) row[5];

      BigDecimal dailyVelocity =
          qtySold30d.divide(BigDecimal.valueOf(LOOKBACK_DAYS), 4, RoundingMode.HALF_UP);
      BigDecimal targetStock =
          dailyVelocity.multiply(BigDecimal.valueOf(TARGET_COVER_DAYS)).max(minStock);
      BigDecimal suggestedQty =
          targetStock.subtract(currentStock).setScale(0, RoundingMode.CEILING);

      if (suggestedQty.compareTo(BigDecimal.ZERO) > 0) {
        suggestions.add(
            new PurchaseSuggestionResponse(
                productId, productName, sku, currentStock, minStock, suggestedQty));
      }
    }

    // Uu tien hien thi san pham THIEU NHIEU NHAT so voi dinh muc truoc (chenh lech
    // stock - minStock cang am cang khan cap) - giup nguoi dung duyet tung dong theo dung thu tu
    // can quyet dinh truoc, khong phai xep ngau nhien.
    suggestions.sort(
        Comparator.comparing(
            (PurchaseSuggestionResponse s) -> s.getCurrentStock().subtract(s.getMinStock())));
    return suggestions;
  }
}
