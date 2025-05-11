package com.example.telelink.repository;

import com.example.telelink.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    //List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(String estadoTransaccion, String metodoPago);
    //List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPago(Pago.EstadoTransaccion estadoTransaccion, String metodoPago);

    List<Pago> findByEstadoTransaccionAndMetodoPago_MetodoPagoId(Pago.EstadoTransaccion estadoTransaccion, Integer metodoPagoId);


    // Consulta SQL nativa para obtener el monto total semanal por metodo_pago_id
    @Query(value = "SELECT SUM(p.monto) " +
            "FROM pagos p " +
            "WHERE p.metodo_pago_id = :metodoPagoId " +
            "AND EXTRACT(WEEK FROM p.fecha_pago) = EXTRACT(WEEK FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM p.fecha_pago) = EXTRACT(YEAR FROM CURRENT_DATE)",
            nativeQuery = true)
    Double obtenerMontoSemanalPorMetodoPago(@Param("metodoPagoId") Integer metodoPagoId);

    // Consulta SQL nativa para obtener el monto total mensual por metodo_pago_id
    @Query(value = "SELECT SUM(p.monto) " +
            "FROM pagos p " +
            "WHERE p.metodo_pago_id = :metodoPagoId " +
            "AND EXTRACT(MONTH FROM p.fecha_pago) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM p.fecha_pago) = EXTRACT(YEAR FROM CURRENT_DATE)",
            nativeQuery = true)
    Double obtenerMontoMensualPorMetodoPago(@Param("metodoPagoId") Integer metodoPagoId);

    @Query("SELECT p FROM Pago p " +
            "JOIN FETCH p.reserva r " +
            "JOIN FETCH r.usuario " +
            "JOIN FETCH r.espacioDeportivo ed " +
            "JOIN FETCH ed.establecimientoDeportivo " +
            "JOIN FETCH p.metodoPago")
    List<Pago> findAllWithRelations();


}