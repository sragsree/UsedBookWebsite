package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.Test;

class PriceServiceTests {
  private final PriceService prices = new PriceService("", new ObjectMapper());

  String html(String isbn, String price, String currency) {
    return "<script type='application/ld+json'>{\"@type\":\"Book\",\"isbn\":\""
        + isbn
        + "\",\"offers\":{\"price\":\""
        + price
        + "\",\"priceCurrency\":\""
        + currency
        + "\"}}</script>";
  }

  @Test
  void parsesOnlyMatchingIsbnUsdPositiveOffer() throws Exception {
    assertThat(prices.extract(html("9780132350884", "12.99", "USD"), "9780132350884"))
        .isEqualByComparingTo("12.99");
    assertThatThrownBy(() -> prices.extract(html("9780132350884", "12.99", "EUR"), "9780132350884"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> prices.extract(html("9780132350884", "-5", "USD"), "9780132350884"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> prices.extract(html("9780132350884", "12.99", "USD"), "9780262046305"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void malformedOrUnrelatedPagesDoNotInventPrices() {
    assertThatThrownBy(() -> prices.extract("<h1>$42.00</h1>", "9780132350884"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void blocksPrivateNonHttpsAndCredentialUrls() {
    for (String url :
        new String[] {
          "http://example.com/book",
          "https://127.0.0.1/book",
          "https://[fc00::1]/book",
          "https://100.64.0.1/book",
          "https://[::ffff:127.0.0.1]/book",
          "https://localhost/book",
          "https://user:secret@example.com/book",
          "https://example.com:8181/book"
        })
      assertThatThrownBy(() -> PriceService.validateTarget(URI.create(url)))
          .isInstanceOf(Exception.class);
  }

  @Test
  void unavailableConfiguredProviderIsReportedWithoutSampleFallback() {
    PriceService service = new PriceService("http://127.0.0.1/{isbn}", new ObjectMapper());
    var quote = service.quote("9780132350884");
    assertThat(quote.mode()).isEqualTo("LIVE");
    assertThat(quote.suggestedPrice()).isNull();
    assertThat(quote.sources()).allMatch(s -> s.price() == null);
  }
}
