package com.example.telelink.repository;

import com.example.telelink.entity.Observacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservacionRepository extends JpaRepository<Observacion, Integer> {

    //List<Observacion> findByNivelUrgenciaOrderByNivelUrgenciaAsc(NivelUrgencia nivelUrgencia);

    @Query(value = """
            SELECT o.* FROM observaciones o \
            JOIN espacios_deportivos e ON o.espacio_deportivo_id = e.espacio_deportivo_id \
            JOIN establecimientos_deportivos ed ON e.establecimiento_deportivo_id = ed.establecimiento_deportivo_id \
            JOIN usuarios c ON o.coordinador_id = c.usuario_id \
            WHERE o.nivel_urgencia = :nivelUrgencia""", nativeQuery = true)
    List<Observacion> findAllByNivelUrgenciaWithRelationsNative(Observacion.NivelUrgencia nivelUrgencia);

    List<Observacion> findByCoordinador_UsuarioId(Integer coordinadorId);

    // Recien añadido
    List<Observacion> findByEstadoInOrderByEstadoAsc(List<Observacion.Estado> estados);
    List<Observacion> findByEstadoInAndNivelUrgenciaOrderByEstadoAsc(List<Observacion.Estado> estados, Observacion.NivelUrgencia nivelUrgencia);


}