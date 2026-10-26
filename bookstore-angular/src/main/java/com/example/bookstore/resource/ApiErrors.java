package com.example.bookstore.resource;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<Map<String, String>> invalid(RuntimeException e) {
    return ResponseEntity.badRequest().body(Map.of("message", "Invalid request values"));
  }

  @ExceptionHandler({
    org.springframework.dao.DataIntegrityViolationException.class,
    org.springframework.dao.OptimisticLockingFailureException.class
  })
  public ResponseEntity<Map<String, String>> conflict(RuntimeException e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Data changed or already exists. Refresh and retry."));
  }
}
