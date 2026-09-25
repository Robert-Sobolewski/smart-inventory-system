export interface Product {
  id: number;
  name: string;
  sku: string;
  quantity: number;
  price: number;
  category?: string;
}
