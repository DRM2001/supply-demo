package dev.daniel.supply.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ObjectMapper mapper,
      @Value("${app.admin-token}") String token,
      @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource")
          CorsConfigurationSource corsSource)
      throws Exception {
    if (token.length() < 32)
      throw new IllegalArgumentException("ADMIN_TOKEN debe tener al menos 32 caracteres");
    byte[] expected =
        MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    var filter =
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain chain)
              throws ServletException, IOException {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
              try {
                byte[] received =
                    MessageDigest.getInstance("SHA-256")
                        .digest(header.substring(7).getBytes(StandardCharsets.UTF_8));
                if (MessageDigest.isEqual(expected, received))
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          UsernamePasswordAuthenticationToken.authenticated(
                              "portfolio-admin",
                              null,
                              AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
              } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException(ex);
              }
            }
            chain.doFilter(request, response);
          }
        };
    return http.csrf(
            csrf -> csrf.disable()) // Sin cookies ni credenciales automáticas del navegador: token
        // Bearer explícito.
        .cors(cors -> cors.configurationSource(corsSource))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/products",
                        "/api/v1/suppliers",
                        "/api/v1/regions",
                        "/api/v1/recommendations",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) ->
                            writeProblem(mapper, res, 401, "Token Bearer requerido o inválido"))
                    .accessDeniedHandler(
                        (req, res, ex) -> writeProblem(mapper, res, 403, "Acceso denegado")))
        .addFilterBefore(filter, AnonymousAuthenticationFilter.class)
        .build();
  }

  private static void writeProblem(
      ObjectMapper mapper, HttpServletResponse res, int code, String detail) throws IOException {
    res.setStatus(code);
    res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    if (code == 401) res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    mapper.writeValue(
        res.getOutputStream(), ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(code), detail));
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origin) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(origin));
    config.setAllowedMethods(List.of("GET"));
    config.setAllowedHeaders(List.of("Accept", "Content-Type"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/v1/**", config);
    return source;
  }
}
