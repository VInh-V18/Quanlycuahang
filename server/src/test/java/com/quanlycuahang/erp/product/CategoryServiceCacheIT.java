package com.quanlycuahang.erp.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.product.dto.CategoryRequest;
import com.quanlycuahang.erp.product.dto.CategoryResponse;
import com.quanlycuahang.erp.product.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prompt #7 (P2, hieu nang): CategoryService.list() gio cache Caffeine theo tenantId - test nay dam
 * bao 2 dieu KHONG the bo qua o he thong multi-tenant: (1) cache khong lam RO ri du lieu giua 2
 * tenant (key rieng tung tenant), (2) invalidate dung khi ghi (tao danh muc moi phai thay ngay,
 * khong doi het TTL 120s).
 */
class CategoryServiceCacheIT extends AbstractIntegrationTest {

  @Autowired private TestDataFactory testDataFactory;
  @Autowired private CategoryService categoryService;

  private CategoryRequest request(String name) {
    CategoryRequest request = new CategoryRequest();
    request.setName(name);
    request.setDisplayOrder(0);
    return request;
  }

  @Test
  void listIsScopedPerTenantAndInvalidatedOnWrite() {
    TestDataFactory.TestTenant tenantA =
        testDataFactory.createTenantWithBranches("tenantCategoryCacheA");
    actingAsUser(tenantA.owner());
    categoryService.create(request("Danh muc A1"));
    List<CategoryResponse> listA = categoryService.list();
    assertThat(listA).extracting(CategoryResponse::getName).containsExactly("Danh muc A1");

    TestDataFactory.TestTenant tenantB =
        testDataFactory.createTenantWithBranches("tenantCategoryCacheB");
    unbindCurrentThread();
    actingAsUser(tenantB.owner());
    // Tenant B chua tung tao danh muc nao - PHAI thay rong, khong duoc dinh cache cua tenant A.
    assertThat(categoryService.list()).isEmpty();
    categoryService.create(request("Danh muc B1"));
    List<CategoryResponse> listB = categoryService.list();
    assertThat(listB).extracting(CategoryResponse::getName).containsExactly("Danh muc B1");

    unbindCurrentThread();
    actingAsUser(tenantA.owner());
    // Quay lai tenant A: van chi thay dung danh muc cua A, khong lan sang B.
    assertThat(categoryService.list())
        .extracting(CategoryResponse::getName)
        .containsExactly("Danh muc A1");
    categoryService.create(request("Danh muc A2"));
    // Tao moi phai thay NGAY (invalidate on write), khong doi TTL.
    assertThat(categoryService.list())
        .extracting(CategoryResponse::getName)
        .containsExactlyInAnyOrder("Danh muc A1", "Danh muc A2");
  }
}
