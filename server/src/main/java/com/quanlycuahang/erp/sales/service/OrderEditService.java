package com.quanlycuahang.erp.sales.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.sales.dto.EditOrderLineRequest;
import com.quanlycuahang.erp.sales.dto.EditOrderRequest;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.entity.OrderPayment;
import com.quanlycuahang.erp.sales.pricing.OrderLineInput;
import com.quanlycuahang.erp.sales.pricing.OrderLineResult;
import com.quanlycuahang.erp.sales.pricing.OrderPricingRequest;
import com.quanlycuahang.erp.sales.pricing.OrderPricingResult;
import com.quanlycuahang.erp.sales.pricing.OrderPricingService;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.statemachine.OrderStatus;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sua don da hoan tat (order:edit, tinh nang moi) — owner/manager sua so luong/san pham/don
 * gia/giam gia tren 1 don DA CHOT, khong gioi han thoi gian (khac Huy don van chi trong ngay).
 * KHONG tai su dung InventoryDeductionService/ReturnService (xem Javadoc InventoryDeductionService
 * — huong tru/cong kho va nguon gia von khac nhau giua cac luong), tu mirror logic delta rieng.
 *
 * <p>Yeu cau moi san pham chi xuat hien 1 dong trong request (khac luc tao don, cho phep nhieu dong
 * trung productId voi CK khac nhau) — don gian hoa co chu dich vi UI Sua don hien 1 dong/san pham.
 * Neu don GOC (truoc khi sua) tinh co nhieu OrderItem cung productId, cac dong do duoc GOP lai
 * thanh 1 khi sua (giu snapshot gia von cua dong dau, xoa cac dong con lai) — danh doi chap nhan
 * duoc vi day la thao tac chu dong cua owner/manager, khong am tham xay ra ngoai y muon.
 */
