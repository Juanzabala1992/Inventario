<script setup lang="ts">
import {
  AlertCircle,
  CheckCircle2,
  KeyRound,
  PackagePlus,
  RefreshCw,
  Save,
  ShoppingCart
} from "@lucide/vue";
import { computed, onMounted, reactive, ref } from "vue";
import {
  createProduct,
  createPurchase,
  getInventory,
  listProducts,
  listPurchases,
  updateInventory
} from "./api";
import type { ApiConfig } from "./api";
import type { Inventory, Product, Purchase } from "./types";

const money = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0
});

const config = reactive<ApiConfig>({
  productsUrl: import.meta.env.VITE_PRODUCTS_API_URL ?? "http://localhost:8081",
  inventoryUrl: import.meta.env.VITE_INVENTORY_API_URL ?? "http://localhost:8082",
  apiKey: import.meta.env.VITE_SERVICE_API_KEY ?? "local-dev-key"
});

const productForm = reactive({
  name: "",
  price: 0,
  description: ""
});

const products = ref<Product[]>([]);
const purchases = ref<Purchase[]>([]);
const inventory = ref<Inventory | null>(null);
const selectedProductId = ref("");
const stockQuantity = ref(0);
const purchaseQuantity = ref(1);
const message = ref("");
const error = ref("");
const busy = reactive({
  products: false,
  productCreate: false,
  inventory: false,
  purchase: false,
  purchases: false
});

const selectedProduct = computed(() =>
  products.value.find((product) => product.id === selectedProductId.value) ?? null
);

const canOperate = computed(() => Boolean(selectedProduct.value));

onMounted(async () => {
  await refreshProducts();
  await refreshPurchases();
});

async function refreshProducts() {
  await run(async () => {
    busy.products = true;
    products.value = await listProducts(config);

    if (!selectedProductId.value && products.value.length > 0) {
      selectedProductId.value = products.value[0].id;
      await loadSelectedInventory();
    }
  }, "Productos actualizados");
  busy.products = false;
}

async function submitProduct() {
  await run(async () => {
    busy.productCreate = true;
    const created = await createProduct(config, {
      name: productForm.name,
      price: Number(productForm.price),
      description: productForm.description
    });

    productForm.name = "";
    productForm.price = 0;
    productForm.description = "";
    selectedProductId.value = created.id;
    await refreshProducts();
  }, "Producto creado");
  busy.productCreate = false;
}

async function selectProduct(productId: string) {
  selectedProductId.value = productId;
  await loadSelectedInventory();
}

async function loadSelectedInventory() {
  if (!selectedProductId.value) {
    inventory.value = null;
    return;
  }

  await run(async () => {
    busy.inventory = true;
    inventory.value = await getInventory(config, selectedProductId.value);
    stockQuantity.value = inventory.value.quantity;
  }, "Inventario actualizado");
  busy.inventory = false;
}

async function submitInventory() {
  const product = selectedProduct.value;
  if (!product) return;

  await run(async () => {
    busy.inventory = true;
    inventory.value = await updateInventory(
      config,
      product.id,
      Number(stockQuantity.value)
    );
  }, "Stock guardado");
  busy.inventory = false;
}

async function submitPurchase() {
  const product = selectedProduct.value;
  if (!product) return;

  await run(async () => {
    busy.purchase = true;
    const purchase = await createPurchase(
      config,
      product.id,
      Number(purchaseQuantity.value)
    );
    inventory.value = {
      productId: purchase.productId,
      quantity: purchase.remainingQuantity
    };
    stockQuantity.value = purchase.remainingQuantity;
    purchaseQuantity.value = 1;
    await refreshPurchases();
  }, "Compra registrada");
  busy.purchase = false;
}

async function refreshPurchases() {
  await run(async () => {
    busy.purchases = true;
    purchases.value = await listPurchases(config);
  });
  busy.purchases = false;
}

async function run(action: () => Promise<void>, successMessage = "") {
  error.value = "";
  message.value = "";
  try {
    await action();
    message.value = successMessage;
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : "Operacion no disponible";
  }
}
</script>

