package dev.daniel.supply.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
  @Id
  @Column(length = 40)
  private String sku;

  @Column(nullable = false, length = 120)
  private String name;

  protected Product() {}

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }
}
