import { provideHttpClient } from '@angular/common/http';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LogisticsService } from '../../services/logistics-service';
import { GoodsIssue } from './goods-issue';

describe('GoodsIssue (component)', () => {
  let service: LogisticsService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoodsIssue],
      providers: [provideHttpClient()],
    }).compileComponents();
  });

  function createComponent(): GoodsIssue {
    const fixture = TestBed.createComponent(GoodsIssue);
    service = TestBed.inject(LogisticsService);
    return fixture.componentInstance;
  }

  // helper: symuluje załadowany stan magazynowy (omija debounce z onSkuChange)
  function setStock(c: GoodsIssue, qty: number): void {
    c.stockAvailable.set(qty);
    c.stockLoading.set(false);
    c.stockError.set(null);
  }

  describe('form validation', () => {
    it('is invalid when the form is empty', () => {
      const c = createComponent();
      expect(c.issueForm().valid()).toBe(false);
    });

    it('becomes valid with sku and a whole quantity >= 1', () => {
      const c = createComponent();
      c.issueModel.set({ sku: 'SKU-001', qty: 2 });
      expect(c.issueForm().valid()).toBe(true);
    });

    it('is invalid when quantity is missing or not a whole number >= 1', () => {
      const c = createComponent();
      for (const bad of [null, 0, -1, 1.5]) {
        c.issueModel.set({ sku: 'SKU-001', qty: bad });
        expect(c.issueForm().valid(), `qty=${String(bad)}`).toBe(false);
      }
    });
  });

  describe('canSubmit (stock-aware gate)', () => {
    it('is false while stock is loading', () => {
      const c = createComponent();
      c.issueModel.set({ sku: 'SKU-001', qty: 2 });
      c.stockLoading.set(true);
      expect(c.canSubmit()).toBe(false);
    });

    it('is false when stock is unknown (null)', () => {
      const c = createComponent();
      c.issueModel.set({ sku: 'SKU-001', qty: 2 });
      // stockAvailable pozostaje null (domyślnie)
      expect(c.canSubmit()).toBe(false);
    });

    it('is false when the stock lookup failed', () => {
      const c = createComponent();
      c.issueModel.set({ sku: 'SKU-001', qty: 2 });
      setStock(c, 10);
      c.stockError.set('Could not load stock');
      expect(c.canSubmit()).toBe(false);
    });

    it('is false when quantity exceeds available stock (over-stock)', () => {
      const c = createComponent();
      c.issueModel.set({ sku: 'SKU-001', qty: 11 });
      setStock(c, 10);
      expect(c.overStock()).toBe(true);
      expect(c.canSubmit()).toBe(false);
    });

    it('is true when the form is valid and quantity fits within stock', () => {
      const c = createComponent();
      c.issueModel.set({ sku: 'SKU-001', qty: 5 });
      setStock(c, 10);
      expect(c.canSubmit()).toBe(true);
    });

    it('allows issuing the full available stock (qty == stock)', () => {
      const c = createComponent();
      c.issueModel.set({ sku: 'SKU-001', qty: 10 });
      setStock(c, 10);
      expect(c.overStock()).toBe(false);
      expect(c.canSubmit()).toBe(true);
    });
  });

  describe('onSubmit — success path', () => {
    it('calls issueGoods with trimmed SKU and quantity', async () => {
      const c = createComponent();
      setStock(c, 10);
      const spy = vi.spyOn(service, 'issueGoods').mockReturnValue(of('Issued 5 units of SKU-001'));
      c.issueModel.set({ sku: '  SKU-001  ', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith({ sku: 'SKU-001', qty: 5 });
    });

    it('shows the server message, resets form and clears stock preview after success', async () => {
      const c = createComponent();
      setStock(c, 10);
      vi.spyOn(service, 'issueGoods').mockReturnValue(of('Issued 2 units of SKU-001'));
      c.issueModel.set({ sku: 'SKU-001', qty: 2 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.issueMessage()).toBe('Issued 2 units of SKU-001');
      expect(c.issueError()).toBeNull();
      expect(c.issueModel()).toEqual({ sku: '', qty: null });
      // podgląd stanu wyczyszczony po sukcesie — następne SKU pobierze świeży stan
      expect(c.stockAvailable()).toBeNull();
    });
  });

  describe('onSubmit — error path', () => {
    it('shows the server message for a JSON error body and keeps the form data', async () => {
      const c = createComponent();
      setStock(c, 10);
      vi.spyOn(service, 'issueGoods').mockReturnValue(
        throwError(() => ({ error: { message: 'Not enough stock (available: 3)' } })),
      );
      c.issueModel.set({ sku: 'SKU-001', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.issueError()).toBe('Not enough stock (available: 3)');
      expect(c.issueMessage()).toBeNull();
      // dane zostają w formularzu — użytkownik może poprawić i wysłać ponownie
      expect(c.issueModel()).toEqual({ sku: 'SKU-001', qty: 5 });
    });

    it('shows the raw text for a plain-text error body', async () => {
      const c = createComponent();
      setStock(c, 10);
      vi.spyOn(service, 'issueGoods').mockReturnValue(
        throwError(() => ({ error: 'SKU-001 not found in stock' })),
      );
      c.issueModel.set({ sku: 'SKU-001', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.issueError()).toBe('SKU-001 not found in stock');
    });

    it('shows a default message when the error body is empty', async () => {
      const c = createComponent();
      setStock(c, 10);
      vi.spyOn(service, 'issueGoods').mockReturnValue(throwError(() => ({ error: '' })));
      c.issueModel.set({ sku: 'SKU-001', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.issueError()).toBe('Could not issue goods');
    });
  });

  describe('onSubmit — blocked by canSubmit', () => {
    it('does not call issueGoods when stock is unknown', async () => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'issueGoods').mockReturnValue(of('ok'));
      c.issueModel.set({ sku: 'SKU-001', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event); // stockAvailable === null -> canSubmit false

      expect(spy).not.toHaveBeenCalled();
    });

    it('does not call issueGoods when quantity exceeds stock', async () => {
      const c = createComponent();
      setStock(c, 3);
      const spy = vi.spyOn(service, 'issueGoods').mockReturnValue(of('ok'));
      c.issueModel.set({ sku: 'SKU-001', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('onSkuChange (stock preview with debounce)', () => {
    it('clears the stock preview when SKU is empty', () => {
      const c = createComponent();
      setStock(c, 10);
      c.stockLoading.set(true);

      c.onSkuChange({ target: { value: '   ' } } as unknown as Event);

      expect(c.stockAvailable()).toBeNull();
      expect(c.stockLoading()).toBe(false);
      expect(c.stockError()).toBeNull();
    });

    it('fetches stock after the 400 ms debounce for a non-empty SKU', fakeAsync(() => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'getStock').mockReturnValue(of(7));

      c.onSkuChange({ target: { value: 'SKU-001' } } as unknown as Event);

      // przed debounce: załadowanie w toku, request jeszcze nie wyszedł
      expect(c.stockLoading()).toBe(true);
      expect(spy).not.toHaveBeenCalled();

      tick(400);

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith('SKU-001');
      expect(c.stockAvailable()).toBe(7);
      expect(c.stockLoading()).toBe(false);
    }));

    it('trims the SKU before fetching stock', fakeAsync(() => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'getStock').mockReturnValue(of(0));

      c.onSkuChange({ target: { value: '  SKU-002  ' } } as unknown as Event);
      tick(400);

      expect(spy).toHaveBeenCalledWith('SKU-002');
    }));

    it('cancels the pending fetch when the SKU changes again within 400 ms', fakeAsync(() => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'getStock').mockReturnValue(of(1));

      c.onSkuChange({ target: { value: 'SKU-0' } } as unknown as Event);
      tick(200);
      c.onSkuChange({ target: { value: 'SKU-001' } } as unknown as Event);
      tick(400);

      // tylko ostatnie SKU zostało pobrane — pierwsze odwołane (clearTimeout)
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith('SKU-001');
    }));
  });
});
