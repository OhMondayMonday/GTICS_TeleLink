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
        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Verifica tu cuenta | Municipalidad de San Miguel</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" />
            <style>
                body {
                    background-color: #f4f4f4;
                    font-family: Arial, sans-serif;
                    color: #333;
                }
                .container {
                    max-width: 600px;
                    margin: 20px auto;
                    background-color: #ffffff;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                }
                .logo {
                    max-width: 150px;
                    margin: 0 auto;
                    display: block;
                }
                .header {
                    text-align: center;
                    margin-bottom: 20px;
                }
                .header h3 {
                    color: #1a73e8;
                    font-size: 24px;
                    margin: 10px 0;
                }
                .content {
                    text-align: center;
                    font-size: 16px;
                    line-height: 1.6;
                }
                .content p {
                    margin: 10px 0;
                }
                .btn-verify {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #1a73e8;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-verify:hover {
                    background-color: #1557b0;
                }
                .footer {
                    text-align: center;
                    font-size: 14px;
                    color: #777;
                    margin-top: 20px;
                }
                .footer p {
                    margin: 5px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>¡Hola, %s!</h3>
                </div>
                <div class="content">
                    <p>Gracias por registrarte en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <p>Por favor, haz clic en el siguiente botón para verificar tu cuenta:</p>
                    <a href="%s" class="btn-verify">Verificar mi cuenta</a>
                    <p>Este enlace expirará en 24 horas.</p>
                    <p>Si no te registraste, ignora este correo.</p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(nombres, verificationUrl, java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String nombres, String token) throws MessagingException {
        String subject = "Restablece tu contraseña - Municipalidad de San Miguel";
        String resetUrl = "http://localhost:8080/reset-password?token=" + token;
        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Restablece tu contraseña | Municipalidad de San Miguel</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" />
            <style>
                body {
                    background-color: #f4f4f4;
                    font-family: Arial, sans-serif;
                    color: #333;
                }
                .container {
                    max-width: 600px;
                    margin: 20px auto;
                    background-color: #ffffff;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                }
                .logo {
                    max-width: 150px;
                    margin: 0 auto;
                    display: block;
                }
                .header {
                    text-align: center;
                    margin-bottom: 20px;
                }
                .header h3 {
                    color: #1a73e8;
                    font-size: 24px;
                    margin: 10px 0;
                }
                .content {
                    text-align: center;
                    font-size: 16px;
                    line-height: 1.6;
                }
                .content p {
                    margin: 10px 0;
                }
                .btn-reset {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #1a73e8;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-reset:hover {
                    background-color: #1557b0;
                }
                .footer {
                    text-align: center;
                    font-size: 14px;
                    color: #777;
                    margin-top: 20px;
                }
                .footer p {
                    margin: 5px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>¡Hola, %s!</h3>
                </div>
                <div class="content">
                    <p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <p>Por favor, haz clic en el siguiente botón para restablecer tu contraseña:</p>
                    <a href="%s" class="btn-reset">Restablecer contraseña</a>
                    <p>Este enlace expirará en 24 horas.</p>
                    <p>Si no solicitaste este cambio, ignora este correo.</p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(nombres, resetUrl, java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

}