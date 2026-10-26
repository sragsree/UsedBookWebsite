# Verification record

Local verification: 2026-10-25 (America/Los_Angeles).

## Repeatable commands

```bash
./run.sh build
node --check bookstore-angular/src/main/resources/static/app.js
```

The clean build compiles Java 17 source, runs the isolated H2/MockMvc and HTML parsing tests, and packages an executable Spring Boot JAR. Generated dependency trees, build outputs and previous embedded credential configuration are no longer tracked.

## Automated evidence

- Store/admin static resources are served by the application.
- Student registration rejects missing/null fields; password authentication creates a session; logout invalidates it; account JSON excludes the password hash.
- Anonymous and student inventory reads/writes are rejected. CSRF-less writes are rejected, including admin writes.
- Admin create/edit/archive persists inventory and manual prices.
- Repeated additions calculate cumulative totals correctly and enforce the merged per-line quantity limit.
- Cart ownership checks reject changes from a different user.
- Checkout stores orders, snapshots unit prices, updates stock and clears the cart atomically.
- Two concurrent buyers cannot buy the final copy twice; the losing transaction receives a stock conflict.
- Rentals store a 30-day due date; admin-only desk records include borrower username, order ID and line ID; one return restores stock and a repeated return is rejected.
- Empty checkouts, negative/overlarge quantities and card-bearing checkout requests are rejected.
- Price extraction requires matching ISBN, positive bounded USD offers and suitable structured HTML. Invalid sources produce explicit failures, never invented prices.
- Sample HTML is labeled as sample; quote requests do not silently change saved inventory prices.
- HTTPS/source validation rejects loopback, common private, IPv6 ULA/mapped-private and IPv4 CGNAT destinations; no redirects are followed.

## Boundaries

Tests do not call production services, send emails, charge cards or scrape real retailers. Live retailer availability and MySQL operation are not verified. The supported UI is the Spring-served store and admin portal; retained Angular 5 sources are historical and not claimed as compatible. Repository source ownership notices are preserved.

## Independent browser walkthrough

The parent agent exercised the packaged application in the browser on 2026-10-25:

- A purchase plus rental checked out for **$50**, with the order visible afterward.
- The order survived a server restart using the same file database.
- The administrator identified the rental by borrower/order/line, recorded physical return, and restored inventory once.
- Price research visibly labeled the **sample** offers of **$24** and **$26**, producing the expected **$25** median.
- The administrator edited the suggestion to **$26**, saved it, and confirmed that the price persisted.

These observations verify the supported browser workflow and local persistence. They do not validate real payment processing, live retailer pricing or MySQL deployment.

## Permanent-clone release verification

On 2026-10-25, the saved clone passed the documented `./run.sh build` command with JDK 17: **18 automated tests, zero failures/errors**, followed by executable JAR packaging. The documented `./run.sh` entry point then started the saved clone on loopback port 8182.

The new `scripts/http_smoke.py` ran against that real HTTP server and passed **26 checks**. It exercised page serving, real cookie sessions and logout, registration, anonymous/student authorization, CSRF rejection, mixed buy/rent checkout for **$24.49**, order history, identified admin returns, duplicate-return rejection, sample-price labeling, explicit admin price overrides and archiving. It created only fictional smoke data and left the seeded books unchanged. CI now runs this packaged-application smoke after the clean build. The CI startup/readiness/smoke sequence was also replayed locally with an isolated file database and passed the same 26 checks, then shut its server down through the cleanup trap.

JavaScript syntax, shell syntax and staged Git whitespace checks were also checked before commit. These local results do not imply that a hosted CI run, live retailer service, MySQL deployment or payment gateway was tested.
