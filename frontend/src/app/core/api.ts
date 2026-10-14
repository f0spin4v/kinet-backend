import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from './modelos';

/** Direccion del backend Spring Boot. Cambiar aqui cuando se despliegue en AWS. */
export const API_URL = 'http://localhost:8080/api';

/** Convierte cualquier fallo HTTP en un ErrorApi con un mensaje que el usuario entienda. */
export function leerError(respuesta: HttpErrorResponse): ErrorApi {
  if (respuesta.status === 0) {
    return { estado: 0, mensaje: 'No pudimos conectar con el servidor. Revisa que el backend esté encendido.', campos: {} };
  }
  const cuerpo = respuesta.error as Partial<ErrorApi> | null;
  return {
    estado: respuesta.status,
    mensaje: cuerpo?.mensaje ?? 'Algo salió mal. Intenta de nuevo.',
    campos: cuerpo?.campos ?? {},
  };
}
