package co.edu.uniquindio.kinet.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados: lo que viaja dentro de "respuesta" en las consultas de lista que crecen sin tope.
 * "pagina" empieza en 0. El orden lo fija cada consulta, no el cliente.
 */
public record PaginaDTO<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

    public static final int TAMANIO_POR_DEFECTO = 20;
    public static final int TAMANIO_MAXIMO = 100;

    /** Los parámetros del cliente, acotados: pagina mínima 0 y tamanio entre 1 y TAMANIO_MAXIMO. */
    public static Pageable solicitar(int pagina, int tamanio) {
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO));
    }

    public static <T> PaginaDTO<T> de(Page<T> pagina) {
        return de(pagina, pagina.getContent());
    }

    /** Para cuando el contenido se convierte en bloque (una consulta para toda la página) y no de a uno. */
    public static <E, T> PaginaDTO<T> de(Page<E> pagina, Function<List<E>, List<T>> convertir) {
        return de(pagina, convertir.apply(pagina.getContent()));
    }

    private static <T> PaginaDTO<T> de(Page<?> pagina, List<T> contenido) {
        return new PaginaDTO<>(contenido, pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
