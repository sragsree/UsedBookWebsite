package com.example.bookstore.service;

import com.fasterxml.jackson.databind.*;
import java.math.*;
import java.net.*;
import java.time.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PriceService {
  public record Source(String source, BigDecimal price, String status) {}

  public record Quote(
      String isbn,
      String currency,
      String mode,
      BigDecimal suggestedPrice,
      List<Source> sources,
      Instant fetchedAt) {}

  private final String templates;
  private final ObjectMapper json;
  private final Map<String, Quote> cache = new java.util.concurrent.ConcurrentHashMap<>();

  public PriceService(
      @Value("${bookstore.price-url-template:}") String templates, ObjectMapper json) {
    this.templates = templates;
    this.json = json;
  }

  public Quote quote(String isbn) {
    if (!isbn.matches("(?:[0-9]{13}|[0-9]{9}[0-9X])"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ISBN-10/13 required");
    Quote cached = cache.get(isbn);
    if (cached != null && cached.fetchedAt().isAfter(Instant.now().minusSeconds(900)))
      return cached;
    List<Source> sources = new ArrayList<>();
    boolean demo = templates.isBlank();
    if (demo) {
      for (int n = 1; n <= 2; n++) {
        String source = "Bundled sample retailer " + n;
        try (var stream =
            new ClassPathResource("prices/" + isbn + "-" + n + ".html").getInputStream()) {
          sources.add(
              new Source(
                  source,
                  extract(
                      new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8),
                      isbn),
                  "sample HTML parsed"));
        } catch (Exception e) {
          sources.add(new Source(source, null, "No matching sample offer"));
        }
      }
    } else
      for (String template : templates.split(";")) {
        String url = template.trim().replace("{isbn}", isbn);
        String label = "Configured retailer";
        try {
          URI uri = URI.create(url);
          label = uri.getHost();
          validateTarget(uri);
          String html =
              Jsoup.connect(url)
                  .timeout(5000)
                  .maxBodySize(1024 * 1024)
                  .followRedirects(false)
                  .userAgent("UsedBookPortfolio/1.0")
                  .get()
                  .outerHtml();
          sources.add(new Source(label, extract(html, isbn), "live HTML parsed"));
        } catch (Exception e) {
          sources.add(new Source(label, null, "Unavailable or no matching USD offer"));
        }
      }
    List<BigDecimal> prices =
        sources.stream().map(Source::price).filter(Objects::nonNull).sorted().toList();
    BigDecimal suggested = null;
    if (!prices.isEmpty()) {
      int mid = prices.size() / 2;
      suggested =
          prices.size() % 2 == 0
              ? prices
                  .get(mid - 1)
                  .add(prices.get(mid))
                  .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
              : prices.get(mid);
    }
    Quote result =
        new Quote(
            isbn, "USD", demo ? "SAMPLE_HTML_NOT_LIVE" : "LIVE", suggested, sources, Instant.now());
    if (suggested != null) cache.put(isbn, result);
    return result;
  }

  static void validateTarget(URI uri) throws Exception {
    if (!"https".equals(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getPort() != -1 && uri.getPort() != 443)
      throw new IllegalArgumentException("Only configured public HTTPS retailers supported");
    for (InetAddress ip : InetAddress.getAllByName(uri.getHost()))
      if (ip.isAnyLocalAddress()
          || ip.isLoopbackAddress()
          || ip.isLinkLocalAddress()
          || ip.isSiteLocalAddress()
          || ip.isMulticastAddress()
          || nonGlobalAddress(ip)) throw new IllegalArgumentException("Private hosts unsupported");
  }

  static boolean nonGlobalAddress(InetAddress ip) {
    byte[] bytes = ip.getAddress();
    int first = Byte.toUnsignedInt(bytes[0]);
    if (bytes.length == 16) return (first & 0xfe) == 0xfc;
    int second = Byte.toUnsignedInt(bytes[1]);
    return first == 0
        || first >= 224
        || (first == 100 && (second & 0xc0) == 64)
        || (first == 192 && second == 0)
        || (first == 198 && (second == 18 || second == 19 || second == 51))
        || (first == 203 && second == 0);
  }

  public BigDecimal extract(String html, String isbn) throws Exception {
    for (var script : Jsoup.parse(html).select("script[type=application/ld+json]")) {
      try {
        BigDecimal price = product(json.readTree(script.data()), isbn);
        if (price != null) return price;
      } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
      }
    }
    throw new IllegalArgumentException("No matching ISBN and USD offer");
  }

  private BigDecimal product(JsonNode node, String isbn) {
    if (node.isArray()) {
      for (JsonNode child : node) {
        BigDecimal p = product(child, isbn);
        if (p != null) return p;
      }
    }
    if (node.isObject()) {
      String identity =
          node.path("isbn")
              .asText(node.path("gtin13").asText(node.path("gtin").asText("")))
              .replace("-", "")
              .replace(" ", "");
      if (identity.equals(isbn)) {
        BigDecimal p = offer(node.path("offers"));
        if (p != null) return p;
      }
      if (node.has("@graph")) return product(node.get("@graph"), isbn);
    }
    return null;
  }

  private BigDecimal offer(JsonNode n) {
    if (n.isArray()) {
      return java.util.stream.StreamSupport.stream(n.spliterator(), false)
          .map(this::offer)
          .filter(Objects::nonNull)
          .min(BigDecimal::compareTo)
          .orElse(null);
    }
    if (!n.isObject() || !"USD".equals(n.path("priceCurrency").asText())) return null;
    String availability = n.path("availability").asText("");
    if (availability.endsWith("OutOfStock") || availability.endsWith("Discontinued")) return null;
    try {
      BigDecimal value = new BigDecimal(n.path("price").asText(n.path("lowPrice").asText("")));
      return value.signum() > 0 && value.compareTo(BigDecimal.valueOf(10000)) <= 0
          ? value.setScale(2, RoundingMode.HALF_UP)
          : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