@Service
public class OrderEditService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final ProductRepository productRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final DebtRepository debtRepository;
  private final SettingsService settingsService;
  private final CurrentUserProvider currentUserProvider;
  private final BranchAccessGuard branchAccessGuard;
  private final OrderValidationService orderValidationService;

  public OrderEditService(
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      OrderPaymentRepository orderPaymentRepository,
      ProductRepository productRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      DebtRepository debtRepository,
      SettingsService settingsService,
      CurrentUserProvider currentUserProvider,
      BranchAccessGuard branchAccessGuard,
      OrderValidationService orderValidationService) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderPaymentRepository = orderPaymentRepository;
    this.productRepository = productRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.debtRepository = debtRepository;
    this.settingsService = settingsService;
    this.currentUserProvider = currentUserProvider;
    this.branchAccessGuard = branchAccessGuard;
    this.orderValidationService = orderValidationService;
  }

  @Transactional
  public void editOrder(Long orderId, EditOrderRequest request) {
    Order order =
        orderRepository
            .findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    branchAccessGuard.assertAccess(order.getBranch().getId());

    if (OrderStatus.fromValue(order.getStatus()) != OrderStatus.COMPLETED) {
      throw new BusinessRuleException(
          "ORDER_EDIT_NOT_ALLOWED",
          "Chỉ có thể sửa đơn ở trạng thái đã hoàn tất (chưa trả hàng/chưa hủy)");
    }

    long distinctRequestedProducts =
        request.getLines().stream().map(EditOrderLineRequest::getProductId).distinct().count();
    if (distinctRequestedProducts != request.getLines().size()) {
      throw new BusinessRuleException(
          "ORDER_EDIT_DUPLICATE_PRODUCT", "Mỗi sản phẩm chỉ được xuất hiện 1 dòng trong đơn");
    }

    List<Debt> orderDebts =
        debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", order.getId());
    boolean hasPartialPayment =
        orderDebts.stream().anyMatch(d -> d.getAmount().compareTo(d.getOriginalAmount()) != 0);
    if (hasPartialPayment) {
      throw new BusinessRuleException(
          "ORDER_EDIT_DEBT_ALREADY_PAID",
          "Không thể sửa đơn vì công nợ liên quan đã được thu một phần, vui lòng xử lý công nợ"
              + " trước");
    }

    Map<Long, List<OrderItem>> oldItemsByProductId =
        orderItemRepository.findByOrderId(order.getId()).stream()
            .collect(Collectors.groupingBy(oi -> oi.getProduct().getId()));
    Map<Long, BigDecimal> oldQuantityByProductId = new HashMap<>();
    oldItemsByProductId.forEach(
        (productId, items) ->
            oldQuantityByProductId.put(
                productId,
                items.stream()
                    .map(OrderItem::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)));

    List<Long> requestedProductIds =
        request.getLines().stream().map(EditOrderLineRequest::getProductId).toList();
    Map<Long, Product> productsById =
        productRepository.findAllById(requestedProductIds).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
    Map<Long, Inventory> inventoriesByProductId =
        inventoryRepository
            .findByBranchIdAndProductIdIn(order.getBranch().getId(), requestedProductIds)
            .stream()
            .collect(Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));

    boolean allowNegativeStock =
        settingsService.getBoolean(
            order.getBranch().getId(), SettingsService.KEY_ALLOW_NEGATIVE_STOCK, false);
    Map<Long, BigDecimal> newQuantityByProductId = new HashMap<>();
    for (EditOrderLineRequest line : request.getLines()) {
      newQuantityByProductId.put(line.getProductId(), line.getQuantity());
      Product product = productsById.get(line.getProductId());
      if (product == null) {
        throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
      }
      Inventory inventory = inventoriesByProductId.get(line.getProductId());
      if (inventory == null) {
        throw new BusinessRuleException(
            "PRODUCT_OUT_OF_STOCK",
            "Sản phẩm " + product.getName() + " chưa có tồn kho tại chi nhánh này");
      }
      BigDecimal oldQuantity =
          oldQuantityByProductId.getOrDefault(line.getProductId(), BigDecimal.ZERO);
      BigDecimal additionalNeeded = line.getQuantity().subtract(oldQuantity);
      if (!allowNegativeStock
          && additionalNeeded.compareTo(BigDecimal.ZERO) > 0
          && inventory.getStock().compareTo(additionalNeeded) < 0) {
        throw new BusinessRuleException(
            "PRODUCT_OUT_OF_STOCK",
            "Sản phẩm " + product.getName() + " chỉ còn " + inventory.getStock() + " trong kho");
      }
    }

    // Dieu chinh kho theo tung san pham (delta = so luong moi - so luong cu). Ban ghi
    // InventoryTransaction rieng, khong dung lai InventoryDeductionService/ReturnService (xem
    // Javadoc lop nay).
    var editor = currentUserProvider.getCurrentUser();
    List<InventoryTransaction> stockMovements = new ArrayList<>();
    Set<Long> touchedProductIds = new HashSet<>(newQuantityByProductId.keySet());
    touchedProductIds.addAll(oldQuantityByProductId.keySet());
    for (Long productId : touchedProductIds) {
      BigDecimal oldQuantity = oldQuantityByProductId.getOrDefault(productId, BigDecimal.ZERO);
      BigDecimal newQuantity = newQuantityByProductId.getOrDefault(productId, BigDecimal.ZERO);
      BigDecimal delta = newQuantity.subtract(oldQuantity);
      if (delta.compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }
      Inventory inventory =
          inventoryRepository
              .findByProductIdAndBranchId(productId, order.getBranch().getId())
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tồn kho"));
      inventory.setStock(inventory.getStock().subtract(delta));
      inventoryRepository.saveAndFlush(inventory);

      List<OrderItem> oldGroup = oldItemsByProductId.get(productId);
      boolean restocking = delta.compareTo(BigDecimal.ZERO) < 0;
      BigDecimal unitCost =
          restocking && oldGroup != null
              ? oldGroup.get(0).getCostPriceSnapshot()
              : inventory.getCostPrice();

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(
          oldGroup != null ? oldGroup.get(0).getProduct() : productsById.get(productId));
      transaction.setBranch(order.getBranch());
      transaction.setType("order_edit");
      transaction.setQuantity(delta.negate());
      transaction.setUnitCost(unitCost);
      transaction.setReferenceType("order");
      transaction.setReferenceId(order.getId());
      editor.ifPresent(transaction::setCreatedBy);
      stockMovements.add(transaction);
    }
    inventoryTransactionRepository.saveAll(stockMovements);

    // Tinh lai tien theo dung cong thuc 7 buoc (OrderPricingService) - unitPrice/lineDiscountAmount
    // lay THANG tu request (owner/manager ghi de gia thu cong), vatRate lay LIVE tu Product (dong
    // bo voi luc tao don, khong giu snapshot cu).
    List<OrderLineInput> pricingLines = new ArrayList<>();
    for (EditOrderLineRequest line : request.getLines()) {
      Product product = productsById.get(line.getProductId());
      pricingLines.add(
          new OrderLineInput(
              line.getProductId(),
              line.getUnitPrice(),
              line.getQuantity(),
              line.getLineDiscountAmount(),
              product.getVatRate()));
    }
    boolean priceIncludesVat =
        settingsService.getBoolean(
            order.getBranch().getId(), SettingsService.KEY_PRICE_INCLUDES_VAT_DEFAULT, true);
    BigDecimal roundingUnit =
        settingsService.getBigDecimal(
            order.getBranch().getId(), SettingsService.KEY_ROUNDING_UNIT, BigDecimal.valueOf(1000));
    OrderPricingRequest pricingRequest =
        new OrderPricingRequest(
            pricingLines, BigDecimal.ZERO, BigDecimal.ZERO, priceIncludesVat, roundingUnit, null);
    OrderPricingResult pricing = OrderPricingService.calculate(pricingRequest);

    // Xoa het dong cu (ke ca cac dong trung productId bi gop, xem Javadoc lop), roi tao/cap nhat
    // lai dung 1 dong/san pham theo ket qua tinh tien moi.
    for (List<OrderItem> group : oldItemsByProductId.values()) {
      for (OrderItem oldItem : group) {
        if (!newQuantityByProductId.containsKey(oldItem.getProduct().getId())
            || group.get(0) != oldItem) {
          orderItemRepository.delete(oldItem);
        }
      }
    }
    List<OrderItem> savedItems = new ArrayList<>();
    for (OrderLineResult lineResult : pricing.getLines()) {
      Product product = productsById.get(lineResult.getProductId());
      Inventory inventory = inventoriesByProductId.get(lineResult.getProductId());
      List<OrderItem> group = oldItemsByProductId.get(lineResult.getProductId());
      OrderItem item = group != null ? group.get(0) : new OrderItem();
      if (group == null) {
        item.setOrder(order);
        item.setProduct(product);
        item.setCostPriceSnapshot(inventory.getCostPrice());
      }
      item.setProductNameSnapshot(product.getName());
      item.setUnitPriceSnapshot(lineResult.getUnitPrice());
      item.setVatRateSnapshot(product.getVatRate());
      item.setQuantity(lineResult.getQuantity());
      item.setDiscountAmount(lineResult.getDiscountAmount());
      item.setVatAmount(lineResult.getVatAmount());
      item.setLineTotal(lineResult.getLineTotal());
      savedItems.add(item);
    }
    orderItemRepository.saveAll(savedItems);

    order.setSubtotalAmount(pricing.getSubtotalAmount());
    order.setDiscountAmount(pricing.getDiscountAmount());
    order.setVatAmount(pricing.getVatAmount());
    order.setRoundingAdjustment(pricing.getRoundingAdjustment());
    BigDecimal newTotal = pricing.getTotalAmount().add(order.getShippingFee());
    order.setTotalAmount(newTotal);

    // Doi chieu cong no: alreadyPaid khong doi (khach khong phai tra lai tien da thu), phan chenh
    // lech (tang/giam tong don) chuyen thanh cong no moi hoac giam bot - AN TOAN vi buoc tren da
    // dam bao khong co Debt nao dang thu do dang (partial payment) truoc khi toi day.
    BigDecimal totalPaid =
        orderPaymentRepository.findByOrderId(order.getId()).stream()
            .map(OrderPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal newUnpaid = newTotal.subtract(totalPaid);
    if (newUnpaid.compareTo(BigDecimal.ZERO) > 0) {
      orderValidationService.assertUnpaidRequiresCustomer(newUnpaid, order.getCustomer());
      if (order.getCustomer() != null) {
        orderValidationService.assertWithinDebtLimit(order.getCustomer(), newUnpaid);
      }
      Debt debt = orderDebts.isEmpty() ? new Debt() : orderDebts.get(0);
      debt.setCustomer(order.getCustomer());
      debt.setDirection("receivable");
      debt.setAmount(newUnpaid);
      debt.setOriginalAmount(newUnpaid);
      debt.setReferenceType("order");
      debt.setReferenceId(order.getId());
      debtRepository.save(debt);
      for (int i = 1; i < orderDebts.size(); i++) {
        Debt extra = orderDebts.get(i);
        extra.setAmount(BigDecimal.ZERO);
        debtRepository.save(extra);
      }
    } else {
      for (Debt debt : orderDebts) {
        debt.setAmount(BigDecimal.ZERO);
        debtRepository.save(debt);
      }
    }

    orderRepository.save(order);
  }
}
