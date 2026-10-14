import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from './api';
import { Pregunta, ResultadoEvaluacion } from './modelos';

/** Habla con /api/evaluacion: trae las preguntas y envia las respuestas. */
@Injectable({ providedIn: 'root' })
export class EvaluacionService {
  private readonly http = inject(HttpClient);

  preguntas(): Observable<Pregunta[]> {
    return this.http.get<Pregunta[]>(`${API_URL}/evaluacion/preguntas`);
  }

  /** El backend califica las respuestas y devuelve el nivel inicial. */
  enviar(respuestas: string[]): Observable<ResultadoEvaluacion> {
    return this.http.post<ResultadoEvaluacion>(`${API_URL}/evaluacion`, { respuestas });
  }
}
