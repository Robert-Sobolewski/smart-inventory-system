import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/**
 * Główna nawigacja aplikacji (menu).
 * Odpowiedzialność: "dokąd mogę pójść" — celowo osobny komponent od Status
 * ("kim jestem": user + logout), zgodnie z zasadą pojedynczej odpowiedzialności.
 */
@Component({
  imports: [RouterLink, RouterLinkActive],
  selector: 'app-nav',
  styleUrl: './nav.scss',
  templateUrl: './nav.html',
})
export class Nav {}
