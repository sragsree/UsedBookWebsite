# Campus Used Books — purchase and rental prototype

A working local prototype of a used-book marketplace for SDSU students: browse and search books, register/sign in, buy or rent, review orders, and return rented copies through an administrator. The admin portal manages inventory and prices. Java/Spring and JPA remain the application core.

**Demo transactions only.** Checkout persists orders and adjusts inventory; it never charges money. Do not enter payment card data. Pickup is manual. This is not a deployed SDSU service.

## Run from a fresh clone

Requirements: **JDK 17** and **Maven 3.9+**. First build downloads dependencies from public Maven Central. No Node, MySQL, Redis, SMTP, account credentials or external services are needed for the default demo.

```bash
java -version             # must report 17 or later
mvn -version              # must also use JDK 17 or later
./run.sh test
./run.sh
```

Open:

- Store: <http://127.0.0.1:8181/>
- Inventory / rental desk: <http://127.0.0.1:8181/admin.html>

| Account | Username | Password |
| --- | --- | --- |
| Student | `student` | `student-demo-only` |
| Administrator | `admin` | `admin-demo-only` |

These public sample accounts are created only when `BOOKSTORE_DEMO=true` (default). The server binds to loopback. Never expose the default demo accounts on a public interface. Override passwords **before the first start** with `BOOKSTORE_STUDENT_PASSWORD` / `BOOKSTORE_ADMIN_PASSWORD`. Setting `BOOKSTORE_DEMO=false` disables seeding; it does not remove previously created accounts. The file database lives in `.data/` relative to the repository root. Restarting preserves accounts, inventory, orders and returns. To start clean, stop the application, move `.data/` aside, then restart. Do not delete a database while the application is running.

On macOS, `run.sh` selects an installed JDK 17 if `JAVA_HOME` is unset. If your shell exports an older Java, set `JAVA_HOME` to your JDK 17 installation first. Optional settings appear in `.env.example`; the application does not automatically source `.env` files.

## Demonstrate the complete workflow

1. Sign in as student. Search by title, author or ISBN. Add a book to buy and another to rent. Quantities across both modes share the available-copy limit.
2. Update quantities or remove lines. Confirm the clearly labeled demo order. See the stored total, order lines, 30-day rental due date and reduced stock. No payment or outbound email occurs.
3. Sign out, open the admin portal, sign in as admin. Add or edit a book, stock, sale price or rental price. Archive a book to remove it from the store while keeping order history.
4. Look up one of the seeded ISBNs under Price research. The default uses **synthetic retailer HTML samples**, not current prices. The application extracts ISBN-matched USD offers and displays the median with source status. “Use suggestion in editor” requires an explicit Save before any price changes.
5. At the rental desk, match borrower username, order ID and rental line ID, then confirm physical return. The returned date persists and inventory increases once. A second return request is rejected.
6. Restart the server. Orders, inventory, accounts and returned rentals remain stored.

## Original promises and implementation evidence

The original README is preserved verbatim at [docs/README-original.md](docs/README-original.md). This table distinguishes implemented behavior from external dependencies.

| Original requirement | Current behavior | Evidence |
| --- | --- | --- |
| Full working student prototype | Same-origin store, admin portal, registration, authenticated sessions, search, carts, orders and rental return workflow | `MarketplaceIntegrationTests`; manual browser walkthrough |
| Purchase used books | Atomic persisted demo checkout, exact decimal totals, stock recheck, immutable line unit-price snapshot, order history | Purchase subtotal/stock test; competing-checkouts test |
| Rent used books | Separate rental price, rental cart mode, 30-day due date, stored return date, admin return restores stock exactly once | Rental and duplicate-return tests |
| Admin inventory portal | Role-checked create/edit/archive, stock and price edits; PNG upload endpoint | Admin CRUD and authorization tests |
| Determine prices through web scraping | Real HTML/JSON-LD extractor and configured HTTPS fetcher; matches ISBN and USD offers; 15-minute cache; source failures visible | `PriceServiceTests`; fixture quote integration test. Live retailer access is **not verified** and is not required for the demo. |
| Admin can manipulate suggested prices | Quotes do not silently overwrite inventory. Admin loads a suggestion, changes it if desired, then explicitly saves | Price persistence test; admin editor |
| MySQL / Redis / Tomcat setup | Embedded Tomcat + persistent H2 by default. MySQL connection settings remain configurable. Local servlet sessions replace the mandatory Redis dependency. | H2 integration tests; packaged application. MySQL is not exercised by the default test suite. |

