package dev.daniel.supply;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.daniel.supply.catalog.*;
import dev.daniel.supply.domain.*;
import dev.daniel.supply.offer.*;
import dev.daniel.supply.recommendation.RecommendationService;
import dev.daniel.supply.web.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RecommendationServiceTest {
  @Test
  void computesExactDecimalTotalAndCalendarDateWithInjectedClock() {
    var products = mock(ProductRepository.class);
    var offers = mock(OfferRepository.class);
    var supplier = mock(Supplier.class);
    var offer = mock(Offer.class);
    when(products.existsById("DEMO-001")).thenReturn(true);
    when(supplier.getId()).thenReturn(1L);
    when(supplier.getName()).thenReturn("Ficticio");
    when(offer.getId()).thenReturn(10L);
    when(offer.getSupplier()).thenReturn(supplier);
    when(offer.getUnitPrice()).thenReturn(new BigDecimal("0.10"));
    when(offer.getLeadDays()).thenReturn(3);
    when(offers.findEligible("DEMO-001", Region.MADRID, 3)).thenReturn(List.of(offer));
    var service =
        new RecommendationService(
            products, offers, Clock.fixed(Instant.parse("2026-10-29T23:00:00Z"), ZoneOffset.UTC));
    var result = service.recommend("DEMO-001", Region.MADRID, 3, Strategy.CHEAPEST);
    assertThat(result.totalPrice()).isEqualByComparingTo("0.30");
    assertThat(result.estimatedDelivery()).isEqualTo(LocalDate.of(2026, 11, 1));
  }

  @Test
  void unknownProductDoesNotQueryOffers() {
    var products = mock(ProductRepository.class);
    var offers = mock(OfferRepository.class);
    var service = new RecommendationService(products, offers, Clock.systemUTC());
    assertThatThrownBy(() -> service.recommend("MISSING", Region.MADRID, 1, Strategy.CHEAPEST))
        .isInstanceOfSatisfying(
            ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
    verifyNoInteractions(offers);
  }
}
