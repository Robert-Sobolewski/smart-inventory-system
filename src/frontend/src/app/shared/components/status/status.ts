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

  // template widzi TYLKO czlonow klasy komponentu - wiec wystawiam je tutaj:
  // isAuthenticated to signal, w template wywolujemy jak funkcje: isAuthenticated()
  protected readonly isAuthenticated = this.authService.isAuthenticated;
  protected readonly username = () => this.authService.getUsername();

  // logout + redirect na login (inaczej zostaniemy na biezacej stronie bez tokena)
  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