The price scraper did not exist in the original implementation. The local demo now executes the same extraction code on transparent fixtures. It does **not** pretend to have scraped popular retailers or claim live market coverage without a configured, permitted source.

## Optional live price sources

Set `BOOKSTORE_PRICE_URL_TEMPLATE` to semicolon-separated **HTTPS** book-page templates containing `{isbn}`. Only operator-configured templates are fetched; API callers provide an ISBN, never a URL.

```bash
export BOOKSTORE_PRICE_URL_TEMPLATE='https://your-permitted-retailer.example/books/{isbn}'
./run.sh
```

A supported page must contain JSON-LD with the matching `isbn`, `gtin13` or `gtin` and `offers.price` (or `offers.lowPrice`) plus `priceCurrency: "USD"`. Offer arrays and `@graph` are supported. Out-of-stock/discontinued, negative, malformed, non-USD and mismatched-ISBN offers are rejected. Multiple successful sources produce a median suggestion.

Requests use a five-second timeout, one-megabyte response limit, no redirects, HTTPS-only targets and private-address checks. Errors produce “Unavailable or no matching USD offer”; the saved book price remains untouched and no sample fallback is presented as live. Retailer HTML, anti-bot rules, availability and permissions vary. Configure only sources you are permitted to fetch. This is an integration boundary, not a promise that arbitrary retailers will work. Network administrators should enforce outbound restrictions if running outside loopback; DNS validation is defense in depth, not an egress firewall.

## Architecture and existing capabilities

- `bookstore-angular/`: Spring Boot 3.5.7, Java 17, JPA/Hibernate, Spring Security; H2 or configurable MySQL.
- `bookstore-angular/src/main/resources/static/`: supported storefront and admin portal. Plain browser JavaScript, CSS and semantic HTML; no build-chain dependency.
- `MarketplaceService`: serialized per-user cart operations and row locks for inventory; checkout and returns commit atomically. Stock conflicts return HTTP 409 instead of overselling.
- `PriceService`: source-bounded fetch, ISBN/offer parsing, currency validation, median aggregation and cache.
- `store-front/` and `admin-portal/`: **historical Angular 5 clients**, retained for provenance. They are superseded by the same-origin portals and are not backward-compatible with the repaired session/CSRF/payment contracts. Their old `npm start` instructions are not the supported path.
- Historical saved-card/shipping/profile endpoints are disabled in the active security configuration. Passwords are hashed, account JSON omits hashes, state-changing requests need CSRF tokens, inventory writes need an admin role, and cart ownership is checked server-side. Card-bearing checkout requests are rejected rather than stored.

Image uploads: administrators can POST a PNG under 2 MB as multipart field `file` to `/book/add/image?id=<book-id>` or `/book/update/image?id=<book-id>`. Files are stored under `.data/uploads/`, outside the executable package. This API capability is retained; the current portal focuses on book text and does not include an upload widget.

## Verification

```bash
./run.sh test     # API/database workflows and scraper parsing / rejection tests
./run.sh build    # tests plus executable JAR
node --check bookstore-angular/src/main/resources/static/app.js  # optional JS syntax check
# With ./run.sh running in another terminal (Python 3 required):
python3 scripts/http_smoke.py
```

CI runs the clean build on Java 17, then starts the executable JAR and runs the HTTP smoke script. The smoke script accepts only a loopback HTTP URL, creates a fictional student account and book, and archives that book after verification. Existing seeded books are unchanged. Use `--base-url http://127.0.0.1:8182` if you start the server on a different port. Tests use isolated in-memory H2 and synthetic HTML. They cover purchasing, cumulative quantities/totals, rent/return/duplicate return, concurrent checkout, role/CSRF checks, cart ownership, real password login/logout, registration, inventory edits, rejected card data, source failures, URL restrictions, and missing/mismatched prices. The detailed verification record is in [docs/VERIFICATION.md](docs/VERIFICATION.md). Test reports are generated under `bookstore-angular/target/surefire-reports/`.

The app is a single-instance local prototype, not a production commerce service. No payment gateway, shipping provider, automatic late fees, email verification/password recovery, distributed session infrastructure or multi-instance load tests are claimed. Untrusted internet deployment needs further hardening and a real payment provider. Original source ownership notices remain; this update does not establish a new license or change provenance.
