import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth-service';

/**
 * Route guard: decides whether a route may be activated.
 *
 * Angular calls this function BEFORE loading the component of a protected
 * route. It returns:
 *   - true            -> navigation allowed (component loads)
 *   - UrlTree/redirect-> user is sent elsewhere (e.g. /login)
 */
export const authGuard: CanActivateFn = () => {
  // inject() works here because guards run inside Angular's DI context
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // not authenticated -> redirect to login, remember where the user wanted to go
  return router.createUrlTree(['/login']);
};
