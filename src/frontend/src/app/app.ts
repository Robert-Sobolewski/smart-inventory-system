import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Status } from './shared/components/status/status';
import { Nav } from './shared/components/nav/nav';

@Component({
  imports: [RouterOutlet, Status, Nav],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);

  // reactive URL — updates on every navigation (NavigationEnd)
  protected readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: '' },
  );

  // we hide the status bar on the login page
  protected readonly showStatusBar = () => this.currentUrl() !== '/login';

  protected readonly title = signal('frontend');
}
