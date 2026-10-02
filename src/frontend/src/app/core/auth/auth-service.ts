import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { LoginData } from './login/types';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/auth';

  // signal store loginState true/false
  readonly isAuthenticated = signal<boolean>(this.hasToken());

  constructor() {}

  // login method
  login(credentials: LoginData) {
    return this.http.post<{ token: string; username: string }>(`${this.API_URL}/login`, credentials);
  }

  // save token in localStorage
  saveToken(token: string, username?: string) {
    localStorage.setItem('auth_token', token);
    if (username) {
      localStorage.setItem('auth_username', username);
    }
    this.isAuthenticated.set(true);
  }

  // get username from localStorage
  getUsername(): string | null {
    return localStorage.getItem('auth_username');
  }

  // get token
  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  // logout
  logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_username');
    this.isAuthenticated.set(false);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('auth_token');
  }
}
