"use strict";
const $ = (s) => document.querySelector(s);
const adminPage = document.body.dataset.admin === "true";
let csrf,
  current,
  books = [];
const money = (n) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(
    n,
  );
function notice(message, error = false) {
  $("#notice").textContent = message;
  $("#notice").classList.toggle("error", error);
}
function element(tag, text, className) {
  const n = document.createElement(tag);
  if (text !== undefined) n.textContent = text;
  if (className) n.className = className;
  return n;
}
function action(text, fn) {
  const b = element("button", text);
  b.type = "button";
  b.addEventListener("click", () =>
    run(async () => {
      b.disabled = true;
      try {
        await fn();
      } finally {
        b.disabled = false;
      }
    }),
  );
  return b;
}
async function run(fn) {
  try {
    await fn();
  } catch (error) {
    notice(error.message, true);
  }
}
async function api(path, method = "GET", body, extraHeaders = {}) {
  const headers = { ...extraHeaders };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET") {
    headers[csrf.header] = csrf.token;
  }
  const res = await fetch(path, {
    method,
    headers,
    credentials: "same-origin",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!res.ok)
    throw new Error(
      data?.message ||
        `${res.status}: ${res.status === 403 ? "Administrator access or fresh sign-in required." : res.status === 401 ? "Sign in to continue." : "Request failed."}`,
    );
  return data;
}
async function session() {
  const res = await fetch("/user/getCurrentUser", {
    credentials: "same-origin",
  });
  current = res.ok ? await res.json() : null;
  $("#account").hidden = !!current;
  $("#signed-in").hidden = !current;
  if (current)
    $("#identity").textContent =
      `Signed in as ${current.username}${current.admin ? " · administrator" : ""}`;
  if (adminPage) {
    $("#administration").hidden = !current?.admin;
    if (current && !current.admin)
      notice("This account cannot manage inventory.", true);
    if (current?.admin) await admin();
  } else {
    $("#shopping").hidden = !current;
    await catalog();
    if (current) await shopping();
  }
}
$("#login").addEventListener("submit", (e) => {
  e.preventDefault();
  run(async () => {
    const body = Object.fromEntries(new FormData(e.target));
    await api("/token", "POST", undefined, {
      Authorization: `Basic ${btoa(String.fromCharCode(...new TextEncoder().encode(body.username + ":" + body.password)))}`,
    });
    e.target.reset();
    csrf = await api("/api/csrf");
    notice("Signed in.");
    await session();
  });
});
$("#logout").addEventListener("click", () =>
  run(async () => {
    await api("/api/logout", "POST");
    csrf = await api("/api/csrf");
    notice("Signed out.");
    await session();
  }),
);
$("#register")?.addEventListener("submit", (e) => {
  e.preventDefault();
  run(async () => {
    const result = await api(
      "/user/newUser",
      "POST",
      Object.fromEntries(new FormData(e.target)),
    );
    e.target.reset();
    notice(result.message);
  });
});
async function catalog() {
  books = await api("/book/bookList");
  drawCatalog();
}
function drawCatalog() {
  const container = $("#catalog");
  container.replaceChildren();
  const query = $("#search").value.toLowerCase();
  for (const book of books.filter((b) =>
    (b.title + " " + b.author + " " + b.isbn).toLowerCase().includes(query),
  )) {
    const card = element("article", undefined, "card");
    card.append(
      element("h3", book.title),
      element("p", book.author),
      element("p", `ISBN ${book.isbn}`, "muted"),
      element("p", book.description || ""),
      element(
        "p",
        `${book.inStockNumber} available · Buy ${money(book.ourPrice)} · Rent ${money(book.rentalPrice)}`,
      ),
    );
    const controls = element("div", undefined, "actions"),
      qty = element("input");
    qty.type = "number";
    qty.min = "1";
    qty.max = String(book.inStockNumber);
    qty.value = "1";
    qty.setAttribute("aria-label", `Quantity of ${book.title}`);
    controls.append(qty);
    for (const mode of ["BUY", "RENT"]) {
      const b = action(
        mode === "BUY" ? "Add to buy" : "Rent 30 days",
        async () => {
          await api("/cart/add", "POST", {
            bookId: book.id,
            qty: Number(qty.value),
            mode,
          });
          notice("Book added to cart.");
          await shopping();
        },
      );
      b.disabled = !current || book.inStockNumber === 0;
      controls.append(b);
    }
    card.append(controls);
    container.append(card);
  }
  if (!container.children.length)
    container.append(element("p", "No matching books."));
}
$("#search")?.addEventListener("input", drawCatalog);
async function shopping() {
  const cart = await api("/cart/getCartItemList");
  $("#cart").replaceChildren();
  let total = 0;
  for (const item of cart) {
    total += Number(item.subtotal);
    const row = element("div", undefined, "record");
    row.append(
      element(
        "strong",
        `${item.book.title} · ${item.mode} × ${item.qty} · ${money(item.subtotal)}`,
      ),
    );
    const qty = element("input");
    qty.type = "number";
    qty.min = "1";
    qty.max = "100";
    qty.value = String(item.qty);
    qty.setAttribute("aria-label", `Update quantity of ${item.book.title}`);
    const actions = element("div", undefined, "actions");
    actions.append(
      qty,
      action("Update quantity", async () => {
        await api("/cart/updateCartItem", "POST", {
          cartItemId: item.id,
          qty: Number(qty.value),
        });
        await shopping();
      }),
      action("Remove", async () => {
        await api("/cart/removeItem", "POST", item.id);
        await shopping();
      }),
    );
    row.append(actions);
    $("#cart").append(row);
  }
  $("#cart").append(
    element("p", cart.length ? `Total: ${money(total)}` : "Cart is empty."),
  );
  $("#checkout").disabled = !cart.length;
  const orders = await api("/order/getOrderList");
  $("#orders").replaceChildren();
  for (const order of orders) {
    const row = element("div", undefined, "record");
    row.append(
      element("h3", `Order ${order.id} · ${money(order.orderTotal)}`),
      element("p", "Demo confirmed · no payment · campus pickup"),
    );
    for (const item of order.cartItemList) {
      row.append(
        element(
          "p",
          `Line ${item.id}: ${item.book.title} · ${item.mode} × ${item.qty} · ${money(item.subtotal)}${item.dueDate ? ` · due ${item.dueDate}` : ""}${item.returnedDate ? ` · returned ${item.returnedDate}` : ""}`,
        ),
      );
    }
    $("#orders").append(row);
  }
  if (!orders.length) $("#orders").append(element("p", "No orders yet."));
}
$("#checkout")?.addEventListener("click", () =>
  run(async () => {
    const b = $("#checkout");
    b.disabled = true;
    try {
      const order = await api("/checkout/checkout", "POST", {});
      notice(`Order ${order.id} recorded. No money charged.`);
      await catalog();
      await shopping();
    } finally {
      b.disabled = false;
    }
  }),
);
function resetEditor() {
  const f = $("#book-editor");
  f.reset();
  f.elements.id.value = "";
  $("#editor-title").textContent = "Add a used book";
}
$("#cancel-edit")?.addEventListener("click", resetEditor);
$("#book-editor")?.addEventListener("submit", (e) => {
  e.preventDefault();
  run(async () => {
    const body = Object.fromEntries(new FormData(e.target));
    for (const key of ["ourPrice", "rentalPrice", "inStockNumber"])
      body[key] = Number(body[key]);
    body.active = e.target.elements.active.checked;
    const update = !!body.id;
    if (update) body.id = Number(body.id);
    else delete body.id;
    await api(`/book/${update ? "update" : "add"}`, "POST", body);
    resetEditor();
    notice("Inventory saved.");
    await admin();
  });
});
async function admin() {
  books = await api("/book/inventory");
  $("#inventory").replaceChildren();
  for (const book of books) {
    const card = element("article", undefined, "card");
    card.append(
      element("h3", book.title),
      element(
        "p",
        `${book.isbn} · ${book.inStockNumber} available${book.active ? "" : " · archived"}`,
      ),
      element(
        "p",
        `Buy ${money(book.ourPrice)} · Rent ${money(book.rentalPrice)}`,
      ),
    );
    const actions = element("div", undefined, "actions");
    actions.append(
      action("Edit", async () => {
        const f = $("#book-editor");
        for (const name of [
          "id",
          "title",
          "author",
          "isbn",
          "ourPrice",
          "rentalPrice",
          "inStockNumber",
          "category",
          "description",
        ])
          f.elements[name].value = book[name] ?? "";
        f.elements.active.checked = book.active;
        $("#editor-title").textContent = `Edit ${book.title}`;
        f.scrollIntoView({ behavior: "smooth" });
      }),
      action("Price research", async () => {
        $("#price-lookup").elements.isbn.value = book.isbn;
        await prices(book.isbn);
      }),
      action("Archive", async () => {
        await api("/book/remove", "POST", book.id);
        notice("Book archived.");
        await admin();
      }),
    );
    card.append(actions);
    $("#inventory").append(card);
  }
  const rentals = await api("/api/rentals");
  $("#rentals").replaceChildren();
  for (const item of rentals) {
    const row = element("div", undefined, "record");
    row.append(
      element(
        "strong",
        `Rental line ${item.id}: ${item.book.title} × ${item.qty}`,
      ),
      element(
        "p",
        `Borrower ${item.borrower} · Order ${item.orderId} · Due ${item.dueDate}${item.returnedDate ? ` · returned ${item.returnedDate}` : ""}`,
      ),
    );
    if (!item.returnedDate)
      row.append(
        action("Confirm physical return", async () => {
          await api(`/api/rentals/${item.id}/return`, "POST", {});
          notice("Return recorded; inventory restored.");
          await admin();
        }),
      );
    $("#rentals").append(row);
  }
  if (!rentals.length)
    $("#rentals").append(element("p", "No completed rentals yet."));
}
async function prices(isbn) {
  const q = await api(`/api/prices/${encodeURIComponent(isbn)}`);
  const box = $("#prices");
  box.replaceChildren(
    element(
      "h3",
      `${q.mode === "LIVE" ? "Live source lookup" : "Sample HTML lookup · not market data"}`,
    ),
  );
  for (const source of q.sources)
    box.append(
      element(
        "p",
        `${source.source}: ${source.price === null ? source.status : money(source.price) + " · " + source.status}`,
      ),
    );
  if (q.suggestedPrice !== null) {
    box.append(
      element("strong", `Suggested median: ${money(q.suggestedPrice)}`),
    );
    box.append(
      action("Use suggestion in editor", async () => {
        const book = books.find((b) => b.isbn === isbn);
        if (!book) throw new Error("Add this ISBN to inventory first.");
        const f = $("#book-editor");
        for (const name of [
          "id",
          "title",
          "author",
          "isbn",
          "ourPrice",
          "rentalPrice",
          "inStockNumber",
          "category",
          "description",
        ])
          f.elements[name].value = book[name] ?? "";
        f.elements.active.checked = book.active;
        f.elements.ourPrice.value = q.suggestedPrice;
        $("#editor-title").textContent = `Review price for ${book.title}`;
        notice("Suggestion loaded. Review and Save book to apply.");
        f.scrollIntoView({ behavior: "smooth" });
      }),
    );
  } else box.append(element("p", "No valid offers. Existing price unchanged."));
}
$("#price-lookup")?.addEventListener("submit", (e) => {
  e.preventDefault();
  run(() => prices(e.target.elements.isbn.value.trim()));
});
run(async () => {
  csrf = await api("/api/csrf");
  await session();
});
