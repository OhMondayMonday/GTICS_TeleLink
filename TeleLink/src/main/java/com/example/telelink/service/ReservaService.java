package com.example.telelink.service;

import com.example.telelink.entity.Reserva;
import com.example.telelink.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    public void guardarReserva(Reserva reserva) {
        // Asegúrate de validar y guardar la reserva en la base de datos
        reservaRepository.save(reserva);
    }

    // Método para obtener todas las reservas
    public List<Reserva> obtenerReservas() {
        return reservaRepository.findAll(); // Obtiene todas las reservas
    }

    // Si necesitas obtener reservas por usuario, puedes hacerlo así:
    public List<Reserva> obtenerReservasPorUsuario(String usuario) {
        return reservaRepository.findByUsuario(usuario); // Asumiendo que tienes un método findByUsuario
    }
}
