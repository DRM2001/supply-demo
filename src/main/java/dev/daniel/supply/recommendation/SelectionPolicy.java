package dev.daniel.supply.recommendation;

import dev.daniel.supply.domain.Strategy;
import java.math.BigDecimal;
import java.util.*;

/** Regla pura: precio/plazo y desempate estable por proveedor e ID de oferta. */
public final class SelectionPolicy {
  private SelectionPolicy() {}

  public record Candidate(
      long offerId, long supplierId, String supplier, BigDecimal unitPrice, int leadDays) {}

  public static Optional<Candidate> select(List<Candidate> candidates, Strategy strategy) {
    Comparator<Candidate> byPrice = Comparator.comparing(Candidate::unitPrice);
    Comparator<Candidate> byDays = Comparator.comparingInt(Candidate::leadDays);
    Comparator<Candidate> order =
        strategy == Strategy.CHEAPEST
            ? byPrice.thenComparing(byDays)
            : byDays.thenComparing(byPrice);
    return candidates.stream()
        .min(order.thenComparingLong(Candidate::supplierId).thenComparingLong(Candidate::offerId));
  }
}
