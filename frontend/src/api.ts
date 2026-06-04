import type {
  Inventory,
  JsonApiCollection,
  JsonApiDocument,
  Product,
  Purchase
} from "./types";

const JSON_API = "application/vnd.api+json";

export interface ApiConfig {
  productsUrl: string;
  inventoryUrl: string;
  apiKey: string;
}

export async function createProduct(
  config: ApiConfig,
  input: { name: string; price: number; description: string }
): Promise<Product> {
  const document = await request<JsonApiDocument<Product>>(config.productsUrl, "/products", {
    method: "POST",
    apiKey: config.apiKey,
    body: {
      data: {
        type: "products",
        attributes: {
          name: input.name,
          price: input.price,
          description: input.description || null
        }
      }
    }
  });

  return mapProduct(document.data);
}

export async function listProducts(config: ApiConfig): Promise<Product[]> {
  const document = await request<JsonApiCollection<Product>>(config.productsUrl, "/products", {
    method: "GET",
    apiKey: config.apiKey
  });

  return document.data.map(mapProduct);
}

export async function getInventory(config: ApiConfig, productId: string): Promise<Inventory> {
  const document = await request<JsonApiDocument<{ quantity: number }>>(
    config.inventoryUrl,
    `/inventory/${productId}`,
    {
      method: "GET",
      apiKey: config.apiKey
    }
  );

  return {
    productId: document.data.id,
    quantity: document.data.attributes.quantity
  };
}

export async function updateInventory(
  config: ApiConfig,
  productId: string,
  quantity: number
): Promise<Inventory> {
  const document = await request<JsonApiDocument<{ quantity: number }>>(
    config.inventoryUrl,
    `/inventory/${productId}`,
    {
      method: "PUT",
      apiKey: config.apiKey,
      body: {
        data: {
          type: "inventories",
          attributes: {
            quantity
          }
        }
      }
    }
  );

  return {
    productId: document.data.id,
    quantity: document.data.attributes.quantity
  };
}

export async function createPurchase(
  config: ApiConfig,
  productId: string,
  quantity: number
): Promise<Purchase> {
  const document = await request<JsonApiDocument<Purchase>>(config.inventoryUrl, "/purchases", {
    method: "POST",
    apiKey: config.apiKey,
    body: {
      data: {
        type: "purchases",
        attributes: {
          productId,
          quantity
        }
      }
    }
  });

  return mapPurchase(document.data);
}

export async function listPurchases(config: ApiConfig): Promise<Purchase[]> {
  const document = await request<JsonApiCollection<Purchase>>(config.inventoryUrl, "/purchases", {
    method: "GET",
    apiKey: config.apiKey
  });

  return document.data.map(mapPurchase);
}

async function request<T>(
  baseUrl: string,
  path: string,
  options: { method: string; apiKey: string; body?: unknown }
): Promise<T> {
  const response = await fetch(`${baseUrl.replace(/\/$/, "")}${path}`, {
    method: options.method,
    headers: {
      Accept: JSON_API,
      "Content-Type": JSON_API,
      "x-api-key": options.apiKey
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const payload = await response.json();

  if (!response.ok) {
    const detail = payload?.errors?.[0]?.detail ?? `HTTP ${response.status}`;
    throw new Error(detail);
  }

  return payload as T;
}

function mapProduct(resource: JsonApiDocument<Product>["data"]): Product {
  return {
    id: resource.id,
    name: resource.attributes.name,
    price: Number(resource.attributes.price),
    description: resource.attributes.description
  };
}

function mapPurchase(resource: JsonApiDocument<Purchase>["data"]): Purchase {
  return {
    id: resource.id,
    productId: resource.attributes.productId,
    quantity: Number(resource.attributes.quantity),
    unitPrice: Number(resource.attributes.unitPrice),
    total: Number(resource.attributes.total),
    remainingQuantity: Number(resource.attributes.remainingQuantity),
    createdAt: resource.attributes.createdAt
  };
}

