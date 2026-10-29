package dev.daniel.supply.recommendation;

import dev.daniel.supply.catalog.ProductRepository;
import dev.daniel.supply.domain.*;
import dev.daniel.supply.offer.OfferRepository;
import dev.daniel.supply.web.ApiException;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
  private final ProductRepository products;
  private final OfferRepository offers;
  private final Clock clock;

  public RecommendationService(ProductRepository products, OfferRepository offers, Clock clock) {
    this.products = products;
    this.offers = offers;
    this.clock = clock;
  }

  public record Result(
      String sku,
      Region region,
      int quantity,
      Strategy strategy,
      long offerId,
      long supplierId,
      String supplier,
      BigDecimal unitPrice,
      BigDecimal totalPrice,
      String currency,
      int leadDays,
      LocalDate estimatedDelivery) {}

  @Transactional(readOnly = true)
  public Result recommend(String sku, Region region, int quantity, Strategy strategy) {
    if (!products.existsById(sku))
      throw new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado");
    var candidates =
        offers.findEligible(sku, region, quantity).stream()
            .map(
                o ->
                    new SelectionPolicy.Candidate(
                        o.getId(),
                        o.getSupplier().getId(),
                        o.getSupplier().getName(),
                        o.getUnitPrice(),
                        o.getLeadDays()))
            .toList();
    var best =
        SelectionPolicy.select(candidates, strategy)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "No hay un proveedor con stock suficiente en esa región"));
    return new Result(
        sku,
        region,
        quantity,
        strategy,
        best.offerId(),
        best.supplierId(),
        best.supplier(),
        best.unitPrice(),
        best.unitPrice().multiply(BigDecimal.valueOf(quantity)),
        "EUR",
        best.leadDays(),
        LocalDate.now(clock).plusDays(best.leadDays()));
  }
}
