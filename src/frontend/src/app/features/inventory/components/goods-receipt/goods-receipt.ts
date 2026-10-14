import { Component, inject, signal, computed } from '@angular/core';
import { form, FormField, required, validate } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { LogisticsService } from '../../services/logistics-service';

// Local form model: qty is number|null — <input type="number"> zwraca null dla pustego pola
interface ReceiptFormModel {
  sku: string;
  loc: string; // kod lokalizacji (String), nie ID — tak definiuje to ReceiveRequest.java
  qty: number | null;
}

@Component({
  imports: [FormField, RouterLink],
  selector: 'app-goods-receipt',
  styleUrl: './goods-receipt.scss',
  templateUrl: './goods-receipt.html',
})
export class GoodsReceipt {
  private readonly logisticsService = inject(LogisticsService);

  // Reactive list of all locations for the dropdown (auto-tracked by Angular)
  readonly locationResource = this.logisticsService.locationResource;

  // true tylko gdy lista załadowana i pusta — .value() rzuca wyjątek w stanie error,
  // dlatego nie wolno go wołać bezwarunkowo w template (stąd computed z guardem)
  readonly hasNoLocations = computed(
    () => !this.locationResource.isLoading() && !this.locationResource.error()
      && this.locationResource.value()?.length === 0,
  );

  // form model for 'goods receipt' (Wareneingang)
  readonly receiptModel = signal<ReceiptFormModel>({ sku: '', loc: '', qty: null });

  // form validation
  readonly receiptForm = form(this.receiptModel, f => {
    required(f.sku, { message: 'SKU is required' });
    required(f.loc, { message: 'Location is required' });
    required(f.qty, { message: 'Quantity is required' });
    // qty musi być liczbą całkowitą >= 1 (backend: Integer, FIFO batch)
    validate(f.qty, ctx => {
      const v = ctx.value();
      if (v !== null && (!Number.isInteger(v) || v < 1)) {
        return { kind: 'integer', message: 'Quantity must be a whole number >= 1' };
      }
      return null;
    });
  });

  // Feedback messages after a submit attempt
  readonly receiveError = signal<string | null>(null);
  readonly receiveMessage = signal<string | null>(null);

  onSubmit(event: Event): void {
    event.preventDefault();
    if (this.receiptForm().invalid()) return;
    const m = this.receiptModel();
    // payload mirrors the backend ReceiveRequest DTO: {sku, loc, qty}
    const payload = { sku: m.sku.trim(), loc: m.loc, qty: m.qty as number };

    this.logisticsService.receiveGoods(payload).subscribe({
      next: (serverMessage) => {
        // komunikat z backendu, np. "Wareneingang fuer SKU X erfolgreich gebucht!"
        this.receiveMessage.set(serverMessage ?? `Receipt for ${payload.sku} successful`);
        this.receiveError.set(null);
        // reset() czyści wartość + touched/dirty i synchronizuje input (FormField)
        this.receiptForm().reset({ sku: '', loc: '', qty: null });
      },
      error: (err) => {
        let body = err?.error;
        if (typeof body === 'string') {
          // responseType 'text' -> body jest surowym stringiem, nawet przy 400.
          // GlobalExceptionHandler zwraca JSON {message,...} - spróbuj go sparsować.
          try {
            const parsed = JSON.parse(body);
            if (parsed && typeof parsed.message === 'string') {
              this.receiveError.set(parsed.message);
              return;
            }
          } catch { /* nie jest JSON -> plain-text błąd */ }
          this.receiveError.set(body.length > 0 ? body : 'Could not receive goods');
        } else if (body && typeof body.message === 'string') {
          // obiekt JSON (np. gdyby responseType był 'json')
          this.receiveError.set(body.message);
        } else {
          this.receiveError.set('Could not receive goods');
        }
      },
    });
  }
}
