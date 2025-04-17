package com.example.telelink.repository;

import com.example.telelink.entity.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoNotificacionRepository extends JpaRepository<TipoNotificacion, Integer> {
}