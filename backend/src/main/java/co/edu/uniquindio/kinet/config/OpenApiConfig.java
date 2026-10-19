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
                        .version("2.0.0")
                        .description("Kinet v2: cuentas y roles, evaluación de nivel, entrenamientos con cronómetro y evidencias, XP y niveles, retos, "
                                + "clanes, temporadas competitivas, matchmaking, logros, notificaciones, moderación, soporte y analítica."))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
