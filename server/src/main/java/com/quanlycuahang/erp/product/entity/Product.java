package com.quanlycuahang.erp.product.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * San pham. Gia von KHONG luu o day — nam o Inventory (theo tung chi nhanh, bien dong khi nhap hang
 * theo binh quan gia quyen — B4).
 */
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Product extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(name = "sku", nullable = false, unique = true)
  private String sku;

  @Column(name = "barcode", unique = true)
  private String barcode;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "sell_price", nullable = false)
  private BigDecimal sellPrice;

  @Column(name = "price_includes_vat", nullable = false)
  private boolean priceIncludesVat = true;

  @Column(name = "vat_rate", nullable = false)
  private BigDecimal vatRate;

  @Column(name = "min_stock", nullable = false)
  private BigDecimal minStock;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "origin_country")
  private String originCountry;

  @Column(name = "origin_region")
  private String originRegion;

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public BigDecimal getSellPrice() {
    return sellPrice;
  }

  public void setSellPrice(BigDecimal sellPrice) {
    this.sellPrice = sellPrice;
  }

  public boolean isPriceIncludesVat() {
    return priceIncludesVat;
  }

  public void setPriceIncludesVat(boolean priceIncludesVat) {
    this.priceIncludesVat = priceIncludesVat;
  }

  public BigDecimal getVatRate() {
    return vatRate;
  }

  public void setVatRate(BigDecimal vatRate) {
    this.vatRate = vatRate;
  }

  public BigDecimal getMinStock() {
    return minStock;
  }

  public void setMinStock(BigDecimal minStock) {
    this.minStock = minStock;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getOriginCountry() {
    return originCountry;
  }

  public void setOriginCountry(String originCountry) {
    this.originCountry = originCountry;
  }

  public String getOriginRegion() {
    return originRegion;
  }

  public void setOriginRegion(String originRegion) {
    this.originRegion = originRegion;
  }
}
