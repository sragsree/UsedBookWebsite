package com.example.bookstore.resource;

import com.example.bookstore.service.PriceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prices")
public class PriceResource {
  private final PriceService prices;

  public PriceResource(PriceService prices) {
    this.prices = prices;
  }

  @GetMapping("/{isbn}")
  public PriceService.Quote quote(@PathVariable String isbn) {
    return prices.quote(isbn);
  }
}
