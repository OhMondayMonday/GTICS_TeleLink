package com.example.telelink.repository;

import com.example.telelink.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    //List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(String estadoTransaccion, String metodoPago);
    List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(Pago.EstadoTransaccion estadoTransaccion, String metodoPago);




}