package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Reserva;
import com.example.telelink.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Reserva> findByUsuarioOrderByInicioReservaDesc(Usuario usuario);


}