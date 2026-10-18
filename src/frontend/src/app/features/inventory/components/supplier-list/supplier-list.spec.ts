import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { SupplierService } from '../../services/supplier-service';
import { SupplierList } from './supplier-list';

// typed view for testing
type TestableSupplierList = SupplierList & {
  supplierModel: WritableSignal<any>;
  supplierResource: { reload: () => void };
  createError: WritableSignal<string | null>;
  createMessage: WritableSignal<string | null>;
  editingId: Signal<number | null>;
  editName: Signal<string>;
  editEmail: Signal<string>;
  editAddress: Signal<string>;
  updateError: WritableSignal<string | null>;
  updateMessage: WritableSignal<string | null>;
  supplierForm: any; // form signals API
};

describe('SupplierList', () => {
  let component: TestableSupplierList;
  let fixture: ComponentFixture<SupplierList>;
  let mockSupplierService: any;
  // mutable data behind the mocked httpResource — lets DOM tests render a list
  let suppliersData: any[] = [];

  beforeEach(async () => {
    // PITFALL: metody wymagające toHaveBeenCalled muszą być vi.fn() spies, nie arrow functions
    mockSupplierService = {
      supplierResource: {
        reload: vi.fn(),
        isLoading: () => false,
        error: () => null,
        value: () => suppliersData,
      },
      createSupplier: vi.fn(),
      updateSupplier: vi.fn(),
      deleteSupplier: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [SupplierList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SupplierService, useValue: mockSupplierService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SupplierList);
    component = fixture.componentInstance as unknown as TestableSupplierList;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Rendering', () => {
    it('should render the empty state when there are no suppliers', () => {
      suppliersData = [];
      fixture.detectChanges();

      const emptyMsg = fixture.nativeElement.querySelector('.empty-msg');
      expect(emptyMsg?.textContent).toContain('No suppliers yet.');
    });

    it('should render one table row per supplier from the resource', () => {
      suppliersData = [
        { id: 1, name: 'ACME GmbH', email: 'sales@acme.de', address: null },
        { id: 2, name: 'Beta AG', email: 'info@beta.ch', address: 'Musterstr. 1' },
      ];
      fixture.detectChanges();

      const rows = fixture.nativeElement.querySelectorAll('tbody tr');
      expect(rows.length).toBe(2);
      expect(rows[0].textContent).toContain('ACME GmbH');
      expect(rows[0].textContent).toContain('sales@acme.de');
      expect(rows[1].textContent).toContain('Beta AG');
    });
  });

  describe('Create flow', () => {
    it('should submit a valid form, show the server message and reload the resource', () => {
      // setup
      component.supplierModel.set({ name: ' ACME GmbH ', email: 'sales@acme.de', address: '' });
      mockSupplierService.createSupplier.mockReturnValue(
        of('Lieferant ACME GmbH erfolgreich angelegt (ID: 1)'),
      );

      // act
      component.onSubmit(new Event('submit'));

      // assert — pusty adres mapowany na undefined, reszta przycięta
      expect(mockSupplierService.createSupplier).toHaveBeenCalledWith({
        name: 'ACME GmbH',
        email: 'sales@acme.de',
      });
      expect(component.createMessage()).toBe('Lieferant ACME GmbH erfolgreich angelegt (ID: 1)');
      expect(component.createError()).toBe(null);
      expect(mockSupplierService.supplierResource.reload).toHaveBeenCalled();
      // formularz zresetowany po sukcesie
      expect(component.supplierModel().name).toBe('');
    });

    it('should not call the service when the form is invalid', () => {
      component.supplierModel.set({ name: '', email: '', address: '' });

      component.onSubmit(new Event('submit'));

      expect(mockSupplierService.createSupplier).not.toHaveBeenCalled();
    });

    it('should parse a JSON error body (raw string) from the GlobalExceptionHandler', () => {
      // responseType 'text' -> nawet przy 400 body jest surowym stringiem z JSON-em w środku
      component.supplierModel.set({ name: 'ACME GmbH', email: 'sales@acme.de', address: '' });
      mockSupplierService.createSupplier.mockReturnValue(
        throwError(() => ({ error: JSON.stringify({ message: 'Dostawca z adresem e-mail sales@acme.de już istnieje' }) })),
      );

      component.onSubmit(new Event('submit'));

      expect(component.createError()).toBe('Dostawca z adresem e-mail sales@acme.de już istnieje');
    });

    it('should fall back to the raw text for non-JSON error bodies', () => {
      component.supplierModel.set({ name: 'ACME GmbH', email: 'sales@acme.de', address: '' });
      mockSupplierService.createSupplier.mockReturnValue(
        throwError(() => ({ error: 'Plain text error' })),
      );

      component.onSubmit(new Event('submit'));

      expect(component.createError()).toBe('Plain text error');
    });
  });

  describe('Edit Actions', () => {
    it('should set editing state on onEdit', () => {
      const mockSupplier = { id: 1, name: 'ACME GmbH', email: 'sales@acme.de', address: null };

      component.onEdit(mockSupplier);

      expect(component.editingId()).toBe(1);
      expect(component.editName()).toBe('ACME GmbH');
      expect(component.editEmail()).toBe('sales@acme.de');
      expect(component.editAddress()).toBe('');
    });

    it('should reset editing state on onCancelEdit', () => {
      component.onEdit({ id: 1, name: 'ACME GmbH', email: 'x@y.de', address: null });

      component.onCancelEdit();

      expect(component.editingId()).toBeNull();
      expect(component.editName()).toBe('');
      expect(component.editEmail()).toBe('');
      expect(component.editAddress()).toBe('');
    });

    it('should reject an empty name or email without calling the service', () => {
      component.onEdit({ id: 1, name: 'ACME GmbH', email: 'x@y.de', address: null });
      component.editName.set('   ');

      component.onSaveEdit(1);

      expect(component.updateError()).toBe("Name and email can't be empty");
      expect(mockSupplierService.updateSupplier).not.toHaveBeenCalled();
    });

    it('should save the edit, show a message from the returned object and reload', () => {
      component.onEdit({ id: 1, name: 'ACME GmbH', email: 'x@y.de', address: null });
      component.editName.set('ACME AG');
      // PUT zwraca obiekt Supplier (JSON), nie plain text
      mockSupplierService.updateSupplier.mockReturnValue(
        of({ id: 1, name: 'ACME AG', email: 'x@y.de', address: '' }),
      );

      component.onSaveEdit(1);

      expect(mockSupplierService.updateSupplier).toHaveBeenCalledWith(1, {
        name: 'ACME AG',
        email: 'x@y.de',
        address: '',
      });
      expect(component.updateMessage()).toBe('Lieferant ACME AG erfolgreich aktualisiert');
      expect(component.updateError()).toBe(null);
      expect(mockSupplierService.supplierResource.reload).toHaveBeenCalled();
      expect(component.editingId()).toBeNull();
    });

    it('should show the message from a parsed JSON error object (responseType json)', () => {
      component.onEdit({ id: 1, name: 'ACME GmbH', email: 'x@y.de', address: null });
      mockSupplierService.updateSupplier.mockReturnValue(
        throwError(() => ({ error: { message: 'Adres e-mail x@y.de jest już zajęty przez innego dostawcę' } })),
      );

      component.onSaveEdit(1);

      expect(component.updateError()).toBe('Adres e-mail x@y.de jest już zajęty przez innego dostawcę');
    });
  });

  describe('Delete flow', () => {
    let confirmSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
      confirmSpy = vi.spyOn(window, 'confirm');
    });

    afterEach(() => {
      confirmSpy.mockRestore();
    });

    it('should not delete when the user cancels the confirmation', () => {
      confirmSpy.mockReturnValue(false);

      component.onDeleteSupplier({ id: 1, name: 'ACME GmbH' });

      expect(mockSupplierService.deleteSupplier).not.toHaveBeenCalled();
    });

    it('should delete on confirmation and reload the resource', () => {
      confirmSpy.mockReturnValue(true);
      mockSupplierService.deleteSupplier.mockReturnValue(of('Lieferant mit ID 1 geloescht'));

      component.onDeleteSupplier({ id: 1, name: 'ACME GmbH' });

      expect(mockSupplierService.deleteSupplier).toHaveBeenCalledWith(1);
      expect(component.updateMessage()).toBe('Lieferant mit ID 1 geloescht');
      expect(mockSupplierService.supplierResource.reload).toHaveBeenCalled();
    });

    it('should parse a JSON error body (raw string) on delete failure', () => {
      confirmSpy.mockReturnValue(true);
      mockSupplierService.deleteSupplier.mockReturnValue(
        throwError(() => ({ error: JSON.stringify({ message: 'Dostawca z ID 99 nie został znaleziony' }) })),
      );

      component.onDeleteSupplier({ id: 99, name: 'Ghost' });

      expect(component.updateError()).toBe('Dostawca z ID 99 nie został znaleziony');
    });
  });
});
