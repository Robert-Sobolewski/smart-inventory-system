import { Component, inject, signal } from '@angular/core';
import { form, FormField, required } from '@angular/forms/signals';
import { AddLocationService } from '../../services/add-location-service';

interface LocationFormModel{
  locationCode: string
}

@Component({
  imports: [FormField],
  selector: 'app-add-location',
  styleUrl: './add-location.scss',
  templateUrl: './add-location.html',
})
export class AddLocation {

  private readonly locationService = inject(AddLocationService);

  // Reactive list of all locations (auto-tracked by Angular)
  readonly locationResource = this.locationService.locationResource;

  // form model for 'add location'
  readonly locationModel = signal<LocationFormModel>({locationCode:""});

  // form validation
  readonly locationForm = form(this.locationModel,f=>{
    required(f.locationCode,{message: "Location is required"})
  });

  // Feedback messages after a submit attempt
  readonly createError = signal<string | null>(null);
  readonly createMessage = signal<string | null>(null);

  // Edit state: which row is currently being edited + the code being typed
  readonly editingId = signal<number | null>(null);
  readonly editCode = signal('');
  readonly updateError = signal<string | null>(null);
  readonly updateMessage = signal<string | null>(null);

  onEdit(location: { id: number; locationCode: string }): void {
    this.editingId.set(location.id);
    this.editCode.set(location.locationCode);
    this.updateError.set(null);
    this.updateMessage.set(null);
  }

  onCancelEdit(): void {
    this.editingId.set(null);
    this.editCode.set('');
  }

  onEditCodeChange(event: Event): void {
    this.editCode.set((event.target as HTMLInputElement).value);
  }

  onSaveEdit(id: number): void {
    const code = this.editCode().trim();
    if (!code) {
      this.updateError.set("Location code can't be empty");
      return;
    }

    this.locationService.updateLocation(id, code).subscribe({
      next: (serverMessage) => {
        this.updateMessage.set(serverMessage ?? `Location ${code} updated`);
        this.updateError.set(null);
        // Refresh the list so the new code shows up immediately
        this.locationResource.reload();
        this.editingId.set(null);
        this.editCode.set('');
      },
      error: (err) => {
        let body = err?.error;
        if (typeof body === 'string') {
          // responseType 'text' -> body jest surowym stringiem, nawet przy 400.
          try {
            const parsed = JSON.parse(body);
            if (parsed && typeof parsed.message === 'string') {
              this.updateError.set(parsed.message);
              return;
            }
          } catch { /* nie jest JSON -> plain-text błąd */ }
          this.updateError.set(body.length > 0 ? body : "Could not update location");
        } else if (body && typeof body.message === 'string') {
          this.updateError.set(body.message);
        } else {
          this.updateError.set("Could not update location");
        }
      }
    });
  }

  onDeleteLocation(location: { id: number; locationCode: string }): void {
    // Delete is destructive -> ask for confirmation first
    const confirmed = window.confirm(`Really delete location ${location.locationCode}?`);
    if (!confirmed) return;

    this.locationService.deleteLocation(location.id).subscribe({
      next: (serverMessage) => {
        this.updateMessage.set(serverMessage ?? `Location ${location.locationCode} deleted`);
        this.updateError.set(null);
        // Refresh the list so the row disappears immediately
        this.locationResource.reload();
      },
      error: (err) => {
       let body = err?.error;

       // 1. Handle String (Raw or JSON)
       if (typeof body === 'string') {
         try {
           const parsed = JSON.parse(body);
           if (parsed && typeof parsed.message === 'string') {
             this.updateError.set(parsed.message);
             return;
           }
         } catch {
           // Not JSON, treat as plain text
         }
         this.updateError.set(body.length > 0 ? body : 'Could not delete location');
       }
       // 2. Handle Object (e.g., { message: "..." } or { error: "..." })
       else if (body && typeof body === 'object') {
         // Check for common keys: .message, .error, or .description
         const msg = body.message || body.error || body.description;
         if (typeof msg === 'string' && msg.length > 0) {
           this.updateError.set(msg);
         } else {
           this.updateError.set('Could not delete location');
         }
       }
       // 3. Fallback for everything else (null, undefined, numbers)
       else {
         this.updateError.set('Could not delete location');
       }
      }
    });
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if(this.locationForm().invalid()) return;
    const payload : string = this.locationModel().locationCode.trim();

    this.locationService.createLocation(payload).subscribe({
      next: (serverMessage) => {
        // komunikat z backendu, np. "Lagerplatz A-1-01 wurde erfolgreich angelegt."
        this.createMessage.set(serverMessage ?? `Location ${payload} successful created`);
        this.createError.set(null);
        // Refresh the list so the new location shows up immediately
        this.locationResource.reload();
        // reset() czyści wartość + touched/dirty i synchronizuje input (FormField)
        this.locationForm().reset({locationCode: ""});
      },
      error: (err)=>{
        let body = err?.error;
        if(typeof body === 'string'){
          // responseType 'text' -> body jest surowym stringiem, nawet przy 400.
          // GlobalExceptionHandler zwraca JSON {message,...} - spróbuj go sparsować.
          try{
            const parsed = JSON.parse(body);
            if(parsed && typeof parsed.message === 'string'){
              this.createError.set(parsed.message);
              return;
            }
          }catch{ /* nie jest JSON -> plain-text błąd */ }
          this.createError.set(body.length > 0 ? body : "Could not create location");
        } else if(body && typeof body.message === 'string'){
          // obiekt JSON (np. gdyby responseType był 'json')
          this.createError.set(body.message);
        } else {
          this.createError.set("Could not create location");
        }
      }
    })

  }
}
