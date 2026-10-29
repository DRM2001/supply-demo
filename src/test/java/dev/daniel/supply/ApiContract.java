package dev.daniel.supply;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class ApiContract {
  static final String AUTH = "Bearer test-only-token-at-least-32-characters";
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void catalogIsPublicAndPaginated() throws Exception {
    mvc.perform(get("/api/v1/products").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void suppliersAndRegionsArePublic() throws Exception {
    mvc.perform(get("/api/v1/suppliers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
    mvc.perform(get("/api/v1/regions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("MADRID"));
  }

  @Test
  void stockFilterAndCheapestSelectionUseDatabase() throws Exception {
    mvc.perform(
            get("/api/v1/recommendations")
                .param("sku", "DEMO-001")
                .param("region", "MADRID")
                .param("quantity", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.supplierId").value(1))
        .andExpect(jsonPath("$.totalPrice").value(125.0));
  }

  @Test
  void exactStockBoundaryIsEligible() throws Exception {
    mvc.perform(
            get("/api/v1/recommendations")
                .param("sku", "DEMO-001")
                .param("region", "MADRID")
                .param("quantity", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.supplierId").value(3));
  }

  @Test
  void fastestUsesDifferentSupplier() throws Exception {
    mvc.perform(
            get("/api/v1/recommendations")
                .param("sku", "DEMO-001")
                .param("region", "MADRID")
                .param("quantity", "10")
                .param("strategy", "FASTEST"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.supplierId").value(2));
  }

  @Test
  void regionIsAnActualFilter() throws Exception {
    mvc.perform(
            get("/api/v1/recommendations").param("sku", "DEMO-001").param("region", "ANDALUCIA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unitPrice").value(13.0));
  }

  @Test
  void unknownProductIs404() throws Exception {
    mvc.perform(get("/api/v1/recommendations").param("sku", "MISSING").param("region", "MADRID"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  void unavailableStockIs422() throws Exception {
    mvc.perform(
            get("/api/v1/recommendations")
                .param("sku", "DEMO-001")
                .param("region", "MADRID")
                .param("quantity", "101"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void invalidQuantityAndRegionAre400() throws Exception {
    mvc.perform(
            get("/api/v1/recommendations")
                .param("sku", "DEMO-001")
                .param("region", "MADRID")
                .param("quantity", "0"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/recommendations").param("sku", "DEMO-001").param("region", "NOPE"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void oversizedPageIsRejected() throws Exception {
    mvc.perform(get("/api/v1/products").param("size", "101")).andExpect(status().isBadRequest());
  }

  @Test
  void noStockReservationOccurs() throws Exception {
    for (int i = 0; i < 2; i++)
      mvc.perform(
              get("/api/v1/recommendations")
                  .param("sku", "DEMO-001")
                  .param("region", "MADRID")
                  .param("quantity", "100"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.supplierId").value(1));
  }

  @Test
  void missingAndWrongAdminTokensAreRejected() throws Exception {
    mvc.perform(get("/api/v1/admin/offers/1"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"));
    mvc.perform(get("/api/v1/admin/offers/1").header("Authorization", "Bearer wrong"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void healthAndOpenApiArePublic() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.adminToken.scheme").value("bearer"));
  }

  @Test
  void corsAllowsOnlyPortfolioOrigin() throws Exception {
    mvc.perform(get("/api/v1/regions").header("Origin", "https://portfolio.example"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://portfolio.example"));
    mvc.perform(get("/api/v1/regions").header("Origin", "https://untrusted.example"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void adminCreateReadUpdateDeleteAndStaleVersion() throws Exception {
    String created =
        mvc.perform(
                post("/api/v1/admin/offers")
                    .header("Authorization", AUTH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"sku\":\"DEMO-003\",\"supplierId\":1,\"region\":\"MADRID\",\"unitPrice\":5.50,\"stock\":20,\"leadDays\":2}"))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = json.readTree(created).get("id").asLong();
    String path = "/api/v1/admin/offers/" + id;
    mvc.perform(get(path).header("Authorization", AUTH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(0));
    String update = "{\"version\":0,\"unitPrice\":6.00,\"stock\":10,\"leadDays\":1}";
    mvc.perform(
            put(path)
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(update))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));
    mvc.perform(
            put(path)
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(update))
        .andExpect(status().isConflict());
    mvc.perform(delete(path).param("version", "0").header("Authorization", AUTH))
        .andExpect(status().isConflict());
    mvc.perform(delete(path).param("version", "1").header("Authorization", AUTH))
        .andExpect(status().isNoContent());
    mvc.perform(get(path).header("Authorization", AUTH)).andExpect(status().isNotFound());
  }

  @Test
  void rejectsNegativePriceAndExtraPrecision() throws Exception {
    for (String price : new String[] {"-1", "1.234"})
      mvc.perform(
              post("/api/v1/admin/offers")
                  .header("Authorization", AUTH)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"sku\":\"DEMO-003\",\"supplierId\":1,\"region\":\"MADRID\",\"unitPrice\":"
                          + price
                          + ",\"stock\":20,\"leadDays\":2}"))
          .andExpect(status().isBadRequest());
  }

  @Test
  void duplicateOfferReturns409WithoutSqlDetails() throws Exception {
    mvc.perform(
            post("/api/v1/admin/offers")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"sku\":\"DEMO-001\",\"supplierId\":1,\"region\":\"MADRID\",\"unitPrice\":5.50,\"stock\":20,\"leadDays\":2}"))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value("Conflicto: oferta duplicada o modificada por otra operación"));
  }
}
