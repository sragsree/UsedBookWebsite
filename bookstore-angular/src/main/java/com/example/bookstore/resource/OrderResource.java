/*
 * Copyright (C) 2018 by Teradata Corporation.
 * All Rights Reserved.
 * TERADATA CORPORATION CONFIDENTIAL AND TRADE SECRET
 */
package com.example.bookstore.resource;

import com.example.bookstore.domain.*;
import com.example.bookstore.service.MarketplaceService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderResource {
  private final MarketplaceService market;

  public OrderResource(MarketplaceService market) {
    this.market = market;
  }

  @GetMapping("/order/getOrderList")
  public List<Order> orders(Principal p) {
    return market.orders(p.getName());
  }

  public record RentalInfo(
      Long id,
      Long orderId,
      String borrower,
      Book book,
      int qty,
      java.time.LocalDate dueDate,
      java.time.LocalDate returnedDate) {}

  @GetMapping("/api/rentals")
  public List<RentalInfo> rentals() {
    return market.rentals().stream()
        .map(
            line ->
                new RentalInfo(
                    line.getId(),
                    line.getOrder().getId(),
                    line.getOrder().getUser().getUsername(),
                    line.getBook(),
                    line.getQty(),
                    line.getDueDate(),
                    line.getReturnedDate()))
        .toList();
  }

  @PostMapping("/api/rentals/{id}/return")
  public CartItem returned(@PathVariable Long id) {
    return market.returnRental(id);
  }
}
