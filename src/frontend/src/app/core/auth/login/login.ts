import { Component, inject, signal } from '@angular/core';
import { form, FormField, required } from '@angular/forms/signals';
import { AuthService } from '../auth-service';
import { Router } from '@angular/router';

interface LoginData{
  username: string;
  password: string;
}

@Component({
  imports: [FormField],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loginModel = signal<LoginData>({username: "", password: ""});

  // form validation
  protected readonly loginForm = form(this.loginModel,f=>{
    required(f.username, {message: "Username is required"});
    required(f.password, {message: "Password is required"});
  });

  // authorization error
  protected readonly authError = signal<string | null>(null);

  onSubmit(event: Event): void{
    event.preventDefault();
    if(this.loginForm().invalid()) return;

    this.authService.login(this.loginModel()).subscribe({
      next: (res) => {
        // save token - isAthenticated = true
        this.authService.saveToken(res.token);
        this.router.navigate(['/products']);
      },
      error: () => this.authError.set("Invalid username or password")
    });
  }

}
