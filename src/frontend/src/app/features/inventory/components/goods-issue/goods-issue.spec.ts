import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { GoodsIssue } from './goods-issue';

describe('GoodsIssue', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoodsIssue],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(GoodsIssue);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
