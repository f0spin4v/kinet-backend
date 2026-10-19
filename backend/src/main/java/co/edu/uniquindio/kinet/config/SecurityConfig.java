package co.edu.uniquindio.kinet.config;

import co.edu.uniquindio.kinet.security.JwtFiltro;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.uniquindio.kinet.dto.ResponseDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, JwtFiltro jwtFiltro, ObjectMapper json) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // staff
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/moderacion/**").hasAnyRole("ENTRENADOR", "ADMINISTRADOR")
                        .requestMatchers("/api/soporte/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/analitica/**").hasRole("ADMINISTRADOR")
                        // el administrador también consulta el catálogo de logros (para gestionarlos)
                        .requestMatchers(HttpMethod.GET, "/api/logros").hasAnyRole("JUGADOR", "ADMINISTRADOR")
                        // el equipo ve los clanes (lista y detalle) para poder cerrarlos desde moderación
                        .requestMatchers(HttpMethod.GET, "/api/clanes", "/api/clanes/{id:[0-9]+}")
                        .hasAnyRole("JUGADOR", "ENTRENADOR", "ADMINISTRADOR")
                        // acciones de juego: solo jugadores
                        .requestMatchers("/api/entrenamientos/**", "/api/retos/**", "/api/competencias/**",
                                "/api/clanes/**", "/api/matchmaking/**", "/api/evaluacion/**", "/api/logros/**",
                                "/api/temporadas/activa/mia/**", "/api/temporadas/activa/abandono").hasRole("JUGADOR")
                        // perfil, progreso, notificaciones, catálogos y ranking: cualquier cuenta autenticada
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        // sin token válido: 401
                        .authenticationEntryPoint((req, res, ex) ->
                                escribirError(res, json, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Tu sesión no es válida o ya venció. Inicia sesión de nuevo."))
                        // con token pero sin el rol necesario: 403
                        .accessDeniedHandler((req, res, ex) ->
                                escribirError(res, json, HttpServletResponse.SC_FORBIDDEN,
                                        "No tienes permiso para hacer esta acción.")))
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt con sal: las contraseñas nunca se guardan en texto plano (RNF-21). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Orígenes permitidos para el frontend, separados por coma (local y desplegado). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${kinet.cors.origenes}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        return fuente;
    }

    private static void escribirError(HttpServletResponse res, ObjectMapper json, int estado, String mensaje)
            throws IOException {
        res.setStatus(estado);
        res.setCharacterEncoding("UTF-8");
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(res.getOutputStream(), new ResponseDTO<>(true, mensaje));
    }
}
