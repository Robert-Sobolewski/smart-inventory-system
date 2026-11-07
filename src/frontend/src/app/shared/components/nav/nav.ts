import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/**
 * The main application navigation (menu).
 * Responsibility: "where can I go" — deliberately a separate component from Status
 * ("who am I": user + logout), following the single-responsibility principle.
 */
@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-nav',
  styleUrl: './nav.scss',
  templateUrl: './nav.html',
})
export class Nav {}
