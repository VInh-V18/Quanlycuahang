package com.quanlycuahang.erp.product.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "product_units")
@SQLDelete(sql = "UPDATE product_units SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ProductUnit extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "unit_name", nullable = false)
  private String unitName;

  @Column(name = "conversion_rate", nullable = false)
  private BigDecimal conversionRate;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public String getUnitName() {
    return unitName;
  }

  public void setUnitName(String unitName) {
    this.unitName = unitName;
  }

  public BigDecimal getConversionRate() {
    return conversionRate;
  }

  public void setConversionRate(BigDecimal conversionRate) {
    this.conversionRate = conversionRate;
  }
}
