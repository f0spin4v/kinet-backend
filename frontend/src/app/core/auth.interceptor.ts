import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_URL } from './api';
import { AuthService } from './auth.service';

/**
 * Agrega el token a cada peticion hacia la API.
 * Si el backend responde 401 en una ruta protegida, el token vencio: se cierra la sesion.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const esApi = peticion.url.startsWith(API_URL);
  const esRutaPublica = peticion.url.startsWith(`${API_URL}/auth/`);

  const conToken =
    token && esApi && !esRutaPublica
      ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : peticion;

  return siguiente(conToken).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && esApi && !esRutaPublica) {
        auth.cerrarSesion();
      }
      return throwError(() => error);
    }),
  );
};
