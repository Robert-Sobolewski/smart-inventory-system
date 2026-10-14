import { Component, inject, signal, computed } from '@angular/core';
import { form, FormField, required, validate } from '@angular/forms/signals';
import { Subscription } from 'rxjs';
import { LogisticsService } from '../../services/logistics-service';

// Local form model: qty is number|null — <input type="number"> zwraca null dla pustego pola
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

  // form validation — strukturalna (required + integer >= 1).
  // Walidacja "qty <= stan" jest osobnym computed (stockAvailable), bo zależy od stanu z API.
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

  // --- Stock preview (alert braku towaru) -------------------------------------
  // Przy zmianie SKU pobieramy aktualny stan z GET /stock/{sku} (debounce 400 ms).
  readonly stockLoading = signal(false);
  readonly stockAvailable = signal<number | null>(null);
  readonly stockError = signal<string | null>(null);

  // qty > dostępny stan -> alert + submit zablokowany (decyzja v1: all-or-nothing)
  readonly overStock = computed(() => {
    const q = this.issueModel().qty;
    const s = this.stockAvailable();
    return q !== null && s !== null && !this.stockError() && q > s;
  });

  // Submit tylko gdy: formularz ważny + stan znany (nie ładuje się, nie ma błędu) + qty <= stan
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
   * Wywoływane z (change) na polu SKU — debounce 400 ms, potem GET /stock/{sku}.
   * Stan magazynowy jest podstawą alertu braku towaru i walidacji qty <= stan.
   */
  onSkuChange(event: Event): void {
    const sku = (event.target as HTMLInputElement).value.trim();

    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    this.stockSub?.unsubscribe();
    this.stockSub = null;

    // puste SKU -> czyścimy podgląd stanu
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
          // GlobalExceptionHandler zwraca JSON {message,...} — parsujemy jak w pozostałych formach.
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
    // payload mirrors the backend IssueRequest DTO: {sku, qty} — lokalizacji nie ma (FIFO)
    const payload = { sku: m.sku.trim(), qty: m.qty as number };

    this.logisticsService.issueGoods(payload).subscribe({
      next: (serverMessage) => {
        // komunikat z backendu, np. "Warenausgang (FIFO) fuer SKU: X erfolgreich durchgefuert!"
        this.issueMessage.set(serverMessage ?? `Issue for ${payload.sku} successful`);
        this.issueError.set(null);
        // reset() czyści wartość + touched/dirty i synchronizuje input (FormField)
        this.issueForm().reset({ sku: '', qty: null });
        // podgląd stanu też czyścimy — po wydaniu jest nieaktualny
        this.stockAvailable.set(null);
        this.stockError.set(null);
      },
      error: (err) => {
        let body = err?.error;
        if (typeof body === 'string') {
          // responseType 'text' -> body jest surowym stringiem, nawet przy 400.
          try {
            const parsed = JSON.parse(body);
            if (parsed && typeof parsed.message === 'string') {
              this.issueError.set(parsed.message);
              return;
            }
          } catch { /* nie jest JSON -> plain-text błąd */ }
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
