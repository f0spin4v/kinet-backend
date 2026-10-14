import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Solo deja entrar a quien tiene sesion; si no, manda al login. */
export const soloAutenticado: CanActivateFn = () =>
  inject(AuthService).autenticado() ? true : inject(Router).createUrlTree(['/login']);

/** Evita que alguien con sesion vuelva a ver el login o el registro. */
export const soloInvitado: CanActivateFn = () =>
  inject(AuthService).autenticado() ? inject(Router).createUrlTree(['/inicio']) : true;

/** La evaluacion de nivel es obligatoria: sin ella no se entra al resto de la app. */
export const conEvaluacion: CanActivateFn = () =>
  inject(AuthService).usuario()?.nivelInicial ? true : inject(Router).createUrlTree(['/evaluacion']);
