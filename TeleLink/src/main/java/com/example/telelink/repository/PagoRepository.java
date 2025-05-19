package com.example.telelink.repository;

import com.example.telelink.entity.Pago;
import com.example.telelink.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    //List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(String estadoTransaccion, String metodoPago);
    //List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(Pago.EstadoTransaccion estadoTransaccion, String metodoPago);

    List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPagoId(Pago.EstadoTransaccion estadoTransaccion, Integer metodoPagoId);


    @Query(value = "SELECT COALESCE(SUM(p.monto), 0) " +
            "FROM pagos p " +
            "WHERE p.metodo_pago_id = :metodoPagoId " +
            "AND p.estado_transaccion = 'completado' " +  // Asegúrate que coincida con tu enum
            "AND EXTRACT(WEEK FROM p.fecha_pago) = EXTRACT(WEEK FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM p.fecha_pago) = EXTRACT(YEAR FROM CURRENT_DATE)",
            nativeQuery = true)
    BigDecimal obtenerMontoSemanalPorMetodoPago(@Param("metodoPagoId") Integer metodoPagoId);

    @Query(value = "SELECT COALESCE(SUM(p.monto), 0) " +
            "FROM pagos p " +
            "WHERE p.metodo_pago_id = :metodoPagoId " +
            "AND p.estado_transaccion = 'completado' " +  // Asegúrate que coincida con tu enum
            "AND EXTRACT(MONTH FROM p.fecha_pago) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM p.fecha_pago) = EXTRACT(YEAR FROM CURRENT_DATE)",
            nativeQuery = true)
    BigDecimal obtenerMontoMensualPorMetodoPago(@Param("metodoPagoId") Integer metodoPagoId);

    List<Pago> findByReserva_Usuario_UsuarioId(Integer usuarioId);

    Optional<Pago> findByReserva_ReservaId(Integer reservaId);

}