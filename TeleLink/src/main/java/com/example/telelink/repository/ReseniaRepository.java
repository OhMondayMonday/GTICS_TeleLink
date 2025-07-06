package com.example.telelink.repository;
import com.example.telelink.entity.Resenia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ReseniaRepository extends JpaRepository<Resenia, Integer> {
    // Para buscar reseñas por espacio deportivo
    List<Resenia> findByEspacioDeportivo_EspacioDeportivoId(Integer espacioDeportivoId);

    // Metodo nuevo para buscar reseñas por ID de usuario
    List<Resenia> findByUsuario_UsuarioId(Integer usuarioId);

    // Para comentarios destacados (los más recientes con imágenes)
    List<Resenia> findTop2ByEspacioDeportivo_EspacioDeportivoIdAndFotoReseniaUrlIsNotNullOrderByFechaCreacionDesc(
            Integer espacioDeportivoId);

    // Para estadísticas
    long countByEspacioDeportivo_EspacioDeportivoId(Integer espacioDeportivoId);

    // Para el perfil de usuario
    Page<Resenia> findByUsuario_UsuarioId(Integer usuarioId, Pageable pageable);

    // Método existente (con paginación)
    Page<Resenia> findByEspacioDeportivo_EspacioDeportivoId(Integer espacioId, Pageable pageable);


    // ALTERNATIVA: Si no quieres usar @Query, puedes usar este metodo
    List<Resenia> findByEspacioDeportivo_EspacioDeportivoIdOrderByFechaCreacionDesc(Integer espacioId);

}