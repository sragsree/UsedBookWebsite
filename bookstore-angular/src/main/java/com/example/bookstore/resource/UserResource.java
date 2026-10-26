/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.resource;

import com.example.bookstore.config.SecurityUtility;
import com.example.bookstore.domain.User;
import com.example.bookstore.domain.security.*;
import com.example.bookstore.service.UserService;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/user")
public class UserResource {
  private final UserService users;

  public UserResource(UserService users) {
    this.users = users;
  }

  @PostMapping("/newUser")
  public Map<String, String> register(@RequestBody Map<String, String> body) {
    if (body == null || body.values().stream().anyMatch(Objects::isNull))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account fields must be text");
    String username = body.getOrDefault("username", "").trim(),
        email = body.getOrDefault("email", "").trim(),
        password = body.getOrDefault("password", "");
    if (!username.matches("[A-Za-z0-9_-]{3,40}")
        || !email.matches("[^@ ]+@[^@ ]+[.][^@ ]+")
        || password.length() < 12
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Username 3–40 letters/digits, valid email and password 12–72 characters required");
    if (users.findByUsername(username) != null || users.findByEmail(email) != null)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
    User u = new User();
    u.setUsername(username);
    u.setEmail(email);
    u.setPassword(SecurityUtility.passwordEncoder().encode(password));
    Role role = new Role();
    role.setRoleId(1);
    role.setName("ROLE_USER");
    users.createUser(u, Set.of(new UserRole(u, role)));
    return Map.of("message", "Account created. Sign in with your password.");
  }

  @GetMapping("/getCurrentUser")
  public Map<String, Object> current(Principal principal) {
    User u = users.findByUsername(principal.getName());
    return Map.of(
        "username",
        u.getUsername(),
        "email",
        u.getEmail(),
        "admin",
        u.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }
}
