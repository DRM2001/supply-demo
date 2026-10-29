package dev.daniel.supply.web;

import dev.daniel.supply.catalog.*;
import dev.daniel.supply.domain.Region;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Validated
public class CatalogController {
  private final ProductRepository products;
  private final SupplierRepository suppliers;

  public CatalogController(ProductRepository products, SupplierRepository suppliers) {
    this.products = products;
    this.suppliers = suppliers;
  }

  public record ProductView(String sku, String name) {}

  public record SupplierView(long id, String name) {}

  public record ProductPage(
      List<ProductView> content, int page, int size, long totalElements, int totalPages) {}

  @GetMapping("/products")
  public ProductPage products(
      @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    var result =
        products
            .findAll(PageRequest.of(page, size, Sort.by("sku")))
            .map(p -> new ProductView(p.getSku(), p.getName()));
    return new ProductPage(
        result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
  }

  @GetMapping("/suppliers")
  public List<SupplierView> suppliers() {
    return suppliers.findAll(Sort.by("id")).stream()
        .map(s -> new SupplierView(s.getId(), s.getName()))
        .toList();
  }

  @GetMapping("/regions")
  public Region[] regions() {
    return Region.values();
  }
}
