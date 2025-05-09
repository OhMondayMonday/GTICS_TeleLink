package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    List<Reserva> findByEspacioDeportivoAndInicioReservaBetweenAndEstadoNot(
            EspacioDeportivo espacioDeportivo,
            LocalDateTime inicio,
            LocalDateTime fin,
            Reserva.Estado estado
    );

    List<Reserva> findByEspacioDeportivoAndEstadoNotAndFinReservaAfterAndInicioReservaBefore(
            EspacioDeportivo espacioDeportivo,
            Reserva.Estado estado,
            LocalDateTime inicio,
            LocalDateTime fin
    );

}