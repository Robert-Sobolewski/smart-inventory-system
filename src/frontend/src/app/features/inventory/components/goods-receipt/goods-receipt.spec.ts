import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { GoodsReceipt } from './goods-receipt';
import { LogisticsService } from '../../services/logistics-service';

describe('GoodsReceipt (component)', () => {
  let service: LogisticsService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoodsReceipt],
      providers: [provideHttpClient()],
    }).compileComponents();
  });

  function createComponent(): GoodsReceipt {
    const fixture = TestBed.createComponent(GoodsReceipt);
    service = TestBed.inject(LogisticsService);
    return fixture.componentInstance;
  }

  describe('form validation', () => {
    it('is invalid when the form is empty', () => {
      const c = createComponent();
      expect(c.receiptForm().valid()).toBe(false);
    });

    it('becomes valid with sku, location and a whole quantity >= 1', () => {
      const c = createComponent();
      c.receiptModel.set({ sku: 'SKU-001', loc: 'A1', qty: 5 });
      expect(c.receiptForm().valid()).toBe(true);
    });

    it('is invalid when the location is missing', () => {
      const c = createComponent();
      c.receiptModel.set({ sku: 'SKU-001', loc: '', qty: 5 });
      expect(c.receiptForm().valid()).toBe(false);
    });

    it('is invalid when quantity is missing (null)', () => {
      const c = createComponent();
      c.receiptModel.set({ sku: 'SKU-001', loc: 'A1', qty: null });
      expect(c.receiptForm().valid()).toBe(false);
    });

    it('is invalid when quantity is not a whole number or < 1', () => {
      const c = createComponent();
      for (const bad of [0, -3, 2.5]) {
        c.receiptModel.set({ sku: 'SKU-001', loc: 'A1', qty: bad });
        expect(c.receiptForm().valid(), `qty=${bad}`).toBe(false);
      }
    });
  });

  describe('onSubmit — success path', () => {
    it('calls receiveGoods with trimmed SKU, location and quantity', async () => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'receiveGoods').mockReturnValue(
        of('Received 5 units of SKU-001 into A1'),
      );
      c.receiptModel.set({ sku: '  SKU-001  ', loc: 'A1', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith({ sku: 'SKU-001', loc: 'A1', qty: 5 });
    });

    it('shows the server message and resets the form after success', async () => {
      const c = createComponent();
      vi.spyOn(service, 'receiveGoods').mockReturnValue(
        of('Received 3 units of SKU-002 into B2'),
      );
      c.receiptModel.set({ sku: 'SKU-002', loc: 'B2', qty: 3 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.receiveMessage()).toBe('Received 3 units of SKU-002 into B2');
      expect(c.receiveError()).toBeNull();
      // formularz zresetowany — gotowy na kolejny wpis
      expect(c.receiptModel()).toEqual({ sku: '', loc: '', qty: null });
    });

    it('keeps an empty server body as-is (no crash)', async () => {
      const c = createComponent();
      vi.spyOn(service, 'receiveGoods').mockReturnValue(of(''));
      c.receiptModel.set({ sku: 'SKU-001', loc: 'A1', qty: 1 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.receiveMessage()).toBe('');
    });
  });

  describe('onSubmit — error path', () => {
    it('shows the server message for a JSON error body and keeps the form data', async () => {
      const c = createComponent();
      vi.spyOn(service, 'receiveGoods').mockReturnValue(
        throwError(() => ({ error: { message: 'Location A1 does not exist' } })),
      );
      c.receiptModel.set({ sku: 'SKU-001', loc: 'A1', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.receiveError()).toBe('Location A1 does not exist');
      expect(c.receiveMessage()).toBeNull();
      // dane zostają w formularzu — użytkownik może poprawić i wysłać ponownie
      expect(c.receiptModel()).toEqual({ sku: 'SKU-001', loc: 'A1', qty: 5 });
    });

    it('shows the raw text for a plain-text error body (e.g. 409)', async () => {
      const c = createComponent();
      vi.spyOn(service, 'receiveGoods').mockReturnValue(
        throwError(() => ({ error: 'SKU-001 already exists in location A1' })),
      );
      c.receiptModel.set({ sku: 'SKU-001', loc: 'A1', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.receiveError()).toBe('SKU-001 already exists in location A1');
    });

    it('shows a default message when the error body is empty', async () => {
      const c = createComponent();
      vi.spyOn(service, 'receiveGoods').mockReturnValue(throwError(() => ({ error: '' })));
      c.receiptModel.set({ sku: 'SKU-001', loc: 'A1', qty: 5 });

      await c.onSubmit({ preventDefault() {} } as unknown as Event);

      expect(c.receiveError()).toBe('Could not receive goods');
    });
  });

  describe('onSubmit — invalid form', () => {
    it('does not call receiveGoods when the form is invalid', async () => {
      const c = createComponent();
      const spy = vi.spyOn(service, 'receiveGoods').mockReturnValue(of('ok'));

      await c.onSubmit({ preventDefault() {} } as unknown as Event); // pusty formularz -> invalid

      expect(spy).not.toHaveBeenCalled();
    });
  });
});
