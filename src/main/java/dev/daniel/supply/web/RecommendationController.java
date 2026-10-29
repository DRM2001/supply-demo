package dev.daniel.supply.web;

import dev.daniel.supply.domain.*;
import dev.daniel.supply.recommendation.RecommendationService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@Validated
public class RecommendationController {
  private final RecommendationService service;

  public RecommendationController(RecommendationService service) {
    this.service = service;
  }

  @GetMapping
  public RecommendationService.Result recommend(
      @RequestParam @Pattern(regexp = "[A-Z0-9-]{1,40}") String sku,
      @RequestParam Region region,
      @RequestParam(defaultValue = "1") @Min(1) @Max(10000) int quantity,
      @RequestParam(defaultValue = "CHEAPEST") Strategy strategy) {
    return service.recommend(sku, region, quantity, strategy);
  }
}
