package co.edu.uniquindio.kinet.exceptions;

import co.edu.uniquindio.kinet.dto.ResponseDTO;
import co.edu.uniquindio.kinet.dto.ValidacionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Convierte cada error en una respuesta ResponseDTO con un mensaje claro en español (RNF-12). */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ResponseDTO<String>> noEncontrado(NoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ResponseDTO<String>> conflicto(ConflictoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ResponseDTO<String>> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ResponseDTO<String>> credenciales(CredencialesInvalidasException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ResponseDTO<String>> accesoDenegado(AccesoDenegadoException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Campos que no cumplen las reglas del DTO: se devuelve un mensaje por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<List<ValidacionDTO>>> validacion(MethodArgumentNotValidException ex) {
        List<ValidacionDTO> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ValidacionDTO(nombreCampo(e), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ResponseDTO<>(true, errores));
    }

    /** JSON mal formado o un valor que no existe en una lista (por ejemplo una intensidad inválida). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDTO<String>> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "La información enviada no tiene el formato esperado.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseDTO<String>> archivoGrande(MaxUploadSizeExceededException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "La imagen no puede pesar más de 5 MB.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ResponseDTO<String>> faltaArchivo(MissingServletRequestPartException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Adjunta la imagen en el campo \"" + ex.getRequestPartName() + "\".");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ResponseDTO<String>> tipoInvalido(TypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Uno de los valores de la dirección no es válido.");
    }

    /** Errores propios de Spring (ruta inexistente, método no permitido...) y cualquier error inesperado. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<String>> general(Exception ex) {
        if (ex instanceof ErrorResponse errorSpring) {
            HttpStatus estado = HttpStatus.valueOf(errorSpring.getStatusCode().value());
            return respuesta(estado, estado.is4xxClientError()
                    ? "La petición no es válida para esta dirección."
                    : "Ocurrió un error en el servidor.");
        }
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo.");
    }

    private ResponseEntity<ResponseDTO<String>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ResponseDTO<>(true, mensaje));
    }

    /** "respuestas[2]" se reporta como "respuestas" para que el frontend lo asocie al campo. */
    private String nombreCampo(FieldError error) {
        String campo = error.getField();
        int corchete = campo.indexOf('[');
        return corchete > 0 ? campo.substring(0, corchete) : campo;
    }
}
