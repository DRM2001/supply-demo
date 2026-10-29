package dev.daniel.supply.web;

import dev.daniel.supply.offer.OfferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "adminToken")
@RestController
@RequestMapping("/api/v1/admin/offers")
@Validated
public class OfferController {
  private final OfferService service;

  public OfferController(OfferService service) {
    this.service = service;
  }

  @GetMapping("/{id}")
  public OfferService.View get(@PathVariable @Min(1) long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<OfferService.View> create(@RequestBody @Valid OfferService.Create request) {
    var result = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/admin/offers/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public OfferService.View update(
      @PathVariable @Min(1) long id, @RequestBody @Valid OfferService.Update request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable @Min(1) long id, @RequestParam @Min(0) long version) {
    service.delete(id, version);
    return ResponseEntity.noContent().build();
  }
}
