import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./core/auth/login/login').then((m) => m.Login) },
  {
    path: 'products',
    loadComponent: () =>
      import('./features/inventory/components/product-list/product-list').then(
        (m) => m.ProductList,
      ),
  },
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  { path: '**', redirectTo: 'login' },
];
