package com.example.telelink.repository;

import com.example.telelink.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    
    // Métodos para el dropdown - obtener las últimas notificaciones
    List<Notificacion> findTop5ByUsuario_UsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    
    // Contar notificaciones no leídas
    long countByUsuario_UsuarioIdAndEstado(Integer usuarioId, Notificacion.Estado estado);
    
    // Obtener todas las notificaciones con filtros y paginación
    @Query("SELECT n FROM Notificacion n WHERE n.usuario.usuarioId = :usuarioId " +
           "AND (:estado IS NULL OR n.estado = :estado) " +
           "AND (:tipoId IS NULL OR n.tipoNotificacion.tipoNotificacionId = :tipoId) " +
           "AND (:fechaInicio IS NULL OR n.fechaCreacion >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR n.fechaCreacion <= :fechaFin)")
    Page<Notificacion> findNotificacionesConFiltros(
        @Param("usuarioId") Integer usuarioId,
        @Param("estado") Notificacion.Estado estado,
        @Param("tipoId") Integer tipoId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        Pageable pageable
    );
    
    // Obtener todas las notificaciones de un usuario ordenadas por fecha
    Page<Notificacion> findByUsuario_UsuarioIdOrderByFechaCreacionDesc(Integer usuarioId, Pageable pageable);
    
    // Método heredado (mantenemos compatibilidad)
    List<Notificacion> findTop3ByUsuarioUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
}