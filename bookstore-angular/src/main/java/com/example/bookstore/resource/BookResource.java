package com.example.bookstore.resource;

import com.example.bookstore.domain.Book;
import com.example.bookstore.repository.BookRepository;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/book")
public class BookResource {
  private final BookRepository books;

  @Value("${bookstore.upload-dir}")
  private String uploadDir;

  public BookResource(BookRepository books) {
    this.books = books;
  }

  @GetMapping("/bookList")
  public List<Book> catalog() {
    return ((List<Book>) books.findAll()).stream().filter(Book::isActive).toList();
  }

  @GetMapping("/inventory")
  public Iterable<Book> inventory() {
    return books.findAll();
  }

  @GetMapping("/{id}")
  public Book get(@PathVariable Long id) {
    Book b =
        books.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!b.isActive()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return b;
  }

  @PostMapping("/searchBook")
  public List<Book> search(@RequestBody String keyword) {
    String q = keyword.replace("\"", "").trim().toLowerCase(Locale.ROOT);
    return catalog().stream()
        .filter(
            b ->
                (b.getTitle() + " " + b.getAuthor() + " " + b.getIsbn())
                    .toLowerCase(Locale.ROOT)
                    .contains(q))
        .toList();
  }

  private void validate(Book b) {
    if (b.getTitle() == null
        || b.getTitle().isBlank()
        || b.getTitle().length() > 200
        || b.getAuthor() == null
        || b.getAuthor().isBlank()
        || b.getAuthor().length() > 200
        || b.getIsbn() == null
        || !b.getIsbn().matches("(?:[0-9]{13}|[0-9]{9}[0-9X])")
        || b.getInStockNumber() < 0
        || !Double.isFinite(b.getOurPrice())
        || b.getOurPrice() <= 0
        || b.getOurPrice() > 10000
        || !Double.isFinite(b.getRentalPrice())
        || b.getRentalPrice() <= 0
        || b.getRentalPrice() > 10000)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Title, author, ISBN-10/13, positive sale/rental prices and nonnegative stock required");
  }

  @PostMapping("/add")
  public Book add(@RequestBody Book b) {
    validate(b);
    b.setId(null);
    return books.save(b);
  }

  @PostMapping("/update")
  @Transactional
  public Book update(@RequestBody Book input) {
    validate(input);
    Book b =
        books
            .lockById(input.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    b.setTitle(input.getTitle());
    b.setAuthor(input.getAuthor());
    b.setIsbn(input.getIsbn());
    b.setDescription(input.getDescription());
    b.setCategory(input.getCategory());
    b.setOurPrice(input.getOurPrice());
    b.setRentalPrice(input.getRentalPrice());
    b.setInStockNumber(input.getInStockNumber());
    b.setActive(input.isActive());
    return books.save(b);
  }

  @PostMapping("/remove")
  @Transactional
  public Map<String, String> remove(@RequestBody Long id) {
    Book b =
        books.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    b.setActive(false);
    books.save(b);
    return Map.of("message", "Archived; past orders and rental records retained");
  }

  @PostMapping({"/add/image", "/update/image"})
  public Map<String, String> image(@RequestParam Long id, @RequestParam("file") MultipartFile file)
      throws java.io.IOException {
    if (!books.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    byte[] bytes = file.getBytes();
    if (bytes.length > 2 * 1024 * 1024
        || bytes.length < 8
        || bytes[0] != (byte) 137
        || bytes[1] != 80
        || bytes[2] != 78
        || bytes[3] != 71
        || javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes)) == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid PNG under 2 MB required");
    Path dir = Path.of(uploadDir);
    Files.createDirectories(dir);
    Files.write(dir.resolve(id + ".png"), bytes);
    return Map.of("url", "/image/uploads/" + id + ".png");
  }
}
