package com.example.telelink.repository;

import com.example.telelink.entity.ServicioDeportivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioDeportivoRepository extends JpaRepository<ServicioDeportivo, Integer> {
    @Query("SELECT DISTINCT s FROM EspacioDeportivo e JOIN e.servicioDeportivo s WHERE e.establecimientoDeportivo.establecimientoDeportivoId = :establecimientoId")
    List<ServicioDeportivo> findByEstablecimientoDeportivoId(@Param("establecimientoId") Integer establecimientoId);

    ServicioDeportivo findByServicioDeportivo(String servicioDeportivo);
}