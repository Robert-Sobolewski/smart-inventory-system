import { Component, inject } from '@angular/core';
import { InventoryService } from '../../services/inventory-service';
import { CurrencyPipe } from '@angular/common';

@Component({
  imports: [CurrencyPipe],
  selector: 'app-product-list',
  styleUrl: './product-list.scss',
  templateUrl: './product-list.html',
  standalone: true
})
export class ProductList {

  protected readonly inventoryService = inject(InventoryService);
  protected readonly productResource = this.inventoryService.productResource;
}
