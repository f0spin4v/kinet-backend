package co.edu.uniquindio.kinet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación de la API en /swagger-ui.html, con el botón "Authorize" para pegar el token. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kinetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kinet API")
                        .version("1.0.0")
                        .description("Procesos 1 (Usuario y Autenticación) y 2 (Entrenamientos y Retos) de Kinet."))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
