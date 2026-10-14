import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL } from './api';
import { DatosLogin, DatosRegistro, NivelInicial, RespuestaAuth, Usuario } from './modelos';

const CLAVE_TOKEN = 'kinet.token';
const CLAVE_USUARIO = 'kinet.usuario';

/** Guarda la sesion (token + usuario) y habla con /api/auth. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(localStorage.getItem(CLAVE_TOKEN));
  private readonly _usuario = signal<Usuario | null>(leerUsuarioGuardado());

  readonly token = this._token.asReadonly();
  readonly usuario = this._usuario.asReadonly();
  readonly autenticado = computed(() => this._token() !== null);

  registrar(datos: DatosRegistro): Observable<RespuestaAuth> {
    return this.http
      .post<RespuestaAuth>(`${API_URL}/auth/registro`, datos)
      .pipe(tap((r) => this.guardarSesion(r)));
  }

  iniciarSesion(datos: DatosLogin): Observable<RespuestaAuth> {
    return this.http
      .post<RespuestaAuth>(`${API_URL}/auth/login`, datos)
      .pipe(tap((r) => this.guardarSesion(r)));
  }

  /** Vuelve a pedir los datos del usuario al backend; confirma que el token sigue vigente. */
  refrescarUsuario(): Observable<Usuario> {
    return this.http.get<Usuario>(`${API_URL}/usuarios/me`).pipe(
      tap((u) => {
        this._usuario.set(u);
        localStorage.setItem(CLAVE_USUARIO, JSON.stringify(u));
      }),
    );
  }

  /** Guarda en la sesion el nivel que acaba de asignar la evaluacion. */
  registrarNivel(nivel: NivelInicial): void {
    const actual = this._usuario();
    if (actual) {
      const actualizado = { ...actual, nivelInicial: nivel };
      this._usuario.set(actualizado);
      localStorage.setItem(CLAVE_USUARIO, JSON.stringify(actualizado));
    }
  }

  cerrarSesion(): void {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_USUARIO);
    this._token.set(null);
    this._usuario.set(null);
    this.router.navigateByUrl('/login');
  }

  private guardarSesion(respuesta: RespuestaAuth): void {
    localStorage.setItem(CLAVE_TOKEN, respuesta.token);
    localStorage.setItem(CLAVE_USUARIO, JSON.stringify(respuesta.usuario));
    this._token.set(respuesta.token);
    this._usuario.set(respuesta.usuario);
  }
}

function leerUsuarioGuardado(): Usuario | null {
  try {
    const texto = localStorage.getItem(CLAVE_USUARIO);
    return texto ? (JSON.parse(texto) as Usuario) : null;
  } catch {
    return null;
  }
}
