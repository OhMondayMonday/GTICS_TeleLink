package com.example.telelink.repository;

import com.example.telelink.dto.Superadmin.AvisoDTO;
import com.example.telelink.entity.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisoRepository extends JpaRepository<Aviso, Integer> {

    @Query("SELECT a FROM Aviso a WHERE a.estadoAviso = 'activo' ORDER BY a.avisoId ASC LIMIT 1")
    Aviso findLatestAviso();

    @Query(value = "SELECT * FROM avisos " +
            "ORDER BY fecha_aviso DESC " +
            "LIMIT 5",
            nativeQuery = true)
    List<Aviso> obtenerUltimosAvisos();

    @Query("SELECT a FROM Aviso a WHERE a.estadoAviso = 'activo' ORDER BY RAND() LIMIT 1")
    Aviso findAnyAvisoActivo();

    List<Aviso> findByEstadoAviso(String estado);

    @Query("SELECT a FROM Aviso a WHERE a.estadoAviso = 'activo' AND a.estadoAviso <> 'default'")
    List<Aviso> findAvisosActivosNoDefault();

    // Consulta para obtener el aviso default
    @Query("SELECT a FROM Aviso a WHERE a.estadoAviso = 'default'")
    List<Aviso> findAvisoDefault();

}