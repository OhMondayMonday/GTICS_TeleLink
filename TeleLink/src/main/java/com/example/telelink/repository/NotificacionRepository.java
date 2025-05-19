package com.example.telelink.repository;

import com.example.telelink.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    List<Notificacion> findTop3ByUsuarioUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
}