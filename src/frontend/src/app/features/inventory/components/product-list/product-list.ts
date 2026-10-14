import { Component, inject, signal } from '@angular/core';
import { form, FormField, required } from '@angular/forms/signals';
import { InventoryService } from '../../services/inventory-service';
import { ProductCreateRequest } from '../../models/product';

// Local form model: all fields are plain strings (empty = not provided)
interface ProductFormModel {
  sku: string;
  name: string;
  category: string;
  description: string;
}

@Component({
  imports: [FormField],
  selector: 'app-product-list',
  styleUrl: './product-list.scss',
  templateUrl: './product-list.html',
})
export class ProductList {

  private readonly inventoryService = inject(InventoryService);
  readonly productResource = this.inventoryService.productResource;

  // Form model for the "add product" form (signal-based)
  readonly productModel = signal<ProductFormModel>({
    sku: '', name: '', category: '', description: ''
  });

  // form validation
  readonly productForm = form(this.productModel, f => {
    required(f.sku, { message: 'SKU is required' });
    required(f.name, { message: 'Name is required' });
  });

  // Feedback messages after a submit attempt
  readonly saveError = signal<string | null>(null);
  readonly savedMessage = signal<string | null>(null);

  onSubmit(event: Event): void {
    event.preventDefault();
    if (this.productForm().invalid()) return;

    const m = this.productModel();
    // Map empty strings to undefined so the backend stores NULL, not ""
    const payload: ProductCreateRequest = {
      sku: m.sku.trim(),
      name: m.name.trim(),
      category: m.category.trim() || undefined,
      description: m.description.trim() || undefined
    };

    this.inventoryService.addProduct(payload).subscribe({
      next: () => {
        // Refresh the list so the new product shows up immediately
        this.productResource.reload();
        this.savedMessage.set(`Product ${payload.sku} created.`);
        this.saveError.set(null);
        this.productModel.set({ sku: '', name: '', category: '', description: '' });
      },
      error: (err) => {
        // Backend rejects duplicate SKUs with an error message in the body
        const msg = err?.error ?? 'Unknown server error';
        this.saveError.set(typeof msg === 'string' ? msg : 'Could not create product.');
      }
    });
  }
}
