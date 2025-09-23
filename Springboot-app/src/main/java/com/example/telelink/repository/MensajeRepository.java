package com.example.telelink.repository;

import com.example.telelink.entity.Mensaje;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {
    // Obtener mensajes de una conversación ordenados por fecha
    List<Mensaje> findByConversacionConversacionIdOrderByFechaAsc(Integer conversacionId);

    // Obtener últimos N mensajes de una conversación
    @Query("SELECT m FROM Mensaje m WHERE m.conversacion.conversacionId = :conversacionId ORDER BY m.fecha DESC")
    List<Mensaje> findTopNByConversacionId(@Param("conversacionId") Integer conversacionId, Pageable pageable);

    // Contar mensajes de una conversación
    long countByConversacionConversacionId(Integer conversacionId);
}