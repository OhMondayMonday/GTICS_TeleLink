package com.example.telelink.repository;

import com.example.telelink.entity.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReembolsoRepository extends JpaRepository<Reembolso, Integer> {
    List<Reembolso> findByPago_Reserva_Usuario_UsuarioId(Integer pagoReservaUsuarioUsuarioId);

    List<Reembolso> findByEstadoInOrderByFechaReembolsoDesc(List<Reembolso.Estado> estados);
    List<Reembolso> findByEstadoOrderByFechaReembolsoDesc(Reembolso.Estado estado);
    
<<<<<<< Updated upstream
    @Query("SELECT r FROM Reembolso r " +
           "JOIN FETCH r.pago p " +
           "JOIN FETCH p.reserva res " +
           "JOIN FETCH res.usuario u " +
           "JOIN FETCH res.espacioDeportivo ed " +
           "JOIN FETCH ed.establecimientoDeportivo est")
    List<Reembolso> findAllWithRelations();

=======
    /**
     * Verificar si existe un reembolso para un pago específico
     */
    boolean existsByPagoPagoId(Integer pagoId);
    
    /**
     * Buscar reembolso por ID de pago
     */
    Optional<Reembolso> findByPagoPagoId(Integer pagoId);
    
    /**
     * Buscar reembolsos por estado
     */
    List<Reembolso> findByEstado(Reembolso.Estado estado);
>>>>>>> Stashed changes
}