package com.example.telelink.repository;

import com.example.telelink.entity.Pago;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    //List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(String estadoTransaccion, String metodoPago);
    List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(Pago.EstadoTransaccion estadoTransaccion, String metodoPago);

    Optional<Pago> findByReserva(Reserva reserva);

    List<Pago> findByReserva_Usuario(Usuario usuario);

}