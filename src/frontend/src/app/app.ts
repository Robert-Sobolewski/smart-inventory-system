import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Status } from './shared/components/status/status';

@Component({
  imports: [RouterOutlet, Status],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);

  // reaktywny URL - aktualizuje sie przy kazdej nawigacji (NavigationEnd)
  protected readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: '' },
  );

  // pasek statusu ukrywamy na stronie logowania
  protected readonly showStatusBar = () => this.currentUrl() !== '/login';

  protected readonly title = signal('frontend');
}
