package co.edu.uniquindio.kinet.repositories;

/** Proyección genérica clave / total para las consultas agregadas de analítica. */
public interface Conteo {
    String getClave();

    Long getTotal();
}
