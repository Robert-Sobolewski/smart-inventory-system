import { Signal } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ProductList } from './product-list';
import { InventoryService } from '../../services/inventory-service';
import { Product } from '../../models/product';

// Lokalny kształt modelu formularza (w komponencie jest to prywatny interfejs)
interface ProductFormShape {
  sku: string;
  name: string;
  category: string;
  description: string;
}

// Widok testowy komponentu — odsłania protected members (jak w dashboard.spec.ts).
type TestableProductList = ProductList & {
  productModel: Signal<ProductFormShape>;
  saveError: Signal<string | null>;
  savedMessage: Signal<string | null>;
  productForm: () => { valid(): boolean; invalid(): boolean };
};

describe('ProductList (component)', () => {
  let service: InventoryService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [provideHttpClient()],
    }).compileComponents();
  });

  function createComponent(): TestableProductList {
    const fixture = TestBed.createComponent(ProductList);
    service = TestBed.inject(InventoryService);
    return fixture.componentInstance as unknown as TestableProductList;
  }

  // Stub eventu submit — preventDefault musi być prawdziwym spy (vi.fn), nie strzałką.
  function createSubmitEvent() {
    const event = { preventDefault: vi.fn() };
    return event as unknown as Event;
  }

  describe('form validation', () => {
    it('is invalid when the form is empty', () => {
      const c = createComponent();
      expect(c.productForm().invalid()).toBe(true);
    });

    it('becomes valid with sku and name filled in (category/description optional)', () => {
      const c = createComponent();
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });
      expect(c.productForm().valid()).toBe(true);
    });

    it('is invalid when the SKU is missing', () => {
      const c = createComponent();
      c.productModel.set({ sku: '', name: 'Resistor 10k Ohm', category: '', description: '' });
      expect(c.productForm().invalid()).toBe(true);
    });

    it('is invalid when the name is missing', () => {
      const c = createComponent();
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: '', category: '', description: '' });
      expect(c.productForm().invalid()).toBe(true);
    });
  });

  describe('onSubmit — success path', () => {
    it('sends a trimmed payload with empty optional fields mapped to undefined', () => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'addProduct').mockReturnValue(
        of('Produkt RES-10K-OHM-001 erfolgreich angelegt!'),
      );
      c.productModel.set({ sku: '  RES-10K-OHM-001  ', name: ' Resistor 10k Ohm ', category: '', description: '' });

      const event = createSubmitEvent();
      c.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledTimes(1);
      // puste opcjonalne pola -> undefined (backend zapisze NULL, nie "")
      expect(spy).toHaveBeenCalledWith({
        sku: 'RES-10K-OHM-001',
        name: 'Resistor 10k Ohm',
        category: undefined,
        description: undefined,
      });
    });

    it('shows the success message and resets the form after a successful save', () => {
      const c = createComponent();
      vi.spyOn(service, 'addProduct').mockReturnValue(
        of('Produkt RES-10K-OHM-001 erfolgreich angelegt!'),
      );
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: 'Passivbauteile', description: 'Technische Details' });

      c.onSubmit(createSubmitEvent());

      expect(c.savedMessage()).toBe('Product RES-10K-OHM-001 created.');
      expect(c.saveError()).toBeNull();
      // formularz czyszczony po sukcesie — gotowy na kolejny produkt
      expect(c.productModel()).toEqual({ sku: '', name: '', category: '', description: '' });
    });

    it('clears a previous error when the retry succeeds', () => {
      const c = createComponent();
      vi.spyOn(service, 'addProduct')
        .mockReturnValueOnce(throwError(() => ({ error: 'Produkt mit SKU RES-10K-OHM-001 existiert bereits' })))
        .mockReturnValueOnce(of('ok'));
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });

      c.onSubmit(createSubmitEvent()); // 1. próba — błąd z backendu
      expect(c.saveError()).toBe('Produkt mit SKU RES-10K-OHM-001 existiert bereits');

      c.productModel.set({ sku: 'RES-20K-OHM-001', name: 'Resistor 20k Ohm', category: '', description: '' });
      c.onSubmit(createSubmitEvent()); // 2. próba — sukces

      expect(c.saveError()).toBeNull();
      expect(c.savedMessage()).toBe('Product RES-20K-OHM-001 created.');
    });
  });

  describe('onSubmit — error path', () => {
    it('shows the plain-text server message (e.g. duplicate SKU)', () => {
      const c = createComponent();
      vi.spyOn(service, 'addProduct').mockReturnValue(
        throwError(() => ({ error: 'Produkt mit SKU RES-10K-OHM-001 existiert bereits' })),
      );
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });

      c.onSubmit(createSubmitEvent());

      expect(c.saveError()).toBe('Produkt mit SKU RES-10K-OHM-001 existiert bereits');
      expect(c.savedMessage()).toBeNull();
    });

    it('falls back to a generic message when the error body is not a string', () => {
      const c = createComponent();
      vi.spyOn(service, 'addProduct').mockReturnValue(
        throwError(() => ({ error: { message: 'Internal Server Error' } })),
      );
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });

      c.onSubmit(createSubmitEvent());

      expect(c.saveError()).toBe('Could not create product.');
    });

    it('falls back to "Unknown server error" when the response has no body at all', () => {
      const c = createComponent();
      vi.spyOn(service, 'addProduct').mockReturnValue(throwError(() => ({})));
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });

      c.onSubmit(createSubmitEvent());

      expect(c.saveError()).toBe('Unknown server error');
    });

    it('keeps the form data after a failed save (user can fix and retry)', () => {
      const c = createComponent();
      vi.spyOn(service, 'addProduct').mockReturnValue(
        throwError(() => ({ error: 'Produkt mit SKU RES-10K-OHM-001 existiert bereits' })),
      );
      c.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });

      c.onSubmit(createSubmitEvent());

      expect(c.productModel()).toEqual({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });
    });
  });

  describe('onSubmit — invalid form', () => {
    it('does not call addProduct when the form is invalid', () => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'addProduct').mockReturnValue(of('ok'));

      c.onSubmit(createSubmitEvent()); // pusty formularz -> invalid

      expect(spy).not.toHaveBeenCalled();
      expect(c.savedMessage()).toBeNull();
      expect(c.saveError()).toBeNull();
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Testy renderowania template'u (DOM) — InventoryService jest w pełni zmockowany,
// więc warstwa HTTP nie bierze udziału (wzór jak w dashboard.spec.ts).
// ─────────────────────────────────────────────────────────────────────────────
describe('ProductList (template rendering)', () => {
  let fixture: ComponentFixture<ProductList>;
  let component: ProductList & { productModel: Signal<{ sku: string; name: string; category: string; description: string }> };
  let reloadSpy: ReturnType<typeof vi.fn>;

  function setupDom(options: { rows?: Product[] | undefined; loading?: boolean; error?: unknown } = {}) {
    const { rows, loading = false, error = null } = options;
    reloadSpy = vi.fn();
    const mockInventoryService = {
      productResource: {
        value: () => rows,
        isLoading: () => loading,
        error: () => error,
        reload: reloadSpy,
      },
      addProduct: vi.fn().mockReturnValue(of('ok')),
    };
    TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [{ provide: InventoryService, useValue: mockInventoryService }],
    });
    fixture = TestBed.createComponent(ProductList);
    component = fixture.componentInstance as typeof component;
    fixture.detectChanges();
  }

  it('renders a product card with name and SKU for each product', () => {
    setupDom({
      rows: [
        { id: 1, sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: 'Passivbauteile' },
        { id: 2, sku: 'CAP-100N-002', name: 'Capacitor 100nF' },
      ],
    });

    const cards = fixture.nativeElement.querySelectorAll('.product-card');
    expect(cards.length).toBe(2);
    // brak kategorii -> placeholder "—" (template: product.category ?? '—')
    expect(cards[0].textContent).toContain('Resistor 10k Ohm');
    expect(cards[0].textContent).toContain('RES-10K-OHM-001');
    expect(cards[1].textContent).toContain('Capacitor 100nF');
    expect(cards[1].textContent).toContain('—');
  });

  it('shows the empty-state message when there are no products', () => {
    setupDom({ rows: [] });

    const el = fixture.nativeElement.querySelector('.empty-msg');
    expect(el?.textContent).toContain('Warehouse is empty.');
    expect(fixture.nativeElement.querySelectorAll('.product-card').length).toBe(0);
  });

  it('shows the loading state (and hides the grid) while fetching', () => {
    setupDom({ loading: true });

    const el = fixture.nativeElement.querySelector('.loading-state');
    expect(el?.textContent).toContain('Connecting with Spring Boot backend...');
    expect(fixture.nativeElement.querySelector('.product-grid')).toBeNull();
  });

  it('shows the error state and "Try again" triggers productResource.reload()', () => {
    setupDom({ error: new Error('boom') });

    const el = fixture.nativeElement.querySelector('.error-state');
    expect(el?.textContent).toContain('Connecting server error!');

    // act — kliknięcie przycisku retry w DOM
    (fixture.nativeElement.querySelector('.error-state button') as HTMLButtonElement).click();
    // assert
    expect(reloadSpy).toHaveBeenCalledTimes(1);
  });

  it('disables the submit button while the form is invalid, enables it when valid', () => {
    setupDom({ rows: [] });
    const btn = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;

    // pusty formularz -> disabled (sku i name wymagane)
    expect(btn.disabled).toBe(true);

    component.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });
    fixture.detectChanges();
    expect(btn.disabled).toBe(false);
  });

  it('renders the success message in the DOM after a successful save', () => {
    setupDom({ rows: [] });
    component.productModel.set({ sku: 'RES-10K-OHM-001', name: 'Resistor 10k Ohm', category: '', description: '' });
    fixture.detectChanges();

    // act — submit przez handler komponentu (jak w prawdziwym formularzu)
    const event = { preventDefault: vi.fn() } as unknown as Event;
    component.onSubmit(event);
    fixture.detectChanges();

    // assert — komunikat sukcesu widoczny dla użytkownika (role="status")
    const msg = fixture.nativeElement.querySelector('.success-msg');
    expect(msg?.textContent).toContain('Product RES-10K-OHM-001 created.');
  });
});
