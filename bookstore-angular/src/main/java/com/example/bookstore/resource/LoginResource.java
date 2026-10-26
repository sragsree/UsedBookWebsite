/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.resource;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class LoginResource {
  @GetMapping("/api/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "header", token.getHeaderName());
  }

  @PostMapping("/token")
  public Map<String, String> login(Authentication authentication, HttpServletRequest request) {
    request.getSession(true);
    request.changeSessionId();
    request
        .getSession()
        .setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext());
    return Map.of("username", authentication.getName());
  }

  @GetMapping("/checkSession")
  public Map<String, String> session(Authentication auth) {
    return Map.of("username", auth.getName());
  }
}
