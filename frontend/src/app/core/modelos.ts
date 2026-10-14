/** Formas de los datos que intercambia el frontend con la API de Kinet. */

export type NivelInicial = 'INICIACION' | 'INTERMEDIO' | 'AVANZADO';

/** Nombre de cada nivel tal como se muestra en pantalla. */
export const NOMBRE_NIVEL: Record<NivelInicial, string> = {
  INICIACION: 'Iniciación',
  INTERMEDIO: 'Intermedio',
  AVANZADO: 'Avanzado',
};

export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: 'JUGADOR' | 'ADMINISTRADOR';
  nickname: string | null;
  avatarUrl: string | null;
  fechaNacimiento: string | null;
  ciudad: string | null;
  deporteFavorito: string | null;
  /** null mientras el jugador no haya hecho la evaluacion de nivel. */
  nivelInicial: NivelInicial | null;
}

export interface RespuestaAuth {
  token: string;
  usuario: Usuario;
}

export interface DatosRegistro {
  nombre: string;
  correo: string;
  contrasena: string;
  nickname: string;
  aceptaDatos: boolean;
}

export interface DatosLogin {
  correo: string;
  contrasena: string;
}

/** Error que devuelve el backend. "campos" trae el mensaje de cada campo invalido. */
export interface ErrorApi {
  estado: number;
  mensaje: string;
  campos: Record<string, string>;
}

export interface OpcionPregunta {
  id: 'A' | 'B';
  titulo: string;
  descripcion: string;
}

export interface Pregunta {
  numero: number;
  texto: string;
  opciones: OpcionPregunta[];
}

export interface ResultadoEvaluacion {
  puntaje: number;
  nivelInicial: NivelInicial;
  fecha: string;
}
