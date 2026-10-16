import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Componente raíz: solo aloja el enrutador. */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AppComponent {}
