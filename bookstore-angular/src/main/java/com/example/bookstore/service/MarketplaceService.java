package com.example.bookstore.service;

import com.example.bookstore.domain.*;
import com.example.bookstore.repository.*;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketplaceService {
  private final UserRepository users;
  private final BookRepository books;
  private final CartItemRepository items;
  private final ShoppingCartRepository carts;
  private final OrderRepository orders;

  public MarketplaceService(
      UserRepository users,
      BookRepository books,
      CartItemRepository items,
      ShoppingCartRepository carts,
      OrderRepository orders) {
    this.users = users;
    this.books = books;
    this.items = items;
    this.carts = carts;
    this.orders = orders;
  }

  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private User user(String name) {
    User u = users.lockByUsername(name);
    if (u == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return u;
  }

  private Book book(Long id) {
    try {
      return books
          .lockById(id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    } catch (org.springframework.dao.OptimisticLockingFailureException conflict) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Inventory changed; review your cart", conflict);
    }
  }

  private BigDecimal price(Book b, String mode) {
    return BigDecimal.valueOf(mode.equals("RENT") ? b.getRentalPrice() : b.getOurPrice())
        .setScale(2, RoundingMode.HALF_UP);
  }

  private void qty(int q) {
    if (q < 1 || q > 100) throw bad("Quantity must be between 1 and 100");
  }

  private void total(ShoppingCart cart) {
    BigDecimal sum = BigDecimal.ZERO;
    for (CartItem i : items.findByShoppingCart(cart)) {
      i.setUnitPrice(price(i.getBook(), i.getMode()));
      i.setSubtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQty())));
      items.save(i);
      sum = sum.add(i.getSubtotal());
    }
    cart.setGrandTotal(sum);
    carts.save(cart);
  }

  @Transactional
  public CartItem add(String name, Long id, int quantity, String mode) {
    qty(quantity);
    if (!Set.of("BUY", "RENT").contains(mode)) throw bad("Mode must be BUY or RENT");
    User u = user(name);
    Book b = book(id);
    if (!b.isActive()) throw bad("Book is unavailable");
    if (price(b, mode).signum() <= 0) throw bad("Book is not available for this transaction");
    List<CartItem> existing = items.findByShoppingCart(u.getShoppingCart());
    int requested =
        quantity
            + existing.stream()
                .filter(i -> i.getBook().getId().equals(id))
                .mapToInt(CartItem::getQty)
                .sum();
    if (requested > b.getInStockNumber()) throw bad("Not enough stock");
    CartItem item =
        existing.stream()
            .filter(i -> i.getBook().getId().equals(id) && i.getMode().equals(mode))
            .findFirst()
            .orElseGet(CartItem::new);
    item.setBook(b);
    item.setShoppingCart(u.getShoppingCart());
    item.setMode(mode);
    qty(item.getQty() + quantity);
    item.setQty(item.getQty() + quantity);
    items.save(item);
    total(u.getShoppingCart());
    return item;
  }

  private CartItem owned(String name, Long id) {
    User u = user(name);
    CartItem item =
        items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (item.getShoppingCart() == null
        || !item.getShoppingCart().getUser().getId().equals(u.getId()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "This cart item belongs to another account");
    return item;
  }

  @Transactional
  public void remove(String name, Long id) {
    CartItem i = owned(name, id);
    ShoppingCart c = i.getShoppingCart();
    items.delete(i);
    total(c);
  }

  @Transactional
  public void update(String name, Long id, int quantity) {
    qty(quantity);
    CartItem i = owned(name, id);
    Book b = book(i.getBook().getId());
    int other =
        items.findByShoppingCart(i.getShoppingCart()).stream()
            .filter(x -> x.getBook().getId().equals(b.getId()) && !x.getId().equals(id))
            .mapToInt(CartItem::getQty)
            .sum();
    if (other + quantity > b.getInStockNumber()) throw bad("Not enough stock");
    i.setQty(quantity);
    items.save(i);
    total(i.getShoppingCart());
  }

  @Transactional
  public List<CartItem> cart(String name) {
    User u = user(name);
    total(u.getShoppingCart());
    return items.findByShoppingCart(u.getShoppingCart());
  }

  @Transactional
  public Order checkout(String name) {
    User u = user(name);
    ShoppingCart cart = u.getShoppingCart();
    List<CartItem> lines = items.findByShoppingCart(cart);
    if (lines.isEmpty()) throw bad("Cart is empty");
    Map<Long, Integer> quantities = new TreeMap<>();
    for (CartItem line : lines) {
      qty(line.getQty());
      quantities.merge(line.getBook().getId(), line.getQty(), Integer::sum);
    }
    Map<Long, Book> locked = new HashMap<>();
    for (var entry : quantities.entrySet()) {
      Book b = book(entry.getKey());
      if (!b.isActive() || b.getInStockNumber() < entry.getValue())
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock changed; review your cart");
      b.setInStockNumber(b.getInStockNumber() - entry.getValue());
      locked.put(b.getId(), b);
      books.save(b);
    }
    Order order = new Order();
    order.setUser(u);
    order.setOrderDate(new Date());
    order.setOrderStatus("DEMO_CONFIRMED_NO_PAYMENT");
    order.setShippingMethod("CAMPUS_PICKUP");
    BigDecimal sum = BigDecimal.ZERO;
    for (CartItem line : lines) {
      line.setUnitPrice(price(locked.get(line.getBook().getId()), line.getMode()));
      if (line.getUnitPrice().signum() <= 0) throw bad("Price unavailable");
      line.setSubtotal(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty())));
      sum = sum.add(line.getSubtotal());
      if (line.getMode().equals("RENT")) line.setDueDate(LocalDate.now().plusDays(30));
      line.setShoppingCart(null);
      line.setOrder(order);
    }
    order.setOrderTotal(sum);
    order.setCartItemList(lines);
    orders.save(order);
    cart.setGrandTotal(BigDecimal.ZERO);
    carts.save(cart);
    return order;
  }

  @Transactional
  public CartItem returnRental(Long id) {
    CartItem line =
        items.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (line.getOrder() == null || !"RENT".equals(line.getMode()))
      throw bad("Not a completed rental");
    Book b = book(line.getBook().getId());
    // Lock the rental line before changing inventory, preventing duplicate returns.
    if (line.getReturnedDate() != null)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Rental already returned");
    b.setInStockNumber(b.getInStockNumber() + line.getQty());
    books.save(b);
    line.setReturnedDate(LocalDate.now());
    return items.save(line);
  }

  public List<Order> orders(String name) {
    return orders.findByUserUsernameOrderByIdDesc(name);
  }

  public List<CartItem> rentals() {
    return items.findByModeAndOrderIsNotNullOrderByIdDesc("RENT");
  }
}
