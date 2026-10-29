package dev.daniel.supply;

import static org.assertj.core.api.Assertions.*;

import dev.daniel.supply.domain.Strategy;
import dev.daniel.supply.recommendation.SelectionPolicy;
import dev.daniel.supply.recommendation.SelectionPolicy.Candidate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SelectionPolicyTest {
  Candidate c(long id, long supplier, String price, int days) {
    return new Candidate(id, supplier, "Demo", new BigDecimal(price), days);
  }

  @Test
  void cheapestPrioritizesPriceOverSpeed() {
    var cheap = c(1, 1, "10.10", 4);
    assertThat(SelectionPolicy.select(List.of(c(2, 2, "11.00", 1), cheap), Strategy.CHEAPEST))
        .contains(cheap);
  }

  @Test
  void fastestPrioritizesDaysOverPrice() {
    var fast = c(2, 2, "11.00", 1);
    assertThat(SelectionPolicy.select(List.of(c(1, 1, "10.10", 4), fast), Strategy.FASTEST))
        .contains(fast);
  }

  @Test
  void equalPriceUsesShorterLeadTime() {
    var fast = c(2, 2, "10.00", 1);
    assertThat(SelectionPolicy.select(List.of(c(1, 1, "10.00", 3), fast), Strategy.CHEAPEST))
        .contains(fast);
  }

  @Test
  void equalLeadTimeUsesLowerPrice() {
    var cheap = c(2, 2, "9.99", 1);
    assertThat(SelectionPolicy.select(List.of(c(1, 1, "10.00", 1), cheap), Strategy.FASTEST))
        .contains(cheap);
  }

  @Test
  void tiesUseStableSupplierIdRatherThanInputOrder() {
    var first = c(2, 1, "10.0", 1);
    assertThat(SelectionPolicy.select(List.of(c(1, 2, "10.00", 1), first), Strategy.CHEAPEST))
        .contains(first);
  }

  @Test
  void emptyOffersHaveNoWinner() {
    assertThat(SelectionPolicy.select(List.of(), Strategy.CHEAPEST)).isEmpty();
  }
}
