import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient()],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(Dashboard);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
