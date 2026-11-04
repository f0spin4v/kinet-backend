package co.edu.uniquindio.kinet.security;

import co.edu.uniquindio.kinet.model.enums.EstadoUsuario;
import co.edu.uniquindio.kinet.repositories.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el token del encabezado "Authorization: Bearer ..." en cada petición.
 * Además de la firma y la vigencia, revisa en la base que la cuenta siga ACTIVA y usa el rol ACTUAL (no el que
 * traía el token): si el administrador bloquea una cuenta o le cambia el rol, el cambio aplica en la siguiente
 * petición y no cuando venza el token (auditoría 03/11/2026, criterio 6.7.2).
 */
@Component
@RequiredArgsConstructor
public class JwtFiltro extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");
        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            jwtUtils.validar(encabezado.substring(7))
                    .flatMap(datos -> usuarioRepository.estadoYRol(datos.getSubject())
                            .filter(cuenta -> cuenta.getEstado() == EstadoUsuario.ACTIVO)
                            .map(cuenta -> new UsernamePasswordAuthenticationToken(datos.getSubject(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + cuenta.getRol())))))
                    // sin autenticación, Spring Security responde 401 en las rutas protegidas
                    .ifPresent(autenticacion -> SecurityContextHolder.getContext().setAuthentication(autenticacion));
        }
        chain.doFilter(request, response);
    }
}
