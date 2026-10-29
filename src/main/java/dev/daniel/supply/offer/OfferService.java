package dev.daniel.supply.offer;

import dev.daniel.supply.catalog.*;
import dev.daniel.supply.domain.Region;
import dev.daniel.supply.web.ApiException;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferService {
  private final OfferRepository offers;
  private final ProductRepository products;
  private final SupplierRepository suppliers;

  public OfferService(
      OfferRepository offers, ProductRepository products, SupplierRepository suppliers) {
    this.offers = offers;
    this.products = products;
    this.suppliers = suppliers;
  }

  public record Create(
      @NotBlank @Pattern(regexp = "[A-Z0-9-]{1,40}") String sku,
      @NotNull @Positive Long supplierId,
      @NotNull Region region,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
      @NotNull @Min(0) @Max(1000000) Integer stock,
      @NotNull @Min(0) @Max(365) Integer leadDays) {}

  public record Update(
      @NotNull @Min(0) Long version,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
      @NotNull @Min(0) @Max(1000000) Integer stock,
      @NotNull @Min(0) @Max(365) Integer leadDays) {}

  public record View(
      long id,
      long version,
      String sku,
      long supplierId,
      Region region,
      BigDecimal unitPrice,
      int stock,
      int leadDays) {}

  private View view(Offer o) {
    return new View(
        o.getId(),
        o.getVersion(),
        o.getProduct().getSku(),
        o.getSupplier().getId(),
        o.getRegion(),
        o.getUnitPrice(),
        o.getStock(),
        o.getLeadDays());
  }

  private Offer find(long id) {
    return offers
        .findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
  }

  @Transactional(readOnly = true)
  public View get(long id) {
    return view(find(id));
  }

  @Transactional
  public View create(Create request) {
    var product =
        products
            .findById(request.sku())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    var supplier =
        suppliers
            .findById(request.supplierId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
    return view(
        offers.saveAndFlush(
            new Offer(
                product,
                supplier,
                request.region(),
                request.unitPrice(),
                request.stock(),
                request.leadDays())));
  }

  @Transactional
  public View update(long id, Update request) {
    var offer = find(id);
    if (!offer.getVersion().equals(request.version()))
      throw new ApiException(HttpStatus.CONFLICT, "Versión obsoleta: vuelve a consultar la oferta");
    offer.revise(request.unitPrice(), request.stock(), request.leadDays());
    offers.flush();
    return view(offer);
  }

  @Transactional
  public void delete(long id, long version) {
    var offer = find(id);
    if (offer.getVersion() != version)
      throw new ApiException(HttpStatus.CONFLICT, "Versión obsoleta: vuelve a consultar la oferta");
    offers.delete(offer);
    offers.flush();
  }
}
