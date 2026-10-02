import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';

// @Service()
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/auth';

  // signal store loginState true/false
  readonly isAuthenticated = signal<boolean>(this.hasToken());

  constructor() {}

  // login method
  login(credentials: any) {
    return this.http.post<{ token: string }>(`${this.API_URL}/login`, credentials);
  }

  // save token in localStorage
  saveToken(token: string) {
    localStorage.setItem('auth_token', token);
    this.isAuthenticated.set(true);
  }

  // get token
  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  // logout
  logout() {
    localStorage.removeItem('auth_token');
    this.isAuthenticated.set(false);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('auth_token');
  }
}
