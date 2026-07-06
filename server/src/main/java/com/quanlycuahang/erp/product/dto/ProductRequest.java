package com.quanlycuahang.erp.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class ProductRequest {

  private Long categoryId;

  /** De trong -> he thong tu sinh theo mau cau hinh sku_prefix (UC-03). */
  private String sku;

  private String barcode;

  @NotBlank private String name;

  @NotBlank private String unit;

  @NotNull @PositiveOrZero private BigDecimal sellPrice;

  private boolean priceIncludesVat = true;

  @NotNull private BigDecimal vatRate;

  @NotNull @PositiveOrZero private BigDecimal minStock;

  private String imageUrl;

  private String originCountry;

  private String originRegion;

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
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
