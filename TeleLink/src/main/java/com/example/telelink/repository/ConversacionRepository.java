package com.example.telelink.repository;

import com.example.telelink.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Integer> {
    // Buscar conversación activa del usuario
    @Query("SELECT c FROM Conversacion c WHERE c.usuario.usuarioId = :usuarioId AND c.estado = :estado ORDER BY c.inicioConversacion DESC")
    Optional<Conversacion> findByUsuarioIdAndEstado(@Param("usuarioId") Integer usuarioId, @Param("estado") Conversacion.Estado estado);

    // Buscar última conversación del usuario (activa o finalizada)
    Optional<Conversacion> findFirstByUsuarioUsuarioIdOrderByInicioConversacionDesc(Integer usuarioId);

    // Buscar todas las conversaciones de un usuario
    List<Conversacion> findByUsuarioUsuarioIdOrderByInicioConversacionDesc(Integer usuarioId);

}