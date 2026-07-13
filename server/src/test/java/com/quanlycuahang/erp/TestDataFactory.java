package com.quanlycuahang.erp;

import com.quanlycuahang.erp.auth.entity.Role;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.RoleRepository;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.operation.entity.Shift;
import com.quanlycuahang.erp.operation.repository.ShiftRepository;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.repository.CustomerRepository;
import com.quanlycuahang.erp.partner.repository.SupplierRepository;
import com.quanlycuahang.erp.product.entity.Category;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.CategoryRepository;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Dung du lieu chuan cho integration test - moi lan goi createTenantWithBranches() tao 1 tenant
 * HOAN TOAN MOI (ten/username co UUID suffix) de cac test chay song song/lap lai khong dam vao
 * nhau, thay vi dua vao du lieu seed co san (V2__seed_data.sql) von thuoc ve tenant #1 "that".
 *
 * <p>Moi Entity duoc gan tenant/branch TUONG MINH qua setTenant()/setBranch() (khong dua
 * vao @PrePersist doc TenantContext) de factory nay dung duoc bat ke test da actingAsTenant() hay
 * chua tai thoi diem goi.
 */
@Component
public class TestDataFactory {

  private final TenantRepository tenantRepository;
  private final BranchRepository branchRepository;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final InventoryRepository inventoryRepository;
  private final CustomerRepository customerRepository;
  private final SupplierRepository supplierRepository;
  private final ShiftRepository shiftRepository;
  private final PasswordEncoder passwordEncoder;

  public TestDataFactory(
      TenantRepository tenantRepository,
      BranchRepository branchRepository,
      UserRepository userRepository,
      RoleRepository roleRepository,
      CategoryRepository categoryRepository,
      ProductRepository productRepository,
      InventoryRepository inventoryRepository,
      CustomerRepository customerRepository,
      SupplierRepository supplierRepository,
      ShiftRepository shiftRepository,
      PasswordEncoder passwordEncoder) {
    this.tenantRepository = tenantRepository;
    this.branchRepository = branchRepository;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
    this.inventoryRepository = inventoryRepository;
    this.supplierRepository = supplierRepository;
    this.customerRepository = customerRepository;
    this.shiftRepository = shiftRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public record TestTenant(
      Tenant tenant, Branch branchA, Branch branchB, User owner, User cashier) {}

  /**
   * Tao 1 tenant moi + 2 chi nhanh + 1 owner (toan quyen ca 2 chi nhanh) + 1 cashier (chi branchA)
   * - dung lam "tenant A" hoac "tenant B" trong test cach ly du lieu.
   */
  public TestTenant createTenantWithBranches(String namePrefix) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    Tenant tenant = new Tenant();
    tenant.setName(namePrefix + "-" + suffix);
    tenant.setActive(true);
    tenant = tenantRepository.save(tenant);

    Branch branchA = new Branch();
    branchA.setTenant(tenant);
    branchA.setName("Chi nhanh A");
    branchA.setActive(true);
    branchA = branchRepository.save(branchA);

    Branch branchB = new Branch();
    branchB.setTenant(tenant);
    branchB.setName("Chi nhanh B");
    branchB.setActive(true);
    branchB = branchRepository.save(branchB);

    Role ownerRole = roleRepository.findByCode("owner").orElseThrow();
    Role cashierRole = roleRepository.findByCode("cashier").orElseThrow();

    User owner = new User();
    owner.setTenant(tenant);
    owner.setUsername("owner-" + suffix);
    owner.setPasswordHash(passwordEncoder.encode("Test@12345"));
    owner.setFullName("Chu cua hang test");
    owner.setActive(true);
    owner.setRoles(Set.of(ownerRole));
    owner.setBranches(Set.of(branchA, branchB));
    owner = userRepository.save(owner);

    User cashier = new User();
    cashier.setTenant(tenant);
    cashier.setUsername("cashier-" + suffix);
    cashier.setPasswordHash(passwordEncoder.encode("Test@12345"));
    cashier.setFullName("Thu ngan test");
    cashier.setActive(true);
    cashier.setRoles(Set.of(cashierRole));
    cashier.setBranches(Set.of(branchA));
    cashier = userRepository.save(cashier);

    return new TestTenant(tenant, branchA, branchB, owner, cashier);
  }

  /** Tao 1 san pham co danh muc rieng + 1 dong ton kho tai branch cho truoc. */
  public Product createProductWithStock(
      Tenant tenant, Branch branch, BigDecimal sellPrice, BigDecimal costPrice, BigDecimal stock) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    Category category = new Category();
    category.setTenant(tenant);
    category.setName("Danh muc test " + suffix);
    category = categoryRepository.save(category);

    Product product = new Product();
    product.setTenant(tenant);
    product.setCategory(category);
    product.setSku("SKU-" + suffix);
    product.setName("San pham test " + suffix);
    product.setUnit("Cai");
    product.setSellPrice(sellPrice);
    product.setPriceIncludesVat(true);
    product.setVatRate(BigDecimal.ZERO);
    product.setMinStock(BigDecimal.ZERO);
    product.setActive(true);
    product = productRepository.save(product);

    Inventory inventory = new Inventory();
    inventory.setTenant(tenant);
    inventory.setProduct(product);
    inventory.setBranch(branch);
    inventory.setStock(stock);
    inventory.setCostPrice(costPrice);
    inventoryRepository.save(inventory);

    return product;
  }

  public Supplier createSupplier(Tenant tenant) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Supplier supplier = new Supplier();
    supplier.setTenant(tenant);
    supplier.setName("Nha cung cap test " + suffix);
    supplier.setPhone("08" + System.nanoTime() % 100_000_000L);
    return supplierRepository.save(supplier);
  }

  public Customer createCustomerWithDebtLimit(Tenant tenant, BigDecimal debtLimit) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Customer customer = new Customer();
    customer.setTenant(tenant);
    customer.setName("Khach hang test " + suffix);
    customer.setPhone("09" + System.nanoTime() % 100_000_000L);
    customer.setDebtLimit(debtLimit);
    return customerRepository.save(customer);
  }

  /**
   * Mo 1 ca lam viec dang "open" cho user tai branch cho truoc - dung khi test can 1 ca san co (vd
   * OrderService gan shiftId vao don).
   */
  public Shift openShift(Tenant tenant, Branch branch, User openedBy, BigDecimal openingCash) {
    Shift shift = new Shift();
    shift.setTenant(tenant);
    shift.setBranch(branch);
    shift.setOpenedBy(openedBy);
    shift.setOpeningCash(openingCash);
    shift.setStatus("open");
    shift.setOpenedAt(OffsetDateTime.now());
    return shiftRepository.save(shift);
  }
}
