package com.example.bookstore;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.bookstore.config.SecurityUtility;
import com.example.bookstore.domain.*;
import com.example.bookstore.domain.security.*;
import com.example.bookstore.repository.*;
import com.example.bookstore.service.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:booktests;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class MarketplaceIntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired BookRepository books;
  @Autowired UserService users;
  @Autowired MarketplaceService market;
  String buyer;

  @BeforeEach
  void account() {
    buyer = "buyer" + UUID.randomUUID().toString().replace("-", "");
    create(buyer);
  }

  void create(String name) {
    User u = new User();
    u.setUsername(name);
    u.setEmail(name + "@example.test");
    u.setPassword(SecurityUtility.passwordEncoder().encode("test-password-long"));
    Role r = new Role();
    r.setRoleId(1);
    r.setName("ROLE_USER");
    users.createUser(u, Set.of(new UserRole(u, r)));
  }

  Book book(int stock) {
    Book b = new Book();
    b.setTitle("Test book");
    b.setAuthor("Test author");
    b.setIsbn("9780132350884");
    b.setOurPrice(19.99);
    b.setRentalPrice(4.50);
    b.setInStockNumber(stock);
    return books.save(b);
  }

  JsonNode add(String who, Long id, int qty, String mode) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/cart/add")
                    .with(user(who))
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(Map.of("bookId", id, "qty", qty, "mode", mode))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode checkout(String who) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/checkout/checkout")
                    .with(user(who))
                    .with(csrf())
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void accumulatedCartQuantityCannotExceedLineLimit() throws Exception {
    Book b = book(101);
    add(buyer, b.getId(), 100, "BUY");
    mvc.perform(
            post("/cart/add")
                .with(user(buyer))
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(Map.of("bookId", b.getId(), "qty", 1, "mode", "BUY"))))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/cart/getCartItemList").with(user(buyer)))
        .andExpect(jsonPath("$[0].qty").value(100));
    assertThat(checkout(buyer).path("cartItemList").get(0).path("qty").asInt()).isEqualTo(100);
  }

  @Test
  void inventoryAndBorrowerInformationAreAdminOnly() throws Exception {
    mvc.perform(get("/book/inventory")).andExpect(status().isUnauthorized());
    mvc.perform(get("/book/inventory").with(user(buyer))).andExpect(status().isForbidden());
    mvc.perform(get("/api/rentals").with(user(buyer))).andExpect(status().isForbidden());
    Book b = book(2);
    add(buyer, b.getId(), 1, "RENT");
    JsonNode order = checkout(buyer);
    mvc.perform(get("/api/rentals").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].borrower").value(buyer))
        .andExpect(jsonPath("$[0].orderId").value(order.path("id").asLong()))
        .andExpect(
            jsonPath("$[0].id").value(order.path("cartItemList").get(0).path("id").asLong()));
  }

  @Test
  void storeAndAdminPagesAreServed() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    mvc.perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("no money is charged")));
    mvc.perform(get("/admin.html")).andExpect(status().isOk());
    mvc.perform(get("/book/bookList"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").exists());
  }

  @Test
  void rejectsAnonymousStudentAndCsrfLessInventoryWrites() throws Exception {
    String body = "{\"title\":\"x\"}";
    mvc.perform(post("/book/add").with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/book/add")
                .with(user(buyer))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/book/add")
                .with(user("admin").roles("ADMIN"))
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanCreateUpdateArchiveInventory() throws Exception {
    Book b = book(4);
    b.setId(null);
    String payload = json.writeValueAsString(b);
    JsonNode created =
        json.readTree(
            mvc.perform(
                    post("/book/add")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    Book change = json.treeToValue(created, Book.class);
    change.setOurPrice(12.25);
    mvc.perform(
            post("/book/update")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(json.writeValueAsString(change)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ourPrice").value(12.25));
    mvc.perform(
            post("/book/remove")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(change.getId().toString()))
        .andExpect(status().isOk());
    mvc.perform(get("/book/" + change.getId())).andExpect(status().isNotFound());
  }

  @Test
  void registrationAndRealPasswordLoginPersistSessionWithoutLeakingHash() throws Exception {
    String name = "new" + UUID.randomUUID().toString().replace("-", "");
    mvc.perform(
            post("/user/newUser")
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "username",
                            name,
                            "email",
                            name + "@example.test",
                            "password",
                            "long-password-123"))))
        .andExpect(status().isOk());
    MvcResult login =
        mvc.perform(post("/token").with(httpBasic(name, "long-password-123")).with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    mvc.perform(get("/user/getCurrentUser").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(name))
        .andExpect(jsonPath("$.password").doesNotExist());
    mvc.perform(post("/token").with(httpBasic(name, "wrong")).with(csrf()))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void malformedRegistrationIsRejected() throws Exception {
    for (String body : List.of("{}", "{\"username\":null}", "{\"username\":\"x\"}"))
      mvc.perform(post("/user/newUser").with(csrf()).contentType("application/json").content(body))
          .andExpect(status().isBadRequest());
  }

  @Test
  void purchaseAccumulatesCorrectSubtotalAndClearsCart() throws Exception {
    Book b = book(5);
    add(buyer, b.getId(), 1, "BUY");
    JsonNode line = add(buyer, b.getId(), 2, "BUY");
    assertThat(line.path("qty").asInt()).isEqualTo(3);
    assertThat(line.path("subtotal").decimalValue()).isEqualByComparingTo("59.97");
    JsonNode order = checkout(buyer);
    assertThat(order.path("orderTotal").decimalValue()).isEqualByComparingTo("59.97");
    assertThat(order.path("orderStatus").asText()).isEqualTo("DEMO_CONFIRMED_NO_PAYMENT");
    assertThat(books.findById(b.getId()).orElseThrow().getInStockNumber()).isEqualTo(2);
    mvc.perform(get("/cart/getCartItemList").with(user(buyer)))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/order/getOrderList").with(user(buyer)))
        .andExpect(jsonPath("$[0].cartItemList[0].unitPrice").value(19.99));
  }

  @Test
  void rentalsHaveDueDatesAndReturnRestoresStockOnce() throws Exception {
    Book b = book(2);
    add(buyer, b.getId(), 1, "RENT");
    JsonNode order = checkout(buyer);
    JsonNode line = order.path("cartItemList").get(0);
    assertThat(line.path("dueDate").asText())
        .isEqualTo(java.time.LocalDate.now().plusDays(30).toString());
    long id = line.path("id").asLong();
    mvc.perform(post("/api/rentals/" + id + "/return").with(user(buyer)).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/rentals/" + id + "/return").with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returnedDate").exists());
    mvc.perform(
            post("/api/rentals/" + id + "/return").with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isConflict());
    assertThat(books.findById(b.getId()).orElseThrow().getInStockNumber()).isEqualTo(2);
  }

  @Test
  void cartCannotBeChangedByAnotherAccount() throws Exception {
    Book b = book(4);
    long id = add(buyer, b.getId(), 1, "BUY").path("id").asLong();
    mvc.perform(
            post("/cart/removeItem")
                .with(user("student"))
                .with(csrf())
                .contentType("application/json")
                .content(Long.toString(id)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/cart/updateCartItem")
                .with(user("student"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"cartItemId\":" + id + ",\"qty\":2}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/order/getOrderList").with(user(buyer)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void rejectsNegativeQuantityOversizedCartAndEmptyCheckout() throws Exception {
    Book b = book(2);
    mvc.perform(
            post("/cart/add")
                .with(user(buyer))
                .with(csrf())
                .contentType("application/json")
                .content("{\"bookId\":" + b.getId() + ",\"qty\":-1,\"mode\":\"BUY\"}"))
        .andExpect(status().isBadRequest());
    add(buyer, b.getId(), 2, "BUY");
    mvc.perform(
            post("/cart/add")
                .with(user(buyer))
                .with(csrf())
                .contentType("application/json")
                .content("{\"bookId\":" + b.getId() + ",\"qty\":1,\"mode\":\"RENT\"}"))
        .andExpect(status().isBadRequest());
    checkout(buyer);
    mvc.perform(
            post("/checkout/checkout")
                .with(user(buyer))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsCardDataRatherThanPretendingToCharge() throws Exception {
    mvc.perform(
            post("/checkout/checkout")
                .with(user(buyer))
                .with(csrf())
                .contentType("application/json")
                .content("{\"payment\":{\"cardNumber\":\"4111111111111111\"}}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/payment/add").with(user(buyer)).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void competingCheckoutsCannotOversell() throws Exception {
    Book b = book(1);
    String other = buyer + "other";
    create(other);
    market.add(buyer, b.getId(), 1, "BUY");
    market.add(other, b.getId(), 1, "BUY");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> results = new ArrayList<>();
      for (String name : List.of(buyer, other))
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    market.checkout(name);
                    return true;
                  } catch (org.springframework.web.server.ResponseStatusException e) {
                    assertThat(e.getStatusCode().value()).isEqualTo(409);
                    return false;
                  }
                }));
      start.countDown();
      int successes = 0;
      for (var result : results) if (result.get(15, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(1);
      assertThat(books.findById(b.getId()).orElseThrow().getInStockNumber()).isZero();
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void priceQuoteIsExplicitSampleAndNeverOverridesSavedPrice() throws Exception {
    Book b = book(2);
    mvc.perform(get("/api/prices/" + b.getIsbn()).with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("SAMPLE_HTML_NOT_LIVE"))
        .andExpect(jsonPath("$.suggestedPrice").value(25.0));
    assertThat(books.findById(b.getId()).orElseThrow().getOurPrice()).isEqualTo(19.99);
    mvc.perform(get("/api/prices/9780000000000").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestedPrice").isEmpty());
    mvc.perform(get("/api/prices/not-an-isbn").with(user("admin").roles("ADMIN")))
        .andExpect(status().isBadRequest());
  }
}
