package com.example.telelink.controller;

import com.example.telelink.entity.PasswordResetToken;
import com.example.telelink.entity.Rol;
import com.example.telelink.entity.Usuario;
import com.example.telelink.entity.VerificationToken;
import com.example.telelink.repository.RolRepository;
import com.example.telelink.repository.UsuarioRepository;
import com.example.telelink.repository.VerificationTokenRepository;
import com.example.telelink.repository.PasswordResetTokenRepository;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public LoginController(UsuarioRepository usuarioRepository,
                           VerificationTokenRepository tokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           EmailService emailService,
                           PasswordEncoder passwordEncoder,
                           RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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
            redirectMap.put("vecino", "/usuarios/inicio");

            return "redirect:" + redirectMap.getOrDefault(rol, "/usuarios/inicio");
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


    @GetMapping("/recuperar-contrasenia")
    public String recuperarContraseniaForm() {
        return "recuperar-contrasenia";
    }

    @PostMapping("/recuperar-contrasenia")
    public String recuperarContraseniaSubmit(@RequestParam String correoElectronico, Model model) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico);
        if (usuario == null) {
            model.addAttribute("error", "El correo electrónico no está registrado.");
            return "recuperar-contrasenia";
        }

        if (usuario.getEstadoCuenta() != Usuario.EstadoCuenta.activo) {
            model.addAttribute("error", "La cuenta no está activa. Verifica tu correo para activarla.");
            return "recuperar-contrasenia";
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, usuario);
        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(correoElectronico, usuario.getNombres(), token);
        } catch (Exception e) {
            model.addAttribute("error", "Error al enviar el correo de restablecimiento. Intenta de nuevo.");
            return "recuperar-contrasenia";
        }

        model.addAttribute("success", "Se ha enviado un enlace de restablecimiento a tu correo.");
        return "recuperar-contrasenia";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam("token") String token, Model model) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Token inválido o expirado.");
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String token,
                                      @RequestParam String contrasenia,
                                      @RequestParam String confirmarContrasenia,
                                      Model model) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Token inválido o expirado.");
            return "reset-password";
        }

        if (contrasenia.length() < 8) {
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "reset-password";
        }

        if (!contrasenia.equals(confirmarContrasenia)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "reset-password";
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setContraseniaHash(passwordEncoder.encode(contrasenia));
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        passwordResetTokenRepository.delete(resetToken);

        model.addAttribute("success", "Contraseña restablecida exitosamente. Inicia sesión.");
        return "reset-password";
    }

}