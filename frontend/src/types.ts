export interface Product {
  id: string;
  name: string;
  price: number;
  description: string | null;
}

export interface Inventory {
  productId: string;
  quantity: number;
}

export interface Purchase {
  id: string;
  productId: string;
  quantity: number;
  unitPrice: number;
  total: number;
  remainingQuantity: number;
  createdAt: string;
}

export interface JsonApiResource<TAttributes> {
  type: string;
  id: string;
  attributes: TAttributes;
}

export interface JsonApiDocument<TAttributes> {
  data: JsonApiResource<TAttributes>;
  included?: JsonApiResource<Record<string, unknown>>[];
}

export interface JsonApiCollection<TAttributes> {
  data: JsonApiResource<TAttributes>[];
}

