package dev.daniel.supply.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier {
  @Id private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  protected Supplier() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
