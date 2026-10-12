
(() => {
  const $ = (id) => document.getElementById(id);

  const form = $("createSaleForm");
  const createButton = $("createSaleButton");
  const createMessage = $("createSaleMessage");
  const productSelect = $("productSelect");
  const reserveButton = $("reserveButton");

  let products = [];
  let selectedProduct = null;
  let pendingKey = null;
  let reservations = 0;
  let busy = false;
  let saleTimer = null;

  function log(level, message) {
    const line = document.createElement("p");
    const time = document.createElement("time");
    const badge = document.createElement("span");
    const text = document.createElement("span");

    time.textContent = new Date().toLocaleTimeString("en-GB");
    badge.textContent = `[${level}]`;
    badge.className =
      level === "OK" ? "log-ok" :
      level === "ERR" ? "log-error" : "log-info";
    text.textContent = message;

    line.append(time, badge, text);
    $("activityLog").prepend(line);

    while ($("activityLog").children.length > 8) {
      $("activityLog").lastElementChild.remove();
    }
  }

  function money(value) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 2
    }).format(value);
  }

  function setMessage(message, type = "") {
    createMessage.textContent = message;
    createMessage.className = `form-message ${type}`;
  }

  async function api(url, options = {}) {
    const response = await fetch(url, {
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers
      }
    });

    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || `Request failed (${response.status})`);
    }

    return text ? JSON.parse(text) : null;
  }

  function statusOf(product) {
    const now = Date.now();
    const start = new Date(product.saleStartsAt).getTime();
    const end = new Date(product.saleEndsAt).getTime();

    if (!Number.isFinite(start) || !Number.isFinite(end)) {
      return "INVALID";
    }
    if (now < start) return "UPCOMING";
    if (now >= end) return "ENDED";
    if (product.stock <= 0) return "SOLD OUT";
    return "ACTIVE";
  }

  function updateCountdown() {
    if (!selectedProduct) return;

    const product = selectedProduct;
    const now = Date.now();
    const start = new Date(product.saleStartsAt).getTime();
    const end = new Date(product.saleEndsAt).getTime();
    const status = statusOf(product);

    $("saleStatus").textContent = status;
    $("countdownLabel").textContent =
      status === "UPCOMING" ? "SALE STARTS IN" : "SALE ENDS IN";

    let target = status === "UPCOMING" ? start : end;

    if (status === "ACTIVE") {
      const seconds = Math.max(0, Math.floor((target - now) / 1000));
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;

      $("countdown").textContent =
        `${String(hours).padStart(2, "0")}:` +
        `${String(minutes).padStart(2, "0")}:` +
        `${String(secs).padStart(2, "0")}`;
    } else if (status === "UPCOMING") {
      const seconds = Math.max(0, Math.floor((target - now) / 1000));
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;
      $("countdown").textContent =
        `${String(hours).padStart(2, "0")}:` +
        `${String(minutes).padStart(2, "0")}:` +
        `${String(secs).padStart(2, "0")}`;
    } else {
      $("countdown").textContent = status;
    }

    reserveButton.disabled = busy || status !== "ACTIVE";
    reserveButton.innerHTML = busy
      ? "PROCESSING..."
      : status === "ACTIVE"
        ? "▸ RESERVE ITEM"
        : status === "UPCOMING"
          ? "SALE NOT STARTED"
          : status === "SOLD OUT"
            ? "SOLD OUT"
            : "SALE CLOSED";
  }

  function renderProduct() {
    const p = selectedProduct;

    if (!p) {
      $("productName").textContent = "No products found";
      $("productDescription").textContent = "Create a sale using the admin form.";
      $("productPrice").textContent = "₹---";
      $("stockLabel").textContent = "---";
      $("stockBar").style.width = "0%";
      $("productIdLabel").textContent = "---";
      $("sideProductId").textContent = "---";
      $("initialStock").textContent = "---";
      $("sidePrice").textContent = "₹---";
      $("saleStatus").textContent = "NO PRODUCTS";
      $("countdown").textContent = "--:--";
      reserveButton.disabled = true;
      return;
    }

    $("productName").textContent = p.name;
    $("productDescription").textContent = `Product ID: ${p.id}`;
    $("productPrice").textContent = money(p.price);
    $("productIdLabel").textContent = String(p.id).padStart(3, "0");
    $("sideProductId").textContent = p.id;
    $("initialStock").textContent = p.stock;
    $("sidePrice").textContent = money(p.price);
    $("stockLabel").textContent = `${p.stock} UNITS LEFT`;

    // The API exposes current stock, not original stock.
    // This bar is a visual indicator, not a percentage of initial stock.
    $("stockBar").style.width = p.stock > 0 ? "100%" : "0%";

    updateCountdown();
  }

  async function loadProducts(preferredId) {
    products = await api("/api/products");

    productSelect.replaceChildren();

    for (const p of products) {
      const option = document.createElement("option");
      option.value = p.id;
      option.textContent = `#${p.id} — ${p.name} (${p.stock} left)`;
      productSelect.append(option);
    }

    if (!products.length) {
      selectedProduct = null;
      renderProduct();
      log("INFO", "No products yet. Create your first sale.");
      return;
    }

    const id = preferredId ?? selectedProduct?.id ?? products[0].id;
    selectedProduct = products.find((p) => String(p.id) === String(id))
      ?? products[0];

    productSelect.value = String(selectedProduct.id);
    renderProduct();
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (createButton.disabled) return;

    const data = new FormData(form);
    const saleStartsAt = data.get("saleStartsAt");
    const saleEndsAt = data.get("saleEndsAt");

    if (new Date(saleEndsAt) <= new Date(saleStartsAt)) {
      setMessage("Sale end must be later than sale start.", "error");
      return;
    }

   const payload = {
     name: String(data.get("name")).trim(),
     price: Number(data.get("price")),
     stock: Number(data.get("stock")),
     saleStartsAt: new Date(saleStartsAt).toISOString(),
     saleEndsAt: new Date(saleEndsAt).toISOString()
   };

    createButton.disabled = true;
    setMessage("Creating sale...");

    try {
      const created = await api("/api/products", {
        method: "POST",
        body: JSON.stringify(payload)
      });

      // Refresh first so a Redis initialization error can be detected
      // without blindly submitting a second create request.
      await loadProducts(created.id);

      setMessage(`Sale created successfully. Product ID: ${created.id}`, "success");
      log("OK", `Created "${created.name}" (#${created.id}).`);
      form.reset();
    } catch (error) {
      setMessage(
        `Request failed: ${error.message}. Check the product list before retrying.`,
        "error"
      );
      log("ERR", "Create sale request failed. Verify whether it was saved.");
      try {
        await loadProducts();
      } catch {
        // Keep the original error visible.
      }
    } finally {
      createButton.disabled = false;
    }
  });

  productSelect.addEventListener("change", () => {
    selectedProduct =
      products.find((p) => String(p.id) === productSelect.value) ?? null;
    pendingKey = null;
    reservations = 0;
    $("reservedCount").textContent = "000";
    renderProduct();
  });

  reserveButton.addEventListener("click", async () => {
    if (!selectedProduct || busy || statusOf(selectedProduct) !== "ACTIVE") {
      return;
    }

    pendingKey ??= crypto.randomUUID();
    busy = true;
    updateCountdown();
    log("INFO", `Requesting reservation for product #${selectedProduct.id}...`);

    try {
      const order = await api(`/api/purchases/${selectedProduct.id}`, {
        method: "POST",
        headers: { "Idempotency-Key": pendingKey }
      });

      reservations++;
      $("reservedCount").textContent = String(reservations).padStart(3, "0");
      log("OK", `Order #${order.id} created. Status: ${order.status}.`);
      pendingKey = null;

      await loadProducts(selectedProduct.id);
    } catch (error) {
      log("ERR", error.message || "Purchase failed.");

      // Keep the key after an ambiguous failure to make retry safe.
      // A definitive 409 from the current API arrives as an Error too;
      // refreshing products will at least update visible stock.
      try {
        await loadProducts(selectedProduct?.id);
      } catch {
        // Preserve the original error in the activity log.
      }
    } finally {
      busy = false;
      updateCountdown();
    }
  });

  loadProducts().then(() => {
    log("OK", "Product data loaded from Spring Boot.");
  }).catch((error) => {
    log("ERR", `Could not load products: ${error.message}`);
    $("productName").textContent = "Backend unavailable";
    $("productDescription").textContent = "Check Spring Boot and /api/products.";
  });

  saleTimer = setInterval(updateCountdown, 1000);
})();