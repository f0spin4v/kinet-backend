import { Component } from '@angular/core';

/** Logo y nombre de Kinet, igual al encabezado de los mockups. */
@Component({
  selector: 'app-marca',
  template: `
    <div class="marca">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 3 4 21l8-5 8 5z" fill="#1f6bff" />
        <path d="M12 3v13l8 5z" fill="#19b5fe" />
      </svg>
      <span>KINET</span>
    </div>
  `,
})
export class Marca {}
