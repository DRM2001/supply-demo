package dev.daniel.supply.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Supply Demo API")
                .version("1.0.0")
                .description(
                    "Portfolio backend. Datos ficticios. Consultas públicas; administración con"
                        + " token Bearer. Una recomendación no reserva stock."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "adminToken",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")));
  }
}
