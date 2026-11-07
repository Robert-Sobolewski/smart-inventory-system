import { TestBed } from '@angular/core/testing';
import { DashboardService, StockSummaryItem } from '../../services/dashboard-service';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;

  // helper — a row with sensible default values + overriding specific fields
  function row(overrides: Partial<StockSummaryItem> = {}): StockSummaryItem {
    return {
      sku: 'SKU-1',
      name: 'Produkt',
      category: null,
      totalQuantity: 0,
      minStockLevel: 5,
      lowStock: false,
      ...overrides,
    };
  }

  // The component reads the resource ONLY via .value() — the mock returns controlled data.
  // undefined = the httpResource state before the HTTP response (initial state).
  function setup(rows: StockSummaryItem[] | undefined) {
    const mockDashboardService = {
      stockOverviewResource: { value: () => rows },
      locationCountResource: { value: () => 0 },
    };
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardService, useValue: mockDashboardService }],
    });
    component = TestBed.createComponent(Dashboard).componentInstance;
  }

  it('should create the component', () => {
    setup(undefined);
    expect(component).toBeTruthy();
  });

  describe('totalUnits (computed)', () => {
    it('returns null before data arrives', () => {
      // act
      setup(undefined);
      // assert
      expect((component as any).totalUnits()).toBeNull();
    });

    it('sums totalQuantity of all rows', () => {
      // act
      setup([row({ sku: 'A-100', totalQuantity: 10 }), row({ sku: 'B-200', totalQuantity: 5 })]);
      // assert
      expect((component as any).totalUnits()).toBe(15);
    });
  });

  describe('lowStockCount (computed)', () => {
    it('returns null before data arrives', () => {
      // act
      setup(undefined);
      // assert
      expect((component as any).lowStockCount()).toBeNull();
    });

    it('counts only rows flagged lowStock', () => {
      // act
      setup([
        row({ sku: 'A-100', lowStock: true }),
        row({ sku: 'B-200' }),
        row({ sku: 'C-300', lowStock: true }),
      ]);
      // assert
      expect((component as any).lowStockCount()).toBe(2);
    });
  });

  describe('sortedRows (computed)', () => {
    it('returns empty array before data arrives', () => {
      // act
      setup(undefined);
      // assert
      expect((component as any).sortedRows()).toEqual([]);
    });

    it('sorts low-stock first, then by sku alphabetically', () => {
      // act — input deliberately "shuffled"
      setup([
        row({ sku: 'B-100' }),
        row({ sku: 'A-200', lowStock: true }),
        row({ sku: 'C-300', lowStock: true }),
      ]);
      // assert — low-stock (A, C) before normal (B); within a group ordered by SKU
      expect((component as any).sortedRows().map((r: StockSummaryItem) => r.sku)).toEqual([
        'A-200',
        'C-300',
        'B-100',
      ]);
    });

    it('does not mutate the source array (copy before sort)', () => {
      // setup — we keep a reference to the source to verify that sort does not mutate it
      const rows = [row({ sku: 'B-100' }), row({ sku: 'A-200', lowStock: true })];
      setup(rows);
      // act
      (component as any).sortedRows();
      // assert — the source keeps its original order ([...rows].sort in the component)
      expect(rows.map((r) => r.sku)).toEqual(['B-100', 'A-200']);
    });
  });
});
