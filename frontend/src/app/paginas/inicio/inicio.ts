import { Component, OnInit, computed, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { Marca } from '../../core/marca';
import { NOMBRE_NIVEL } from '../../core/modelos';

/** Pantalla de llegada despues de iniciar sesion. Aqui ira el dashboard (UI-003) en el Sprint 4. */
@Component({
  selector: 'app-inicio',
  imports: [Marca],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css',
})
export class Inicio implements OnInit {
  private readonly auth = inject(AuthService);
  readonly usuario = this.auth.usuario;
  readonly nivel = computed(() => {
    const nivel = this.usuario()?.nivelInicial;
    return nivel ? NOMBRE_NIVEL[nivel] : null;
  });

  ngOnInit(): void {
    // Confirma con el backend que el token guardado sigue vigente.
    // Si ya vencio, el interceptor cierra la sesion y manda al login.
    this.auth.refrescarUsuario().subscribe({ error: () => undefined });
  }

  cerrarSesion(): void {
    this.auth.cerrarSesion();
  }
}
