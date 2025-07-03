package com.example.telelink.service;

import com.example.telelink.entity.Asistencia;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
        String verificationUrl = "https://deportesanmiguel.site/verify?token=" + token;
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
        String resetUrl = "https://deportesanmiguel.site/reset-password?token=" + token;
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

    public void sendAssistanceNotification(Usuario coordinador, Asistencia asistencia) throws MessagingException {
        String subject = "Nueva Asistencia Asignada - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = asistencia.getHorarioEntrada().toLocalDate().format(dateFormatter);
        String formattedEntrada = asistencia.getHorarioEntrada().toLocalTime().format(timeFormatter);
        String formattedSalida = asistencia.getHorarioSalida().toLocalTime().format(timeFormatter);
        String espacioNombre = asistencia.getEspacioDeportivo().getNombre();
        String establecimientoNombre = asistencia.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nueva Asistencia Asignada | Municipalidad de San Miguel</title>
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
                .btn-view {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #1a73e8;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-view:hover {
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
                .details {
                    background-color: #f9f9f9;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
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
                    <p>Te hemos asignado una nueva asistencia en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <div class="details">
                        <p><strong>Fecha:</strong> %s</p>
                        <p><strong>Horario:</strong> %s - %s</p>
                        <p><strong>Espacio Deportivo:</strong> %s</p>
                        <p><strong>Establecimiento:</strong> %s</p>
                    </div>
                    <p>Por favor, revisa los detalles en el sistema:</p>
                    <a href="https://deportesanmiguel.site/coordinador/asistencia" class="btn-view">Ver Asistencia</a>
                    <p>Si tienes alguna duda, contacta con el administrador.</p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(coordinador.getNombres(), formattedFecha, formattedEntrada, formattedSalida,
                espacioNombre, establecimientoNombre, java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(coordinador.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendReservationConfirmation(Usuario usuario, Reserva reserva) throws MessagingException {
        String subject = "Confirmación de Reserva - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = reserva.getInicioReserva().toLocalDate().format(dateFormatter);
        String formattedInicio = reserva.getInicioReserva().toLocalTime().format(timeFormatter);
        String formattedFin = reserva.getFinReserva().toLocalTime().format(timeFormatter);
        String espacioNombre = reserva.getEspacioDeportivo().getNombre();
        String establecimientoNombre = reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Confirmación de Reserva | Municipalidad de San Miguel</title>
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
                .btn-view {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #1a73e8;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-view:hover {
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
                .details {
                    background-color: #f9f9f9;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
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
                    <p>Tu reserva ha sido confirmada exitosamente en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <div class="details">
                        <p><strong>Fecha:</strong> %s</p>
                        <p><strong>Horario:</strong> %s - %s</p>
                        <p><strong>Espacio Deportivo:</strong> %s</p>
                        <p><strong>Establecimiento:</strong> %s</p>
                        <p><strong>Monto Pagado:</strong> S/ %s</p>
                    </div>
                    <p>Por favor, revisa los detalles en el sistema:</p>
                    <a href="https://deportesanmiguel.site/usuarios/mis-reservas" class="btn-view">Ver Reserva</a>
                    <p>Si tienes alguna duda, contacta con el administrador.</p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(usuario.getNombres(), formattedFecha, formattedInicio, formattedFin,
                espacioNombre, establecimientoNombre, monto.toString(),
                java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(usuario.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }    /**
     * Envía un correo de notificación de cancelación de asistencia
     */
    public void sendAssistanceCancellation(Usuario coordinador, Asistencia asistencia, String motivo) throws MessagingException {
        String subject = "Asistencia Reasignada - Municipalidad de San Miguel";
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        String formattedFecha = asistencia.getHorarioEntrada().toLocalDate().format(dateFormatter);
        String formattedInicio = asistencia.getHorarioEntrada().toLocalTime().format(timeFormatter);
        String formattedFin = asistencia.getHorarioSalida().toLocalTime().format(timeFormatter);
        String espacioNombre = asistencia.getEspacioDeportivo().getNombre();
        String establecimientoNombre = asistencia.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Asistencia Reasignada | Municipalidad de San Miguel</title>
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
                    background-color: #fff;
                    border-radius: 8px;
                    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                    overflow: hidden;
                }
                .header {
                    background: linear-gradient(135deg, #ffc107, #ff8c00);
                    color: #fff;
                    padding: 20px;
                    text-align: center;
                }
                .logo {
                    width: 80px;
                    height: 80px;
                    border-radius: 50%%;
                    margin-bottom: 10px;
                }
                .content {
                    padding: 20px;
                }
                .details {
                    background-color: #f8f9fa;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    border-left: 4px solid #ffc107;
                }
                .btn-view {
                    display: inline-block;
                    background-color: #007bff;
                    color: #fff;
                    padding: 10px 20px;
                    text-decoration: none;
                    border-radius: 5px;
                    margin: 10px 0;
                }
                .footer {
                    background-color: #f8f9fa;
                    padding: 15px;
                    text-align: center;
                    color: #6c757d;
                    font-size: 14px;
                }
                .alert {
                    background-color: #fff3cd;
                    color: #856404;
                    padding: 10px;
                    border-radius: 5px;
                    border: 1px solid #ffeaa7;
                    margin: 15px 0;
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
                    <div class="alert">
                        <strong>Tu asistencia ha sido reasignada</strong>
                    </div>
                    <p>Te informamos que tu asistencia programada ha sido reasignada por la administración a otro coordinador.</p>
                    <div class="details">
                        <p><strong>Fecha:</strong> %s</p>
                        <p><strong>Horario:</strong> %s - %s</p>
                        <p><strong>Espacio Deportivo:</strong> %s</p>
                        <p><strong>Establecimiento:</strong> %s</p>
                        <p><strong>Motivo:</strong> %s</p>
                    </div>
                    <p>Puedes revisar el estado de tus asistencias en el sistema:</p>
                    <a href="https://deportesanmiguel.site/coordinador/asistencias" class="btn-view">Ver Mis Asistencias</a>
                    <p>Si tienes alguna duda, contacta con el administrador.</p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(coordinador.getNombres(), formattedFecha, formattedInicio, formattedFin,
                espacioNombre, establecimientoNombre, motivo,
                java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(coordinador.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendReservationCreated(Usuario usuario, Reserva reserva) throws MessagingException {
        String subject = "Reserva Creada - Pendiente de Pago - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = reserva.getInicioReserva().toLocalDate().format(dateFormatter);
        String formattedInicio = reserva.getInicioReserva().toLocalTime().format(timeFormatter);
        String formattedFin = reserva.getFinReserva().toLocalTime().format(timeFormatter);
        String espacioNombre = reserva.getEspacioDeportivo().getNombre();
        String tipoDeporte = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        String establecimientoNombre = reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        if (duracionHoras < 1) duracionHoras = 1; // Mínimo 1 hora
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reserva Creada | Municipalidad de San Miguel</title>
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
                    color: #ff9800;
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
                .btn-pay {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #ff9800;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-pay:hover {
                    background-color: #f57c00;
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
                .details {
                    background-color: #fff3e0;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
                    border-left: 4px solid #ff9800;
                }
                .alert-warning {
                    background-color: #fff3cd;
                    border: 1px solid #ffeaa7;
                    color: #856404;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>🏃‍♂️ Reserva Creada - Pendiente de Pago</h3>
                </div>
                <div class="content">
                    <p>¡Hola, <strong>%s</strong>!</p>
                    <p>Hemos recibido tu solicitud de reserva en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <div class="details">
                        <p><strong>📅 Fecha:</strong> %s</p>
                        <p><strong>⏰ Horario:</strong> %s - %s</p>
                        <p><strong>🏟️ Espacio Deportivo:</strong> %s</p>
                        <p><strong>⚽ Tipo de Deporte:</strong> %s</p>
                        <p><strong>🏢 Establecimiento:</strong> %s</p>
                        <p><strong>⏱️ Duración:</strong> %d hora(s)</p>
                        <p><strong>💰 Monto a Pagar:</strong> S/ %s</p>
                        <p><strong>📋 Estado:</strong> <span style="color: #ff9800; font-weight: bold;">PENDIENTE DE PAGO</span></p>
                    </div>
                    <div class="alert-warning">
                        <strong>⚠️ Importante:</strong> Tu reserva está en estado pendiente. Debes completar el pago para confirmarla. 
                        Las reservas no pagadas pueden ser canceladas automáticamente.
                    </div>
                    <p>Completa tu pago para confirmar la reserva:</p>
                    <a href="https://deportesanmiguel.site/usuarios/mis-reservas" class="btn-pay">💳 Completar Pago</a>
                    <p><small>También puedes acceder a tus reservas desde el menú principal del sistema.</small></p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                    <p>📧 contacto@munisanmiguel.gob.pe | 📞 (01) 567-8900</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(usuario.getNombres(), formattedFecha, formattedInicio, formattedFin,
            espacioNombre, tipoDeporte, establecimientoNombre, duracionHoras, monto.toString(),
            java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(usuario.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendReservationCancelled(Usuario usuario, Reserva reserva, String razonCancelacion) throws MessagingException {
        String subject = "Reserva Cancelada - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = reserva.getInicioReserva().toLocalDate().format(dateFormatter);
        String formattedInicio = reserva.getInicioReserva().toLocalTime().format(timeFormatter);
        String formattedFin = reserva.getFinReserva().toLocalTime().format(timeFormatter);
        String espacioNombre = reserva.getEspacioDeportivo().getNombre();
        String tipoDeporte = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        String establecimientoNombre = reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reserva Cancelada | Municipalidad de San Miguel</title>
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
                    color: #d32f2f;
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
                .btn-view {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #1a73e8;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-view:hover {
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
                .details {
                    background-color: #ffebee;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
                    border-left: 4px solid #d32f2f;
                }
                .reason-box {
                    background-color: #f5f5f5;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                    font-style: italic;
                    border-left: 3px solid #9e9e9e;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>❌ Reserva Cancelada</h3>
                </div>
                <div class="content">
                    <p>¡Hola, <strong>%s</strong>!</p>
                    <p>Te confirmamos que tu reserva ha sido <strong>cancelada</strong> en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <div class="details">
                        <p><strong>📅 Fecha:</strong> %s</p>
                        <p><strong>⏰ Horario:</strong> %s - %s</p>
                        <p><strong>🏟️ Espacio Deportivo:</strong> %s</p>
                        <p><strong>⚽ Tipo de Deporte:</strong> %s</p>
                        <p><strong>🏢 Establecimiento:</strong> %s</p>
                        <p><strong>📋 Estado:</strong> <span style="color: #d32f2f; font-weight: bold;">CANCELADA</span></p>
                    </div>
                    <div class="reason-box">
                        <strong>💬 Razón de cancelación:</strong><br>
                        "%s"
                    </div>
                    <p>Si hubo un pago asociado y cumples con las políticas de reembolso, procesaremos la devolución en los próximos días laborables.</p>
                    <p>Puedes revisar el estado de tus reservas en cualquier momento:</p>
                    <a href="https://deportesanmiguel.site/usuarios/mis-reservas" class="btn-view">📋 Ver Mis Reservas</a>
                    <p>Si necesitas realizar una nueva reserva, estamos aquí para ayudarte.</p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                    <p>📧 contacto@munisanmiguel.gob.pe | 📞 (01) 567-8900</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(usuario.getNombres(), formattedFecha, formattedInicio, formattedFin,
            espacioNombre, tipoDeporte, establecimientoNombre, razonCancelacion != null ? razonCancelacion : "No especificada",
            java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(usuario.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendReservationPaymentCompleted(Usuario usuario, Reserva reserva, BigDecimal montoPageado) throws MessagingException {
        String subject = "✅ Pago Confirmado - Reserva Confirmada - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = reserva.getInicioReserva().toLocalDate().format(dateFormatter);
        String formattedInicio = reserva.getInicioReserva().toLocalTime().format(timeFormatter);
        String formattedFin = reserva.getFinReserva().toLocalTime().format(timeFormatter);
        String espacioNombre = reserva.getEspacioDeportivo().getNombre();
        String tipoDeporte = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        String establecimientoNombre = reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        if (duracionHoras < 1) duracionHoras = 1;

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Pago Confirmado | Municipalidad de San Miguel</title>
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
                    color: #4caf50;
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
                .btn-view {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #4caf50;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-view:hover {
                    background-color: #45a049;
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
                .details {
                    background-color: #e8f5e8;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
                    border-left: 4px solid #4caf50;
                }
                .success-alert {
                    background-color: #d4edda;
                    border: 1px solid #c3e6cb;
                    color: #155724;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                }
                .payment-info {
                    background-color: #f8f9fa;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                    border: 1px solid #dee2e6;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>✅ ¡Pago Confirmado!</h3>
                </div>
                <div class="content">
                    <p>¡Hola, <strong>%s</strong>!</p>
                    <p>¡Excelente! Hemos confirmado tu pago y tu reserva está ahora <strong>confirmada</strong>.</p>
                    <div class="success-alert">
                        <strong>🎉 ¡Tu reserva está confirmada!</strong> Ya puedes usar el espacio deportivo en la fecha y hora programadas.
                    </div>
                    <div class="details">
                        <p><strong>📅 Fecha:</strong> %s</p>
                        <p><strong>⏰ Horario:</strong> %s - %s</p>
                        <p><strong>🏟️ Espacio Deportivo:</strong> %s</p>
                        <p><strong>⚽ Tipo de Deporte:</strong> %s</p>
                        <p><strong>🏢 Establecimiento:</strong> %s</p>
                        <p><strong>⏱️ Duración:</strong> %d hora(s)</p>
                        <p><strong>📋 Estado:</strong> <span style="color: #4caf50; font-weight: bold;">CONFIRMADA</span></p>
                    </div>
                    <div class="payment-info">
                        <p><strong>💳 Información del Pago:</strong></p>
                        <p><strong>Monto Pagado:</strong> S/ %s</p>
                        <p><strong>Fecha de Pago:</strong> %s</p>
                    </div>
                    <p><strong>📝 Recordatorios importantes:</strong></p>
                    <ul style="text-align: left; max-width: 400px; margin: 0 auto;">
                        <li>Llega puntualmente a tu reserva</li>
                        <li>Presenta tu DNI al llegar al establecimiento</li>
                        <li>Respeta las normas del establecimiento</li>
                        <li>Si necesitas cancelar, hazlo con 48 horas de anticipación</li>
                    </ul>
                    <p>Puedes ver los detalles completos de tu reserva:</p>
                    <a href="https://deportesanmiguel.site/usuarios/mis-reservas" class="btn-view">📋 Ver Mi Reserva</a>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                    <p>📧 contacto@munisanmiguel.gob.pe | 📞 (01) 567-8900</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(usuario.getNombres(), formattedFecha, formattedInicio, formattedFin,
            espacioNombre, tipoDeporte, establecimientoNombre, duracionHoras, montoPageado.toString(),
            LocalDate.now().format(dateFormatter), java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(usuario.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendReservationCreated(Usuario usuario, Reserva reserva) throws MessagingException {
        String subject = "Reserva Creada - Pendiente de Pago - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = reserva.getInicioReserva().toLocalDate().format(dateFormatter);
        String formattedInicio = reserva.getInicioReserva().toLocalTime().format(timeFormatter);
        String formattedFin = reserva.getFinReserva().toLocalTime().format(timeFormatter);
        String espacioNombre = reserva.getEspacioDeportivo().getNombre();
        String tipoDeporte = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        String establecimientoNombre = reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        if (duracionHoras < 1) duracionHoras = 1; // Mínimo 1 hora
        BigDecimal monto = reserva.getEspacioDeportivo().getPrecioPorHora().multiply(BigDecimal.valueOf(duracionHoras));

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reserva Creada | Municipalidad de San Miguel</title>
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
                    color: #ff9800;
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
                .btn-pay {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #ff9800;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-pay:hover {
                    background-color: #f57c00;
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
                .details {
                    background-color: #fff3e0;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
                    border-left: 4px solid #ff9800;
                }
                .alert-warning {
                    background-color: #fff3cd;
                    border: 1px solid #ffeaa7;
                    color: #856404;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>🏃‍♂️ Reserva Creada - Pendiente de Pago</h3>
                </div>
                <div class="content">
                    <p>¡Hola, <strong>%s</strong>!</p>
                    <p>Hemos recibido tu solicitud de reserva en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <div class="details">
                        <p><strong>📅 Fecha:</strong> %s</p>
                        <p><strong>⏰ Horario:</strong> %s - %s</p>
                        <p><strong>🏟️ Espacio Deportivo:</strong> %s</p>
                        <p><strong>⚽ Tipo de Deporte:</strong> %s</p>
                        <p><strong>🏢 Establecimiento:</strong> %s</p>
                        <p><strong>⏱️ Duración:</strong> %d hora(s)</p>
                        <p><strong>💰 Monto a Pagar:</strong> S/ %s</p>
                        <p><strong>📋 Estado:</strong> <span style="color: #ff9800; font-weight: bold;">PENDIENTE DE PAGO</span></p>
                    </div>
                    <div class="alert-warning">
                        <strong>⚠️ Importante:</strong> Tu reserva está en estado pendiente. Debes completar el pago para confirmarla. 
                        Las reservas no pagadas pueden ser canceladas automáticamente.
                    </div>
                    <p>Completa tu pago para confirmar la reserva:</p>
                    <a href="https://deportesanmiguel.site/usuarios/mis-reservas" class="btn-pay">💳 Completar Pago</a>
                    <p><small>También puedes acceder a tus reservas desde el menú principal del sistema.</small></p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                    <p>📧 contacto@munisanmiguel.gob.pe | 📞 (01) 567-8900</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(usuario.getNombres(), formattedFecha, formattedInicio, formattedFin,
            espacioNombre, tipoDeporte, establecimientoNombre, duracionHoras, monto.toString(),
            java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(usuario.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendReservationCancelled(Usuario usuario, Reserva reserva, String razonCancelacion) throws MessagingException {
        String subject = "Reserva Cancelada - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = reserva.getInicioReserva().toLocalDate().format(dateFormatter);
        String formattedInicio = reserva.getInicioReserva().toLocalTime().format(timeFormatter);
        String formattedFin = reserva.getFinReserva().toLocalTime().format(timeFormatter);
        String espacioNombre = reserva.getEspacioDeportivo().getNombre();
        String tipoDeporte = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        String establecimientoNombre = reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reserva Cancelada | Municipalidad de San Miguel</title>
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
                    color: #d32f2f;
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
                .btn-view {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #1a73e8;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-view:hover {
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
                .details {
                    background-color: #ffebee;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
                    border-left: 4px solid #d32f2f;
                }
                .reason-box {
                    background-color: #f5f5f5;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                    font-style: italic;
                    border-left: 3px solid #9e9e9e;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>❌ Reserva Cancelada</h3>
                </div>
                <div class="content">
                    <p>¡Hola, <strong>%s</strong>!</p>
                    <p>Te confirmamos que tu reserva ha sido <strong>cancelada</strong> en el Sistema de Gestión Deportiva de la Municipalidad de San Miguel.</p>
                    <div class="details">
                        <p><strong>📅 Fecha:</strong> %s</p>
                        <p><strong>⏰ Horario:</strong> %s - %s</p>
                        <p><strong>🏟️ Espacio Deportivo:</strong> %s</p>
                        <p><strong>⚽ Tipo de Deporte:</strong> %s</p>
                        <p><strong>🏢 Establecimiento:</strong> %s</p>
                        <p><strong>📋 Estado:</strong> <span style="color: #d32f2f; font-weight: bold;">CANCELADA</span></p>
                    </div>
                    <div class="reason-box">
                        <strong>💬 Razón de cancelación:</strong><br>
                        "%s"
                    </div>
                    <p>Si hubo un pago asociado y cumples con las políticas de reembolso, procesaremos la devolución en los próximos días laborables.</p>
                    <p>Puedes revisar el estado de tus reservas en cualquier momento:</p>
                    <a href="https://deportesanmiguel.site/usuarios/mis-reservas" class="btn-view">📋 Ver Mis Reservas</a>
                    <p>Si necesitas realizar una nueva reserva, estamos aquí para ayudarte.</p>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                    <p>📧 contacto@munisanmiguel.gob.pe | 📞 (01) 567-8900</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(usuario.getNombres(), formattedFecha, formattedInicio, formattedFin,
            espacioNombre, tipoDeporte, establecimientoNombre, razonCancelacion != null ? razonCancelacion : "No especificada",
            java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(usuario.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void sendReservationPaymentCompleted(Usuario usuario, Reserva reserva, BigDecimal montoPageado) throws MessagingException {
        String subject = "✅ Pago Confirmado - Reserva Confirmada - Municipalidad de San Miguel";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedFecha = reserva.getInicioReserva().toLocalDate().format(dateFormatter);
        String formattedInicio = reserva.getInicioReserva().toLocalTime().format(timeFormatter);
        String formattedFin = reserva.getFinReserva().toLocalTime().format(timeFormatter);
        String espacioNombre = reserva.getEspacioDeportivo().getNombre();
        String tipoDeporte = reserva.getEspacioDeportivo().getServicioDeportivo().getServicioDeportivo();
        String establecimientoNombre = reserva.getEspacioDeportivo().getEstablecimientoDeportivo().getEstablecimientoDeportivoNombre();
        long duracionHoras = Duration.between(reserva.getInicioReserva(), reserva.getFinReserva()).toHours();
        if (duracionHoras < 1) duracionHoras = 1;

        String content = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Pago Confirmado | Municipalidad de San Miguel</title>
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
                    color: #4caf50;
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
                .btn-view {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #4caf50;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                    font-size: 16px;
                    margin: 20px 0;
                }
                .btn-view:hover {
                    background-color: #45a049;
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
                .details {
                    background-color: #e8f5e8;
                    padding: 15px;
                    border-radius: 5px;
                    margin: 15px 0;
                    text-align: left;
                    border-left: 4px solid #4caf50;
                }
                .success-alert {
                    background-color: #d4edda;
                    border: 1px solid #c3e6cb;
                    color: #155724;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                }
                .payment-info {
                    background-color: #f8f9fa;
                    padding: 12px;
                    border-radius: 5px;
                    margin: 15px 0;
                    border: 1px solid #dee2e6;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <img src="https://yt3.googleusercontent.com/IA0pZ7odBt38SOmTEZYD4K70xlsEzZz7zwAwwdm0tdcBxgCOL43tJdJtUuuXmkiOp6r0BXEW_g8=s900-c-k-c0x00ffffff-no-rj" alt="Municipalidad de San Miguel" class="logo">
                    <h3>✅ ¡Pago Confirmado!</h3>
                </div>
                <div class="content">
                    <p>¡Hola, <strong>%s</strong>!</p>
                    <p>¡Excelente! Hemos confirmado tu pago y tu reserva está ahora <strong>confirmada</strong>.</p>
                    <div class="success-alert">
                        <strong>🎉 ¡Tu reserva está confirmada!</strong> Ya puedes usar el espacio deportivo en la fecha y hora programadas.
                    </div>
                    <div class="details">
                        <p><strong>📅 Fecha:</strong> %s</p>
                        <p><strong>⏰ Horario:</strong> %s - %s</p>
                        <p><strong>🏟️ Espacio Deportivo:</strong> %s</p>
                        <p><strong>⚽ Tipo de Deporte:</strong> %s</p>
                        <p><strong>🏢 Establecimiento:</strong> %s</p>
                        <p><strong>⏱️ Duración:</strong> %d hora(s)</p>
                        <p><strong>📋 Estado:</strong> <span style="color: #4caf50; font-weight: bold;">CONFIRMADA</span></p>
                    </div>
                    <div class="payment-info">
                        <p><strong>💳 Información del Pago:</strong></p>
                        <p><strong>Monto Pagado:</strong> S/ %s</p>
                        <p><strong>Fecha de Pago:</strong> %s</p>
                    </div>
                    <p><strong>📝 Recordatorios importantes:</strong></p>
                    <ul style="text-align: left; max-width: 400px; margin: 0 auto;">
                        <li>Llega puntualmente a tu reserva</li>
                        <li>Presenta tu DNI al llegar al establecimiento</li>
                        <li>Respeta las normas del establecimiento</li>
                        <li>Si necesitas cancelar, hazlo con 48 horas de anticipación</li>
                    </ul>
                    <p>Puedes ver los detalles completos de tu reserva:</p>
                    <a href="https://deportesanmiguel.site/usuarios/mis-reservas" class="btn-view">📋 Ver Mi Reserva</a>
                </div>
                <div class="footer">
                    <p>© %d Municipalidad de San Miguel</p>
                    <p>Sistema de Gestión Deportiva</p>
                    <p>📧 contacto@munisanmiguel.gob.pe | 📞 (01) 567-8900</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(usuario.getNombres(), formattedFecha, formattedInicio, formattedFin,
            espacioNombre, tipoDeporte, establecimientoNombre, duracionHoras, montoPageado.toString(),
            LocalDate.now().format(dateFormatter), java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(usuario.getCorreoElectronico());
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

}