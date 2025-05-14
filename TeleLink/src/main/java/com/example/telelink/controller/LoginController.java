package com.example.telelink.controller;

import com.example.telelink.entity.Rol;
import com.example.telelink.entity.Usuario;
import com.example.telelink.entity.VerificationToken;
import com.example.telelink.repository.RolRepository;
import com.example.telelink.repository.UsuarioRepository;
import com.example.telelink.repository.VerificationTokenRepository;
import com.example.telelink.service.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class LoginController {

    private final UsuarioRepository usuarioRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public LoginController(UsuarioRepository usuarioRepository,
                           VerificationTokenRepository tokenRepository,
                           EmailService emailService,
                           PasswordEncoder passwordEncoder,
                           RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
    }

    @GetMapping("/openLoginWindow")
    public String loginWindow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String rol = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .findFirst()
                    .orElse("vecino");

            Map<String, String> redirectMap = new HashMap<>();
            redirectMap.put("superadmin", "/superadmin/inicio");
            redirectMap.put("administrador", "/admin/dashboard");
            redirectMap.put("coordinador", "/coordinador/inicio");
            redirectMap.put("vecino", "/usuarios");

            return "redirect:" + redirectMap.getOrDefault(rol, "/usuarios");
        }
        return "loginWindow";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String rol = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .findFirst()
                    .orElse("vecino");

            Map<String, String> redirectMap = new HashMap<>();
            redirectMap.put("superadmin", "/superadmin/inicio");
            redirectMap.put("administrador", "/admin/dashboard");
            redirectMap.put("coordinador", "/coordinador/inicio");
            redirectMap.put("vecino", "/usuarios");

            return "redirect:" + redirectMap.getOrDefault(rol, "/usuarios");
        }
        return "redirect:/openLoginWindow";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "registro";
    }

    @PostMapping("/submitRegisterForm")
    public String submitRegisterForm(@RequestParam String nombres,
                                     @RequestParam String apellidos,
                                     @RequestParam String correoElectronico,
                                     @RequestParam String contrasenia,
                                     @RequestParam(required = false) String dni,
                                     Model model) {
        // Validar si el correo ya está registrado
        if (usuarioRepository.findByCorreoElectronico(correoElectronico) != null) {
            model.addAttribute("error", "El correo electrónico ya está registrado.");
            return "registro";
        }

        // Validar longitud mínima de la contraseña
        if (contrasenia.length() < 8) {
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "registro";
        }

        // Validar DNI si se proporciona
        if (dni != null && !dni.isEmpty()) {
            if (dni.length() != 8 || !dni.matches("^[0-9]+$")) {
                model.addAttribute("error", "El DNI debe tener 8 dígitos numéricos.");
                return "registro";
            }
            if (usuarioRepository.findByDni(dni) != null) {
                model.addAttribute("error", "El DNI ya está registrado.");
                return "registro";
            }
        }

        // Crear nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreoElectronico(correoElectronico);
        usuario.setContraseniaHash(passwordEncoder.encode(contrasenia));
        usuario.setDni(dni);
        usuario.setEstadoCuenta(Usuario.EstadoCuenta.pendiente);
        usuario.setFechaCreacion(LocalDateTime.now());

        // Asignar rol "vecino"
        Rol vecinoRol = rolRepository.findByRol("vecino");
        if (vecinoRol == null) {
            model.addAttribute("error", "Error interno: Rol 'vecino' no encontrado.");
            return "registro";
        }
        usuario.setRol(vecinoRol);

        // Guardar usuario
        usuarioRepository.save(usuario);

        // Generar token de verificación
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, usuario);
        tokenRepository.save(verificationToken);

        // Enviar correo de verificación
        try {
            emailService.sendVerificationEmail(correoElectronico, nombres, token);
        } catch (Exception e) {
            model.addAttribute("error", "Error al enviar el correo de verificación. Intenta de nuevo.");
            return "registro";
        }

        return "redirect:/register-success";
    }

    @GetMapping("/register-success")
    public String registerSuccess() {
        return "register-success";
    }

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token, Model model) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken == null || verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Token inválido o expirado.");
            return "verify-error";
        }

        Usuario usuario = verificationToken.getUsuario();
        usuario.setEstadoCuenta(Usuario.EstadoCuenta.activo);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        tokenRepository.delete(verificationToken);

        return "redirect:/openLoginWindow?verified=true";
    }
}