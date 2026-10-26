package com.example.bookstore.config;

import com.example.bookstore.domain.*;
import com.example.bookstore.domain.security.*;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.service.UserService;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bookstore.seed-demo", havingValue = "true")
public class DemoData implements CommandLineRunner {
  private final UserService users;
  private final BookRepository books;

  @Value("${bookstore.demo-student-password}")
  private String studentPassword;

  @Value("${bookstore.demo-admin-password}")
  private String adminPassword;

  public DemoData(UserService users, BookRepository books) {
    this.users = users;
    this.books = books;
  }

  public void run(String... args) {
    account("student", studentPassword, "ROLE_USER", 1);
    account("admin", adminPassword, "ROLE_ADMIN", 2);
    if (books.count() == 0) {
      book("Introduction to Algorithms", "Thomas H. Cormen", "9780262046305", 42, 12, 4);
      book("Clean Code", "Robert C. Martin", "9780132350884", 25, 8, 3);
      book("Designing Data-Intensive Applications", "Martin Kleppmann", "9781449373320", 35, 10, 2);
    }
  }

  private void account(String name, String password, String roleName, int roleId) {
    if (users.findByUsername(name) != null) return;
    User user = new User();
    user.setUsername(name);
    user.setEmail(name + "@example.test");
    user.setFirstName(name);
    user.setPassword(SecurityUtility.passwordEncoder().encode(password));
    Role role = new Role();
    role.setRoleId(roleId);
    role.setName(roleName);
    users.createUser(user, Set.of(new UserRole(user, role)));
  }

  private void book(
      String title, String author, String isbn, double price, double rent, int stock) {
    Book b = new Book();
    b.setTitle(title);
    b.setAuthor(author);
    b.setIsbn(isbn);
    b.setOurPrice(price);
    b.setRentalPrice(rent);
    b.setListPrice(price);
    b.setInStockNumber(stock);
    b.setCategory("Computer Science");
    b.setDescription("Used textbook. Local demo inventory; campus pickup.");
    books.save(b);
  }
}
