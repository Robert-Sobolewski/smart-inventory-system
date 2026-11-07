import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';

@Component({
  imports: [RouterLink],
  selector: 'app-status',
  styleUrl: './status.scss',
  templateUrl: './status.html',
})
export class Status {
  // inject() w klasie komponentu = "daj mi ten serwis" (DI)
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // the template can only see members of the component class — so we expose them here:
  // isAuthenticated is a signal — in the template we call it like a function: isAuthenticated()
  protected readonly isAuthenticated = this.authService.isAuthenticated;
  protected readonly username = () => this.authService.getUsername();

  // logout + redirect to login (otherwise we would stay on the current page without a token)
  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
