package com.example.telelink.repository;
import com.example.telelink.entity.Resenia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ReseniaRepository extends JpaRepository<Resenia, Integer> {

    // Para buscar reseñas por espacio deportivo
    List<Resenia> findByEspacioDeportivo_EspacioDeportivoId(Integer espacioDeportivoId);

    // Para buscar reseñas por ID de usuario (sin paginación)
    List<Resenia> findByUsuario_UsuarioId(Integer usuarioId);

    // Para buscar reseñas por ID de usuario (CON paginación) - METODO PRINCIPAL
    Page<Resenia> findByUsuario_UsuarioId(Integer usuarioId, Pageable pageable);

    // Para comentarios destacados (los más recientes con imágenes)
    List<Resenia> findTop2ByEspacioDeportivo_EspacioDeportivoIdAndFotoReseniaUrlIsNotNullOrderByFechaCreacionDesc(
            Integer espacioDeportivoId);

    // Para estadísticas
    long countByEspacioDeportivo_EspacioDeportivoId(Integer espacioDeportivoId);

    // Para espacios deportivos (con paginación)
    Page<Resenia> findByEspacioDeportivo_EspacioDeportivoId(Integer espacioId, Pageable pageable);

    // Para espacios deportivos (sin paginación, ordenado por fecha)
    List<Resenia> findByEspacioDeportivo_EspacioDeportivoIdOrderByFechaCreacionDesc(Integer espacioId);

    // Metodo adicional para buscar reseñas del usuario ordenadas por fecha (sin paginación)
    List<Resenia> findByUsuario_UsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
}