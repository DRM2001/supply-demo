package dev.daniel.supply.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ErrorAdvice {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> api(ApiException ex) {
    return problem(ex.status(), ex.getMessage());
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ProblemDetail> invalid(Exception ex) {
    return problem(
        HttpStatus.BAD_REQUEST, "Parámetros o JSON inválidos. Consulta el contrato OpenAPI.");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
    var detail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "El cuerpo contiene campos inválidos");
    detail.setProperty(
        "errors",
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .distinct()
            .toList());
    return ResponseEntity.badRequest().body(detail);
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class
  })
  ResponseEntity<ProblemDetail> conflict(Exception ex) {
    return problem(
        HttpStatus.CONFLICT, "Conflicto: oferta duplicada o modificada por otra operación");
  }

  private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
  }
}
