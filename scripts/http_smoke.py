#!/usr/bin/env python3
"""Exercise the packaged localhost application through real HTTP and session cookies.

Creates one fictional account and one book, then archives that book. Seeded books
are unchanged. This script deliberately rejects non-loopback target URLs.
"""
import argparse
import base64
import http.cookiejar
import json
import os
import urllib.error
import urllib.parse
import urllib.request
import uuid
from decimal import Decimal


class Client:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )
        self.token = self.request("GET", "/api/csrf")[1]

    def request(self, method, path, data=None, *, headers=None, csrf=True):
        request_headers = dict(headers or {})
        if method != "GET" and csrf:
            request_headers[self.token["header"]] = self.token["token"]
        payload = None
        if data is not None:
            request_headers["Content-Type"] = "application/json"
            payload = json.dumps(data).encode()
        request = urllib.request.Request(
            self.base_url + path, data=payload, headers=request_headers, method=method
        )
        try:
            response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            raw = response.read()
            try:
                body = json.loads(raw) if raw else None
            except (ValueError, UnicodeDecodeError):
                body = raw.decode(errors="replace")
            return response.status, body

    def login(self, username, password):
        encoded = base64.b64encode(f"{username}:{password}".encode()).decode()
        status, _ = self.request(
            "POST", "/token", headers={"Authorization": f"Basic {encoded}"}
        )
        if status != 200:
            raise AssertionError(f"Sign-in failed with HTTP {status}")
        self.token = self.request("GET", "/api/csrf")[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8181")
    args = parser.parse_args()
    target = urllib.parse.urlparse(args.base_url)
    if target.scheme != "http" or target.hostname not in {"127.0.0.1", "localhost", "::1"}:
        parser.error("Only a local HTTP server is allowed; this smoke test writes demo data.")
    if target.username or target.password or target.path not in {"", "/"}:
        parser.error("Use a loopback origin without credentials or a path.")

    checks = []

    def check(condition, label):
        if not condition:
            raise AssertionError(label)
        checks.append(label)
        print(f"PASS {label}")

    public = Client(args.base_url)
    check(public.request("GET", "/")[0] == 200, "store page served")
    check(public.request("GET", "/admin.html")[0] == 200, "admin page served")
    check(public.request("GET", "/book/inventory")[0] == 401, "anonymous inventory denied")

    admin = Client(args.base_url)
    admin.login("admin", os.environ.get("BOOKSTORE_ADMIN_PASSWORD", "admin-demo-only"))
    check(admin.request("POST", "/book/add", {}, csrf=False)[0] == 403, "CSRF-less admin write denied")
    suffix = uuid.uuid4().hex[:12]
    payload = {
        "title": f"HTTP smoke book {suffix}",
        "author": "Fictional smoke fixture",
        "isbn": "9780132350884",
        "description": "Created by scripts/http_smoke.py; archived after successful verification.",
        "ourPrice": 19.99,
        "rentalPrice": 4.50,
        "inStockNumber": 3,
        "active": True,
    }
    status, book = admin.request("POST", "/book/add", payload)
    check(status == 200 and book.get("id"), "admin creates inventory")
    book_id = book["id"]

    student = Client(args.base_url)
    username = "smoke_" + suffix
    password = "smoke-test-password-only"
    status, _ = student.request("POST", "/user/newUser", {
        "username": username, "email": username + "@example.test", "password": password
    })
    check(status == 200, "student registration persists")
    student.login(username, password)
    status, account = student.request("GET", "/user/getCurrentUser")
    check(status == 200 and account["username"] == username and "password" not in account,
          "authenticated session works without exposing password")
    check(student.request("GET", "/book/inventory")[0] == 403, "student inventory denied")
    for mode in ("BUY", "RENT"):
        status, _ = student.request("POST", "/cart/add", {"bookId": book_id, "qty": 1, "mode": mode})
        check(status == 200, f"{mode.lower()} cart line saved")
    status, order = student.request("POST", "/checkout/checkout", {})
    check(status == 200 and Decimal(str(order["orderTotal"])) == Decimal("24.49"),
          "mixed purchase/rental checkout uses exact totals")
    check(order["orderStatus"] == "DEMO_CONFIRMED_NO_PAYMENT", "checkout explicitly records no payment")
    rental = next(line for line in order["cartItemList"] if line["mode"] == "RENT")
    check(bool(rental["dueDate"]), "rental due date stored")
    check(public.request("GET", f"/book/{book_id}")[1]["inStockNumber"] == 1,
          "checkout decrements inventory")
    check(student.request("GET", "/cart/getCartItemList")[1] == [], "checkout clears cart")
    status, desk = admin.request("GET", "/api/rentals")
    row = next(item for item in desk if item["id"] == rental["id"])
    check(status == 200 and row["borrower"] == username and row["orderId"] == order["id"],
          "admin rental desk identifies borrower and order")
    path = f"/api/rentals/{rental['id']}/return"
    check(student.request("POST", path, {})[0] == 403, "student cannot confirm inventory return")
    check(admin.request("POST", path, {})[0] == 200, "admin return accepted")
    check(admin.request("POST", path, {})[0] == 409, "duplicate return rejected")
    check(public.request("GET", f"/book/{book_id}")[1]["inStockNumber"] == 2,
          "return restores stock exactly once")
    status, quote = admin.request("GET", "/api/prices/9780132350884")
    check(status == 200 and quote["mode"] == "SAMPLE_HTML_NOT_LIVE"
          and Decimal(str(quote["suggestedPrice"])) == Decimal("25"),
          "sample HTML pricing remains explicitly labeled")
    check(Decimal(str(public.request("GET", f"/book/{book_id}")[1]["ourPrice"])) == Decimal("19.99"),
          "quote does not silently overwrite price")
    book.update(ourPrice=26, inStockNumber=2)
    status, _ = admin.request("POST", "/book/update", book)
    check(status == 200 and public.request("GET", f"/book/{book_id}")[1]["ourPrice"] == 26,
          "admin override persists")
    history = student.request("GET", "/order/getOrderList")[1]
    check(Decimal(str(history[0]["orderTotal"])) == Decimal("24.49"),
          "past order total survives price change")
    check(admin.request("POST", "/book/remove", book_id)[0] == 200
          and public.request("GET", f"/book/{book_id}")[0] == 404,
          "archiving hides book and preserves order history")
    check(student.request("POST", "/api/logout")[0] == 204
          and student.request("GET", "/user/getCurrentUser")[0] == 401,
          "logout invalidates real session")
    print(f"\n{len(checks)} HTTP checks passed. Fictional account {username}; archived book {book_id}.")


if __name__ == "__main__":
    main()
