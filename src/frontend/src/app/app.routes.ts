import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./core/auth/login/login').then((m) => m.Login) },
  {
    path: 'products',
    canActivate: [authGuard], // <- guard: bez tokena redirect na /login
    loadComponent: () =>
      import('./features/inventory/components/product-list/product-list').then(
        (m) => m.ProductList,
      ),
  },
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  { path: '**', redirectTo: 'login' },
];
