import { Component, inject, signal } from '@angular/core';
import { email, form, FormField, required } from '@angular/forms/signals';
import { SupplierService } from '../../services/supplier-service';
import { Supplier, SupplierRequest } from '../../models/supplier';

// Local form model: all fields are plain strings (empty = not provided)
interface SupplierFormModel {
  name: string;
  email: string;
  address: string;
}

@Component({
  imports: [FormField],
  selector: 'app-supplier-list',
  styleUrl: './supplier-list.scss',
  templateUrl: './supplier-list.html',
})
export class SupplierList {

  private readonly supplierService = inject(SupplierService);

  // Reactive list of all suppliers (auto-tracked by Angular)
  readonly supplierResource = this.supplierService.supplierResource;

  // form model for 'add supplier'
  readonly supplierModel = signal<SupplierFormModel>({ name: '', email: '', address: '' });

  // form validation
  readonly supplierForm = form(this.supplierModel, f => {
    required(f.name, { message: 'Name is required' });
    required(f.email, { message: 'Email is required' });
    email(f.email);
  });

  // Feedback messages after a submit attempt
  readonly createError = signal<string | null>(null);
  readonly createMessage = signal<string | null>(null);

  // Edit state: which row is currently being edited + the values being typed
  readonly editingId = signal<number | null>(null);
  readonly editName = signal('');
  readonly editEmail = signal('');
  readonly editAddress = signal('');
  readonly updateError = signal<string | null>(null);
  readonly updateMessage = signal<string | null>(null);

  onEdit(supplier: Supplier): void {
    this.editingId.set(supplier.id ?? null);
    this.editName.set(supplier.name ?? '');
    this.editEmail.set(supplier.email ?? '');
    this.editAddress.set(supplier.address ?? '');
    this.updateError.set(null);
    this.updateMessage.set(null);
  }

  onCancelEdit(): void {
    this.editingId.set(null);
    this.editName.set('');
    this.editEmail.set('');
    this.editAddress.set('');
  }

  onEditNameChange(event: Event): void {
    this.editName.set((event.target as HTMLInputElement).value);
  }

  onEditEmailChange(event: Event): void {
    this.editEmail.set((event.target as HTMLInputElement).value);
  }

  onEditAddressChange(event: Event): void {
    this.editAddress.set((event.target as HTMLInputElement).value);
  }

  onSaveEdit(id: number): void {
    const name = this.editName().trim();
    const emailValue = this.editEmail().trim();
    if (!name || !emailValue) {
      // Backend update() oczekuje non-null email (w przeciwnym razie NPE) -> walidacja po stronie klienta
      this.updateError.set("Name and email can't be empty");
      return;
    }

    const payload: SupplierRequest = { name, email: emailValue, address: this.editAddress().trim() };

    this.supplierService.updateSupplier(id, payload).subscribe({
      next: (updated) => {
        // PUT zwraca zaktualizowany obiekt Supplier (JSON), nie plain text
        this.updateMessage.set(`Lieferant ${updated.name} erfolgreich aktualisiert`);
        this.updateError.set(null);
        // Refresh the list so the new values show up immediately
        this.supplierResource.reload();
        this.editingId.set(null);
        this.editName.set('');
        this.editEmail.set('');
        this.editAddress.set('');
      },
      error: (err) => {
        // responseType 'json' -> err.error jest już sparsowanym obiektem; helper toleruje też stringi
        this.updateError.set(this.extractErrorMessage(err, 'Could not update supplier'));
      }
    });
  }

  onDeleteSupplier(supplier: Supplier): void {
    // Delete is destructive -> ask for confirmation first
    const confirmed = window.confirm(`Really delete supplier ${supplier.name}?`);
    if (!confirmed) return;

    this.supplierService.deleteSupplier(supplier.id!).subscribe({
      next: (serverMessage) => {
        this.updateMessage.set(serverMessage ?? `Lieferant ${supplier.name} geloescht`);
        this.updateError.set(null);
        // Refresh the list so the row disappears immediately
        this.supplierResource.reload();
      },
      error: (err) => {
        // responseType 'text' -> body jest surowym stringiem, nawet przy 400.
        this.updateError.set(this.extractErrorMessage(err, 'Could not delete supplier'));
      }
    });
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if (this.supplierForm().invalid()) return;

    const m = this.supplierModel();
    // Map empty address to undefined so the backend stores NULL, not ""
    const payload: SupplierRequest = {
      name: m.name.trim(),
      email: m.email.trim(),
      address: m.address.trim() || undefined
    };

    this.supplierService.createSupplier(payload).subscribe({
      next: (serverMessage) => {
        // komunikat z backendu, np. "Lieferant ACME GmbH erfolgreich angelegt (ID: 1)"
        this.createMessage.set(serverMessage ?? `Supplier ${payload.name} created`);
        this.createError.set(null);
        // Refresh the list so the new supplier shows up immediately
        this.supplierResource.reload();
        // reset() czyści wartość + touched/dirty i synchronizuje input (FormField)
        this.supplierForm().reset({ name: '', email: '', address: '' });
      },
      error: (err) => {
        // responseType 'text' -> body jest surowym stringiem, nawet przy 400.
        // GlobalExceptionHandler zwraca JSON {message,...} - helper parsuje go.
        this.createError.set(this.extractErrorMessage(err, 'Could not create supplier'));
      }
    });
  }

  /**
   * Extracts a human-readable message from an HttpClient error.
   * Handles both raw string bodies (responseType 'text' — even on 400) and
   * already-parsed JSON objects ({message, ...} from GlobalExceptionHandler).
   */
  private extractErrorMessage(err: unknown, fallback: string): string {
    const body = (err as { error?: unknown })?.error;

    // 1. Raw string body (responseType 'text') — may be plain text or JSON in a string
    if (typeof body === 'string') {
      try {
        const parsed = JSON.parse(body) as { message?: unknown };
        if (parsed && typeof parsed.message === 'string' && parsed.message.length > 0) {
          return parsed.message;
        }
      } catch { /* nie jest JSON -> plain-text błąd */ }
      return body.length > 0 ? body : fallback;
    }

    // 2. Parsed object (responseType 'json') — GlobalExceptionHandler shape: {message, ...}
    if (body && typeof body === 'object') {
      const msg = (body as { message?: unknown }).message ?? (body as { error?: unknown }).error;
      if (typeof msg === 'string' && msg.length > 0) return msg;
    }

    // 3. Fallback for everything else (null, undefined, numbers)
    return fallback;
  }
}
