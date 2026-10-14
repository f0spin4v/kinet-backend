import { Routes } from '@angular/router';
import { conEvaluacion, soloAutenticado, soloInvitado } from './core/auth.guard';
import { Evaluacion } from './paginas/evaluacion/evaluacion';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Registro } from './paginas/registro/registro';

export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [soloInvitado], title: 'Iniciar sesión · Kinet' },
  { path: 'registro', component: Registro, canActivate: [soloInvitado], title: 'Crear cuenta · Kinet' },
  { path: 'evaluacion', component: Evaluacion, canActivate: [soloAutenticado], title: 'Evaluación de nivel · Kinet' },
  { path: 'inicio', component: Inicio, canActivate: [soloAutenticado, conEvaluacion], title: 'Inicio · Kinet' },
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
  { path: '**', redirectTo: 'inicio' },
];
