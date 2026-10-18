package co.edu.uniquindio.kinet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Hilos para el trabajo que no debe frenar la respuesta al usuario, como guardar y enviar notificaciones.
 * Con kinet.notificaciones.asincronas=false (pruebas) se ejecuta en el mismo hilo para que el resultado sea inmediato.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String EJECUTOR_NOTIFICACIONES = "notificacionesExecutor";

    @Bean(name = EJECUTOR_NOTIFICACIONES)
    public TaskExecutor notificacionesExecutor(
            @Value("${kinet.notificaciones.asincronas:true}") boolean asincronas) {
        if (!asincronas) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(500);
        ejecutor.setThreadNamePrefix("notificacion-");
        // Si la cola se llena, la notificación la guarda el hilo que la pidió: más lento, pero no se pierde.
        ejecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Al apagar la aplicación se terminan de guardar las notificaciones pendientes.
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(10);
        ejecutor.initialize();
        return ejecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (error, metodo, parametros) ->
                log.error("Falló la tarea asíncrona {}: {}", metodo.getName(), error.getMessage(), error);
    }
}
