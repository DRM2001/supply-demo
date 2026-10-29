package dev.daniel.supply;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude =
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
            .class)
public class SupplyApplication {
  public static void main(String[] args) {
    SpringApplication.run(SupplyApplication.class, args);
  }
}
