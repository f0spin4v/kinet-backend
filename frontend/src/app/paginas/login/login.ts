import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { leerError } from '../../core/api';
import { AuthService } from '../../core/auth.service';
import { Marca } from '../../core/marca';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Marca],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly enviando = signal(false);
  readonly errorGeneral = signal<string | null>(null);

  readonly formulario = inject(FormBuilder).nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required]],
  });

  enviar(): void {
    this.errorGeneral.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.auth.iniciarSesion(this.formulario.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/inicio'),
      error: (respuesta: HttpErrorResponse) => {
        this.errorGeneral.set(leerError(respuesta).mensaje);
        this.enviando.set(false);
      },
    });
  }
}
