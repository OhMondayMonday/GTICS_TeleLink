package com.example.telelink.config;

import com.example.telelink.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSecurityConfig {

    final UsuarioRepository usuarioRepository;
    final DataSource dataSource;

    public WebSecurityConfig(DataSource dataSource, UsuarioRepository usuarioRepository) {
        this.dataSource = dataSource;
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            // Si el usuario está autenticado, redirige según su rol
            if (request.getUserPrincipal() != null) {
                Authentication authentication = (Authentication) request.getUserPrincipal();
                String rol = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .findFirst()
                        .orElse("vecino");

                Map<String, String> redirectMap = new HashMap<>();
                redirectMap.put("superadmin", "/superadmin/inicio");
                redirectMap.put("administrador", "/admin/dashboard");
                redirectMap.put("coordinador", "/coordinador/inicio");
                redirectMap.put("vecino", "/usuarios/inicio");

                String redirectUrl = redirectMap.getOrDefault(rol, "/usuarios/inicio");
                new DefaultRedirectStrategy().sendRedirect(request, response, redirectUrl);
            } else {
                // Si no está autenticado, redirige al login
                new DefaultRedirectStrategy().sendRedirect(request, response, "/openLoginWindow");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/superadmin", "/superadmin/**").hasAuthority("superadmin")
                        .requestMatchers("/admin", "/admin/**").hasAuthority("administrador")
                        .requestMatchers("/coordinador", "/coordinador/**").hasAuthority("coordinador")
                        .requestMatchers("/usuarios", "/usuarios/**").hasAuthority("vecino")
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler()) // Maneja errores 403
                )
                .formLogin(form -> form
                        .loginPage("/openLoginWindow")
                        .loginProcessingUrl("/submitLoginForm")
                        .successHandler((request, response, authentication) -> {
                            HttpSession session = request.getSession();
                            session.setAttribute("usuario",
                                    usuarioRepository.findByCorreoElectronico(authentication.getName()));

                            DefaultSavedRequest defaultSavedRequest =
                                    (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");

                            if (defaultSavedRequest != null) {
                                String targetUrl = defaultSavedRequest.getRequestURL();
                                new DefaultRedirectStrategy().sendRedirect(request, response, targetUrl);
                            } else {
                                String rol = authentication.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .findFirst().orElse("vecino");

                                Map<String, String> redirectMap = new HashMap<>();
                                redirectMap.put("superadmin", "/superadmin/inicio");
                                redirectMap.put("administrador", "/admin/dashboard");
                                redirectMap.put("coordinador", "/coordinador/inicio");
                                redirectMap.put("vecino", "/usuarios/inicio");

                                String redirectUrl = redirectMap.getOrDefault(rol, "/usuarios/inicio");
                                new DefaultRedirectStrategy().sendRedirect(request, response, redirectUrl);
                            }
                        })
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/openLoginWindow")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                )
                .sessionManagement(session -> session
                        .invalidSessionUrl("/openLoginWindow") // Redirige al login si la sesión es inválida
                        .sessionAuthenticationErrorUrl("/openLoginWindow")
                        .maximumSessions(1)
                        .expiredUrl("/openLoginWindow?expired=true")
                );

        return http.build();
    }

    @Bean
    public UserDetailsManager users(DataSource dataSource) {
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
        String sqlAuth = """
            SELECT correo_electronico, contrasenia_hash, 
                   CASE WHEN estado_cuenta = 'activo' THEN true ELSE false END AS enabled
            FROM usuarios 
            WHERE correo_electronico = ?
            """;
        String sqlAuto = """
            SELECT u.correo_electronico, r.rol 
            FROM usuarios u 
            INNER JOIN roles r ON u.rol_id = r.rol_id 
            WHERE u.correo_electronico = ?
            """;
        users.setUsersByUsernameQuery(sqlAuth);
        users.setAuthoritiesByUsernameQuery(sqlAuto);
        return users;
    }
}