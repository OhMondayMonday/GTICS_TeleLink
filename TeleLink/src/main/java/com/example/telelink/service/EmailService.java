package com.example.telelink.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String nombres, String token) throws MessagingException {
        String subject = "Verifica tu cuenta - Municipalidad de San Miguel";
        String verificationUrl = "http://localhost:8080/verify?token=" + token;
        String content = "<h3>Hola, " + nombres + "</h3>" +
                "<p>Gracias por registrarte en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>" +
                "<p>Por favor, haz clic en el siguiente enlace para verificar tu cuenta:</p>" +
                "<a href=\"" + verificationUrl + "\">Verificar mi cuenta</a>" +
                "<p>Este enlace expirará en 24 horas.</p>" +
                "<p>Si no te registraste, ignora este correo.</p>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }
}