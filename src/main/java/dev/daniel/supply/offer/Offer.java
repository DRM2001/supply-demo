package dev.daniel.supply.offer;

import dev.daniel.supply.catalog.*;
import dev.daniel.supply.domain.Region;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "offers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_sku", "supplier_id", "region"}))
public class Offer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private Long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_sku")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supplier_id")
  private Supplier supplier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Region region;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private int stock;

  @Column(nullable = false)
  private int leadDays;

  protected Offer() {}

  public Offer(
      Product product,
      Supplier supplier,
      Region region,
      BigDecimal unitPrice,
      int stock,
      int leadDays) {
    this.product = product;
    this.supplier = supplier;
    this.region = region;
    revise(unitPrice, stock, leadDays);
  }

  public void revise(BigDecimal price, int stock, int leadDays) {
    this.unitPrice = price;
    this.stock = stock;
    this.leadDays = leadDays;
  }

  public Long getId() {
    return id;
  }

  public Long getVersion() {
    return version;
  }

  public Product getProduct() {
    return product;
  }

  public Supplier getSupplier() {
    return supplier;
  }

  public Region getRegion() {
    return region;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public int getStock() {
    return stock;
  }

  public int getLeadDays() {
    return leadDays;
  }
}
