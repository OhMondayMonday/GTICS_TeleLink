package com.example.telelink.service;

import com.example.telelink.config.MunicipalidadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ContactoService {

    private static final Logger logger = LoggerFactory.getLogger(ContactoService.class);

    @Autowired
    private MunicipalidadProperties municipalidadProperties;

    public String obtenerInformacionContacto() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("📞 Información de Contacto:\n\n");

            // Verificar que municipalidadProperties no sea null
            if (municipalidadProperties == null) {
                logger.error("MunicipalidadProperties es null");
                return "❌ Error: Configuración de municipalidad no disponible.";
            }

            info.append("🏛️ ").append(obtenerValorSeguro(municipalidadProperties.getNombre(), "Municipalidad")).append("**\n\n");
            info.append("📍 Dirección: ").append(obtenerValorSeguro(municipalidadProperties.getDireccion(), "No disponible")).append("\n");
            info.append("☎️ Teléfono Central: ").append(obtenerValorSeguro(municipalidadProperties.getTelefono(), "No disponible")).append("\n");
            info.append("🚨 Emergencias: ").append(obtenerValorSeguro(municipalidadProperties.getTelefonoEmergencias(), "No disponible")).append("\n");
            info.append("📱 WhatsApp: ").append(obtenerValorSeguro(municipalidadProperties.getWhatsapp(), "No disponible")).append("\n");
            info.append("🌐 Web: ").append(obtenerValorSeguro(municipalidadProperties.getWeb(), "No disponible")).append("\n");
            info.append("📧 Email: ").append(obtenerValorSeguro(municipalidadProperties.getEmail(), "No disponible")).append("\n");

            // Manejo seguro del horario
            String horario = "Lunes a Viernes de 8:00 AM a 5:00 PM"; // Valor por defecto
            try {
                if (municipalidadProperties.getHorario() != null &&
                        municipalidadProperties.getHorario().getAtencion() != null) {
                    horario = municipalidadProperties.getHorario().getAtencion();
                }
            } catch (Exception e) {
                logger.warn("Error al obtener horario, usando valor por defecto", e);
            }

            info.append("🕒 Horario: ").append(horario).append("\n\n");
            info.append("¿Necesitas algo más?");

            return info.toString();

        } catch (Exception e) {
            logger.error("Error al obtener información de contacto", e);
            return "❌ Error al obtener información de contacto. Por favor, intenta más tarde.";
        }
    }

    public String obtenerTelefono() {
        return obtenerValorSeguro(
                municipalidadProperties != null ? municipalidadProperties.getTelefono() : null,
                "No disponible"
        );
    }

    public String obtenerPaginaWeb() {
        return obtenerValorSeguro(
                municipalidadProperties != null ? municipalidadProperties.getWeb() : null,
                "No disponible"
        );
    }

    public String obtenerDireccion() {
        return obtenerValorSeguro(
                municipalidadProperties != null ? municipalidadProperties.getDireccion() : null,
                "No disponible"
        );
    }

    public String obtenerWhatsApp() {
        return obtenerValorSeguro(
                municipalidadProperties != null ? municipalidadProperties.getWhatsapp() : null,
                "No disponible"
        );
    }

    public String obtenerTelefonoEmergencias() {
        return obtenerValorSeguro(
                municipalidadProperties != null ? municipalidadProperties.getTelefonoEmergencias() : null,
                "No disponible"
        );
    }

    public String obtenerNombre() {
        return obtenerValorSeguro(
                municipalidadProperties != null ? municipalidadProperties.getNombre() : null,
                "Municipalidad"
        );
    }

    /**
     * Método utilitario para obtener valores seguros
     */
    private String obtenerValorSeguro(String valor, String valorPorDefecto) {
        return (valor != null && !valor.trim().isEmpty()) ? valor : valorPorDefecto;
    }
}