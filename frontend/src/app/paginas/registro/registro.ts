import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { leerError } from '../../core/api';
import { AuthService } from '../../core/auth.service';
import { Marca } from '../../core/marca';

type Campo = 'nombre' | 'correo' | 'contrasena' | 'nickname' | 'aceptaDatos';

/** Mensajes de validacion en pantalla. Son las mismas reglas que aplica el backend. */
const MENSAJES: Record<Campo, string> = {
  nombre: 'Escribe tu nombre.',
  correo: 'Escribe un correo válido.',
  contrasena: 'Mínimo 8 caracteres, con al menos una letra y un número.',
  nickname: 'Entre 3 y 30 caracteres: letras, números y guion bajo.',
  aceptaDatos: 'Debes aceptar la política de tratamiento de datos.',
};

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, Marca],
  templateUrl: './registro.html',
})
export class Registro {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly enviando = signal(false);
  readonly errorGeneral = signal<string | null>(null);
  /** Errores por campo que devuelve el backend (por ejemplo, correo ya registrado). */
  readonly erroresServidor = signal<Record<string, string>>({});

  readonly formulario = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    contrasena: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72),
      Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)]],
    nickname: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30),
      Validators.pattern(/^[A-Za-z0-9_]+$/)]],
    aceptaDatos: [false, [Validators.requiredTrue]],
  });

  /** Mensaje de error a mostrar en un campo, o null si esta bien. */
  errorDe(campo: Campo): string | null {
    const control = this.formulario.controls[campo];
    if (control.touched && control.invalid) {
      return MENSAJES[campo];
    }
    return this.erroresServidor()[campo] ?? null;
  }

  enviar(): void {
    this.errorGeneral.set(null);
    this.erroresServidor.set({});
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.auth.registrar(this.formulario.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/inicio'),
      error: (respuesta: HttpErrorResponse) => {
        const error = leerError(respuesta);
        this.erroresServidor.set(error.campos);
        this.errorGeneral.set(error.mensaje);
        this.enviando.set(false);
      },
    });
  }
}
