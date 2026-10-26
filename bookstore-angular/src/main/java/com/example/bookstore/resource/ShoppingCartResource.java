/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.resource;

import com.example.bookstore.domain.CartItem;
import com.example.bookstore.service.MarketplaceService;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class ShoppingCartResource {
  private final MarketplaceService market;

  public ShoppingCartResource(MarketplaceService market) {
    this.market = market;
  }

  public record Add(Long bookId, int qty, String mode) {}

  public record Update(Long cartItemId, int qty) {}

  @PostMapping("/add")
  public CartItem add(@RequestBody Add body, Principal p) {
    return market.add(
        p.getName(), body.bookId(), body.qty(), body.mode() == null ? "BUY" : body.mode());
  }

  @GetMapping("/getCartItemList")
  public List<CartItem> cart(Principal p) {
    return market.cart(p.getName());
  }

  @PostMapping("/removeItem")
  public Map<String, String> remove(@RequestBody Long id, Principal p) {
    market.remove(p.getName(), id);
    return Map.of("message", "Removed");
  }

  @PostMapping("/updateCartItem")
  public Map<String, String> update(@RequestBody Update body, Principal p) {
    market.update(p.getName(), body.cartItemId(), body.qty());
    return Map.of("message", "Updated");
  }
}
