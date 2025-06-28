package com.example.telelink.repository;

import com.example.telelink.entity.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoNotificacionRepository extends JpaRepository<TipoNotificacion, Integer> {
    Optional<TipoNotificacion> findByTipoNotificacion(String tipoNotificacion);
}