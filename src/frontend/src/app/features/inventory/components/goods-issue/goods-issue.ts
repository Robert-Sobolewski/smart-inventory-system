import { Component, inject, signal, computed } from '@angular/core';
import { form, FormField, required, validate } from '@angular/forms/signals';
import { Subscription } from 'rxjs';
import { LogisticsService } from '../../services/logistics-service';

// Local form model: qty is number|null — <input type="number"> returns null for an empty field
interface IssueFormModel {
  sku: string;
  qty: number | null;
}

@Component({
  imports: [FormField],
  selector: 'app-goods-issue',
  styleUrl: './goods-issue.scss',
  templateUrl: './goods-issue.html',
})
export class GoodsIssue {
  private readonly logisticsService = inject(LogisticsService);

  // form model for 'goods issue' (Warenausgang, FIFO)
  readonly issueModel = signal<IssueFormModel>({ sku: '', qty: null });

  // form validation — structural (required + integer >= 1).
  // The "qty <= stock" validation is a separate computed (stockAvailable) because it depends on the state from the API.
  readonly issueForm = form(this.issueModel, f => {
    required(f.sku, { message: 'SKU is required' });
    required(f.qty, { message: 'Quantity is required' });
    validate(f.qty, ctx => {
      const v = ctx.value();
      if (v !== null && (!Number.isInteger(v) || v < 1)) {
        return { kind: 'integer', message: 'Quantity must be a whole number >= 1' };
      }
      return null;
    });
  });

  // Feedback messages after a submit attempt
  readonly issueError = signal<string | null>(null);
  readonly issueMessage = signal<string | null>(null);

  // --- Stock preview (out-of-stock alert) -------------------------------------
  // On SKU change we fetch the current stock from GET /stock/{sku} (400 ms debounce).
  readonly stockLoading = signal(false);
  readonly stockAvailable = signal<number | null>(null);
  readonly stockError = signal<string | null>(null);

  // qty > available stock -> alert + submit blocked (v1 decision: all-or-nothing)
  readonly overStock = computed(() => {
    const q = this.issueModel().qty;
    const s = this.stockAvailable();
    return q !== null && s !== null && !this.stockError() && q > s;
  });

  // Submit only when: the form is valid + the stock state is known (not loading, no error) + qty <= stock
  readonly canSubmit = computed(() => {
    return this.issueForm().valid()
      && !this.stockLoading()
      && !this.stockError()
      && this.stockAvailable() !== null
      && !this.overStock();
  });

  private stockSub: Subscription | null = null;
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  /**
   * Called from the (change) event on the SKU field — 400 ms debounce, then GET /stock/{sku}.
   * The stock state is the basis for the out-of-stock alert and the qty <= stock validation.
   */
  onSkuChange(event: Event): void {
    const sku = (event.target as HTMLInputElement).value.trim();

    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    this.stockSub?.unsubscribe();
    this.stockSub = null;

    // empty SKU -> we clear the stock preview
    if (!sku) {
      this.stockLoading.set(false);
      this.stockAvailable.set(null);
      this.stockError.set(null);
      return;
    }

    this.stockLoading.set(true);
    this.stockAvailable.set(null);
    this.stockError.set(null);

    this.debounceTimer = setTimeout(() => {
      this.stockSub = this.logisticsService.getStock(sku).subscribe({
        next: (qty) => {
          this.stockAvailable.set(qty);
          this.stockLoading.set(false);
        },
        error: (err) => {
          // GlobalExceptionHandler returns JSON {message,...} — parsed like in the other forms.
          let body = err?.error;
          if (typeof body === 'string') {
            try {
              const parsed = JSON.parse(body);
              this.stockError.set(parsed && typeof parsed.message === 'string' ? parsed.message : body || 'Could not load stock');
            } catch {
              this.stockError.set(body.length > 0 ? body : 'Could not load stock');
            }
          } else if (body && typeof body.message === 'string') {
            this.stockError.set(body.message);
          } else {
            this.stockError.set('Could not load stock');
          }
          this.stockLoading.set(false);
        },
      });
    }, 400);
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if (!this.canSubmit()) return;
    const m = this.issueModel();
    // payload mirrors the backend IssueRequest DTO: {sku, qty} — no location (FIFO)
    const payload = { sku: m.sku.trim(), qty: m.qty as number };

    this.logisticsService.issueGoods(payload).subscribe({
      next: (serverMessage) => {
        // message from the backend, e.g. "Warenausgang (FIFO) fuer SKU: X erfolgreich durchgefuert!"
        this.issueMessage.set(serverMessage ?? `Issue for ${payload.sku} successful`);
        this.issueError.set(null);
        // reset() clears the value + touched/dirty and syncs the input (FormField)
        this.issueForm().reset({ sku: '', qty: null });
        // we also clear the stock preview — after issuing it is stale
        this.stockAvailable.set(null);
        this.stockError.set(null);
      },
      error: (err) => {
        let body = err?.error;
        if (typeof body === 'string') {
          // responseType 'text' -> the body is a raw string, even on a 400.
          try {
            const parsed = JSON.parse(body);
            if (parsed && typeof parsed.message === 'string') {
              this.issueError.set(parsed.message);
              return;
            }
          } catch { /* not JSON -> plain-text error */ }
          this.issueError.set(body.length > 0 ? body : 'Could not issue goods');
        } else if (body && typeof body.message === 'string') {
          this.issueError.set(body.message);
        } else {
          this.issueError.set('Could not issue goods');
        }
      },
    });
  }
}
