// Mirrors com.sisie.core.model.Supplier on the backend (GET /api/suppliers)
export interface Supplier {
  id?: number;
  name: string;
  email?: string;
  address?: string;
}

// Payload for POST/PUT /api/suppliers — scalar fields only, no products relation.
// NOTE: the backend update() expects a non-null email (NPE otherwise), so the UI
// always sends all three fields on update.
export interface SupplierRequest {
  name: string;
  email?: string;
  address?: string;
}
