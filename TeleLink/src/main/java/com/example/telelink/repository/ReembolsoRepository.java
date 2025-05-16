package com.example.telelink.repository;

import com.example.telelink.entity.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReembolsoRepository extends JpaRepository<Reembolso, Integer> {
    List<Reembolso> findByPago_Reserva_Usuario_UsuarioId(Integer pagoReservaUsuarioUsuarioId);
}