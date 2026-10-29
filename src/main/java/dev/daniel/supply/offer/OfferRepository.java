package dev.daniel.supply.offer;

import dev.daniel.supply.domain.Region;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OfferRepository extends JpaRepository<Offer, Long> {
  @Query(
      "select o from Offer o join fetch o.supplier where o.product.sku=:sku and o.region=:region"
          + " and o.stock>=:quantity")
  List<Offer> findEligible(
      @Param("sku") String sku, @Param("region") Region region, @Param("quantity") int quantity);
}
