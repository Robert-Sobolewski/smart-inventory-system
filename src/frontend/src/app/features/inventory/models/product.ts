// Mirrors the backend Product entity (com.sisie.core.model.Product)
export interface Product {
  id: number;
  sku: string;
  name: string;
  category?: string;
  description?: string;
  minStockLevel?: number;
}

// Payload for POST /api/products/create — mirrors the backend ProductRequest DTO
export interface ProductCreateRequest {
  sku: string;
  name: string;
  category?: string;
  description?: string;
}
