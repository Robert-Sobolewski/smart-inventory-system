import { Component, inject, signal, computed } from '@angular/core';
import { form, FormField, required, validate } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { LogisticsService } from '../../services/logistics-service';

// Local form model: qty is number|null — <input type="number"> returns null for an empty field
interface ReceiptFormModel {
  sku: string;
  loc: string; // location code (String), not the ID — that's how ReceiveRequest.java defines it
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

  // true only when the list is loaded and empty — .value() throws in the error state,
  // so it must not be called unconditionally in the template (hence the guarded computed)
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
    // qty must be an integer >= 1 (backend: Integer, FIFO batch)
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
        // message from the backend, e.g. "Wareneingang fuer SKU X erfolgreich gebucht!"
        this.receiveMessage.set(serverMessage ?? `Receipt for ${payload.sku} successful`);
        this.receiveError.set(null);
        // reset() clears the value + touched/dirty and syncs the input (FormField)
        this.receiptForm().reset({ sku: '', loc: '', qty: null });
      },
      error: (err) => {
        let body = err?.error;
        if (typeof body === 'string') {
          // responseType 'text' -> the body is a raw string, even on a 400.
          // GlobalExceptionHandler returns JSON {message,...} — try to parse it.
          try {
            const parsed = JSON.parse(body);
            if (parsed && typeof parsed.message === 'string') {
              this.receiveError.set(parsed.message);
              return;
            }
          } catch { /* not JSON -> plain-text error */ }
          this.receiveError.set(body.length > 0 ? body : 'Could not receive goods');
        } else if (body && typeof body.message === 'string') {
          // a JSON object (e.g. if responseType were 'json')
          this.receiveError.set(body.message);
        } else {
          this.receiveError.set('Could not receive goods');
        }
      },
    });
  }
}
