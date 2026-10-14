import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth-service';
import { LoginData } from './login/types';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    for (const req of httpMock.match('/api/auth')) {
      req.flush([]);
    }

    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call login service', async () => {
    let mockUser: LoginData = {
      username: 'worker',
      password: 'Worker123!',
    };
    let token = null;
    const expectResponse: { token: string | null; username: string } = {
      token: 'eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJtYW5hZ2VyIiwicm9sZSI6IlJPT',
      username: 'Worker',
    };

    const resultPromise = firstValueFrom(service.login(mockUser));
    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBe(mockUser);

    req.flush(expectResponse);
    expect(await resultPromise).toBe(expectResponse);
  });

  it('should call save token service and update isAuthenticated signal', () => {
    // setup — jsdom 28: localStorage to Proxy, więc spy musi stać na Storage.prototype
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
    const token = 'eyJhbG...lJPT';
    const username = 'Worker';
    // act
    service.saveToken(token, username);
    //assert
    expect(setItemSpy).toHaveBeenCalledWith('auth_token', token);
    expect(setItemSpy).toHaveBeenCalledWith('auth_username', username);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should call save token service without username', () => {
    // setup — jsdom 28: localStorage to Proxy, więc spy musi stać na Storage.prototype
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
    const token = 'eyJhbG...lJPT';
    const username = 'Worker';
    // act
    service.saveToken(token);
    //assert
    expect(setItemSpy).toHaveBeenCalledWith('auth_token', token);
    expect(setItemSpy).not.toHaveBeenCalledWith('auth_username', undefined);
    expect(setItemSpy).not.toHaveBeenCalledWith('auth_username', null);
    expect(setItemSpy).toHaveBeenCalledTimes(1);
  });

  it('should call get username service', async () => {
    const username = 'worker';
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem');

    vi.mocked(getItemSpy).mockReturnValue(username);

    const result = service.getUsername();
    expect(result).toBe(username);
    expect(getItemSpy).toHaveBeenCalledWith('auth_username');
  });

  it('should call get token service', async () => {
    // setup
    const token = 'eyJhbG...lJPT';
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem');
    vi.mocked(getItemSpy).mockReturnValue(token);
    // act
    const result = service.getToken();
    // assert
    expect(result).toBe(token);
    expect(getItemSpy).toHaveBeenCalledWith('auth_token');
  });

  it('should call logout service', async () => {
    //setup
    const token = 'eyJhbG...LJPT';
    const removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem');

    //act
    const result = service.logout();

    // assert
    expect(removeItemSpy).toHaveBeenCalledWith('auth_token');
    expect(removeItemSpy).toHaveBeenCalledWith('auth_username');
    expect(service.isAuthenticated()).toBe(false);
  });
});