<template>
  <main class="shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">Vue + Spring Boot</p>
        <h1>Inventory Console</h1>
      </div>
      <div class="api-key">
        <KeyRound :size="16" />
        <input v-model="config.apiKey" aria-label="API key" />
      </div>
    </header>

    <section class="status-row" aria-live="polite">
      <div v-if="message" class="notice success">
        <CheckCircle2 :size="18" />
        <span>{{ message }}</span>
      </div>
      <div v-if="error" class="notice danger">
        <AlertCircle :size="18" />
        <span>{{ error }}</span>
      </div>
    </section>

    <section class="grid">
      <form class="panel" @submit.prevent="submitProduct">
        <div class="panel-title">
          <h2>Producto</h2>
          <button type="submit" class="action-button primary" title="Crear producto" :disabled="busy.productCreate">
            <PackagePlus :size="18" />
            <span>Crear</span>
          </button>
        </div>

        <label>
          Nombre
          <input v-model="productForm.name" required />
        </label>

        <label>
          Precio
          <input v-model.number="productForm.price" min="0" required type="number" />
        </label>

        <label>
          Descripcion
          <textarea v-model="productForm.description" rows="3" />
        </label>
      </form>

      <section class="panel products-panel">
        <div class="panel-title">
          <h2>Productos</h2>
          <button type="button" class="icon-button" title="Actualizar productos" @click="refreshProducts">
            <RefreshCw :size="18" />
          </button>
        </div>

        <div class="product-list">
          <p v-if="products.length === 0" class="empty-state">
            No hay productos registrados.
          </p>
          <button
            v-for="product in products"
            :key="product.id"
            class="product-row"
            :class="{ active: product.id === selectedProductId }"
            type="button"
            @click="selectProduct(product.id)"
          >
            <span>
              <strong>{{ product.name }}</strong>
              <small>{{ product.description || "Sin descripcion" }}</small>
            </span>
            <b>{{ money.format(product.price) }}</b>
          </button>
        </div>
      </section>

      <section class="panel operation-panel">
        <div class="panel-title">
          <h2>Inventario</h2>
          <button type="button" class="icon-button" title="Consultar inventario" @click="loadSelectedInventory">
            <RefreshCw :size="18" />
          </button>
        </div>

        <div class="selected-product">
          <span>{{ selectedProduct?.name ?? "Sin producto" }}</span>
          <strong>{{ inventory?.quantity ?? 0 }} disponibles</strong>
        </div>

        <form class="inline-form" @submit.prevent="submitInventory">
          <label>
            Stock
            <input v-model.number="stockQuantity" min="0" type="number" :disabled="!canOperate" />
          </label>
          <button type="submit" class="icon-button primary" title="Guardar stock" :disabled="!canOperate">
            <Save :size="18" />
          </button>
        </form>

        <form class="inline-form purchase-form" @submit.prevent="submitPurchase">
          <label>
            Compra
            <input v-model.number="purchaseQuantity" min="1" type="number" :disabled="!canOperate" />
          </label>
          <button type="submit" class="icon-button accent" title="Registrar compra" :disabled="!canOperate">
            <ShoppingCart :size="18" />
          </button>
        </form>
      </section>

      <section class="panel history-panel">
        <div class="panel-title">
          <h2>Compras</h2>
          <button type="button" class="icon-button" title="Actualizar compras" @click="refreshPurchases">
            <RefreshCw :size="18" />
          </button>
        </div>

        <table>
          <thead>
            <tr>
              <th>Producto</th>
              <th>Cant.</th>
              <th>Total</th>
              <th>Stock</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="purchases.length === 0">
              <td colspan="4" class="empty-cell">No hay compras registradas.</td>
            </tr>
            <tr v-for="purchase in purchases" :key="purchase.id">
              <td>{{ purchase.productId.slice(0, 8) }}</td>
              <td>{{ purchase.quantity }}</td>
              <td>{{ money.format(purchase.total) }}</td>
              <td>{{ purchase.remainingQuantity }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </section>
  </main>
</template>
