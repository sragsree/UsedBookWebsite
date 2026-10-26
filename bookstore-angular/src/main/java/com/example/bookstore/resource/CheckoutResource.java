/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.resource;

import com.example.bookstore.domain.Order;
import com.example.bookstore.service.MarketplaceService;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/checkout")
public class CheckoutResource {
  private final MarketplaceService market;

  public CheckoutResource(MarketplaceService market) {
    this.market = market;
  }

  @PostMapping("/checkout")
  public Order checkout(@RequestBody(required = false) Map<String, Object> body, Principal p) {
    if (body != null && !body.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Demo checkout accepts no payment or card data; use an empty object");
    return market.checkout(p.getName());
  }
}
