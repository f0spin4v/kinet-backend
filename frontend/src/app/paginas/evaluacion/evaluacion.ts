import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { leerError } from '../../core/api';
import { AuthService } from '../../core/auth.service';
import { EvaluacionService } from '../../core/evaluacion.service';
import { Marca } from '../../core/marca';
import { NOMBRE_NIVEL, Pregunta, ResultadoEvaluacion } from '../../core/modelos';

/** Evaluacion de nivel (UI-001): una pregunta por pantalla y al final el nivel inicial. */
@Component({
  selector: 'app-evaluacion',
  imports: [Marca],
  templateUrl: './evaluacion.html',
  styleUrl: './evaluacion.css',
})
export class Evaluacion implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly servicio = inject(EvaluacionService);
  private readonly router = inject(Router);

  readonly preguntas = signal<Pregunta[]>([]);
  readonly respuestas = signal<string[]>([]);
  readonly indice = signal(0);
  readonly cargando = signal(true);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  readonly resultado = signal<ResultadoEvaluacion | null>(null);

  readonly actual = computed(() => this.preguntas()[this.indice()] ?? null);
  readonly total = computed(() => this.preguntas().length);
  readonly avance = computed(() => (this.total() ? ((this.indice() + 1) / this.total()) * 100 : 0));
  readonly nombreNivel = computed(() => {
    const r = this.resultado();
    return r ? NOMBRE_NIVEL[r.nivelInicial] : '';
  });

  ngOnInit(): void {
    // Si el jugador ya la hizo (por ejemplo, desde otro dispositivo), no se repite.
    this.auth.refrescarUsuario().subscribe({
      next: (usuario) => {
        if (usuario.nivelInicial) {
          this.router.navigateByUrl('/inicio');
        } else {
          this.cargarPreguntas();
        }
      },
      error: (respuesta: HttpErrorResponse) => this.mostrarError(respuesta),
    });
  }

  cargarPreguntas(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.servicio.preguntas().subscribe({
      next: (preguntas) => {
        this.preguntas.set(preguntas);
        this.cargando.set(false);
      },
      error: (respuesta: HttpErrorResponse) => this.mostrarError(respuesta),
    });
  }

  /** Guarda la respuesta y avanza; en la ultima pregunta envia todo al backend. */
  responder(opcion: string): void {
    if (this.enviando()) {
      return;
    }
    const respuestas = [...this.respuestas()];
    respuestas[this.indice()] = opcion;
    this.respuestas.set(respuestas);

    if (this.indice() < this.total() - 1) {
      this.indice.update((i) => i + 1);
    } else {
      this.enviar();
    }
  }

  anterior(): void {
    this.error.set(null);
    this.indice.update((i) => Math.max(0, i - 1));
  }

  irAlInicio(): void {
    this.router.navigateByUrl('/inicio');
  }

  cerrarSesion(): void {
    this.auth.cerrarSesion();
  }

  private enviar(): void {
    this.enviando.set(true);
    this.error.set(null);
    this.servicio.enviar(this.respuestas()).subscribe({
      next: (resultado) => {
        this.auth.registrarNivel(resultado.nivelInicial);
        this.resultado.set(resultado);
        this.enviando.set(false);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.enviando.set(false);
        if (respuesta.status === 409) {
          // Ya estaba hecha: se actualiza la sesion y se sigue al inicio.
          this.auth.refrescarUsuario().subscribe(() => this.router.navigateByUrl('/inicio'));
          return;
        }
        this.error.set(leerError(respuesta).mensaje);
      },
    });
  }

  private mostrarError(respuesta: HttpErrorResponse): void {
    this.cargando.set(false);
    this.error.set(leerError(respuesta).mensaje);
  }
}
