import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { GoodsIssue } from './goods-issue';

// The component keeps its signals `protected` (encapsulation — only the template
// and the class itself may touch them). In tests we cast to a small typed view so
// the compiler still checks every access. No `any`.
interface IssueFormModel {
  sku: string;
  qty: number | null;
}

type TestableGoodsIssue = {
  overStock: import('@angular/core').Signal<boolean>;
  canSubmit: import('@angular/core').Signal<boolean>;
  issueModel: import('@angular/core').WritableSignal<IssueFormModel>;
  stockAvailable: import('@angular/core').WritableSignal<number | null>;
  stockLoading: import('@angular/core').WritableSignal<boolean>;
};

describe('GoodsIssue (examples)', () => {
  let fixture: ComponentFixture<GoodsIssue>;
  let component: TestableGoodsIssue;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoodsIssue],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(GoodsIssue);
    // cast ONCE, then work with the typed view below
    component = fixture.componentInstance as unknown as TestableGoodsIssue;
  });

  // ---------- COMPONENT: business logic via signals/computed ----------

  it('overStock is true only when qty exceeds available stock', () => {
    component.stockAvailable.set(50);
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 20 }));
    expect(component.overStock()).toBe(false);

    component.issueModel.update((m) => ({ ...m, qty: 99 }));
    expect(component.overStock()).toBe(true);
  });

  it('canSubmit requires valid form + known stock + no shortage', () => {
    // stock unknown -> cannot submit (preview not fetched yet)
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 5 }));
    expect(component.canSubmit()).toBe(false);

    // stock known and sufficient -> can submit
    component.stockAvailable.set(50);
    expect(component.canSubmit()).toBe(true);

    // shortage -> blocked again
    component.issueModel.update((m) => ({ ...m, qty: 99 }));
    expect(component.canSubmit()).toBe(false);
  });

  it('canSubmit is false while stock is loading', () => {
    component.stockLoading.set(true);
    component.stockAvailable.set(50);
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 5 }));
    expect(component.canSubmit()).toBe(false);
  });

  // ---------- HTML/TEMPLATE: what the user actually sees ----------

  it('renders the stock preview when stock is available', () => {
    component.stockAvailable.set(50);
    fixture.detectChanges();
    const state = fixture.nativeElement.querySelector('.stock-state');
    expect(state).toBeTruthy();
    expect(state.textContent).toContain('Verfuegbar: 50 Stueck');
  });

  it('shows the shortage alert and disables submit when qty > stock', () => {
    component.stockAvailable.set(13);
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 20 }));
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('.stock-error');
    expect(alert).toBeTruthy();
    expect(alert.textContent).toContain('Nicht genug Bestand!');

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type=submit]');
    expect(button.disabled).toBe(true);
  });

  it('hides the shortage alert when qty fits within stock', () => {
    component.stockAvailable.set(13);
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 5 }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.stock-error')).toBeNull();
  });
});
