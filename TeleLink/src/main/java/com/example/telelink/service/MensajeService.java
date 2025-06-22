package com.example.telelink.service;

import com.example.telelink.entity.Conversacion;
import com.example.telelink.entity.Mensaje;
import com.example.telelink.repository.MensajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MensajeService {

    @Autowired
    private MensajeRepository mensajeRepository;

    public void guardarMensajeDeUsuario(Conversacion conversacion, String contenido) {
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setTextoMensaje(contenido);
        mensaje.setOrigen(Mensaje.Origen.usuario);
        mensaje.setFecha(LocalDateTime.now());
        mensajeRepository.save(mensaje);
    }

    public void guardarMensajeDeChatbot(Conversacion conversacion, String contenido) {
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setTextoMensaje(contenido);
        mensaje.setOrigen(Mensaje.Origen.chatbot);
        mensaje.setFecha(LocalDateTime.now());
        mensajeRepository.save(mensaje);
    }
}


