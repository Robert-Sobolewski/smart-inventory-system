import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Status } from './shared/components/status/status';

@Component({
  imports: [RouterOutlet, Status],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
}
