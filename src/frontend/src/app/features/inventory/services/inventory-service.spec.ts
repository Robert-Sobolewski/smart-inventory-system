import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ProductCreateRequest } from '../models/product';
import { InventoryService } from './inventory-service';

describe('InventoryService', () => {
  let service: InventoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InventoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    for (const req of httpMock.match('/api/products')) {
      req.flush([]);
    }
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
    expect(service.productResource).toBeDefined();
  });

  it('should call create product and return success string', async () => {
    const mockProduct: ProductCreateRequest = {
      sku: 'SKU-001',
      name: 'DemoName',
      category: 'Demo',
      description: 'Optional description for demo product.',
    };

    const expectedResponse = `Product ${mockProduct.sku} erfolgreich angelegt!`;
    const resultPromise = firstValueFrom(service.addProduct(mockProduct));
    const req = httpMock.expectOne('/api/products/create');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockProduct);
    req.flush(expectedResponse);
    expect(await resultPromise).toBe(expectedResponse);
  });
});
