package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import com.example.telelink.entity.Resenia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReseniaRepository extends JpaRepository<Resenia, Integer> {
    // Para buscar reseñas por espacio deportivo
    List<Resenia> findByEspacioDeportivo_EspacioDeportivoId(Integer espacioDeportivoId);

    // Método nuevo para buscar reseñas por ID de usuario
    List<Resenia> findByUsuario_UsuarioId(Integer usuarioId);

    // Método opcional para contar reseñas por usuario
    int countByUsuario_UsuarioId(Integer usuarioId);
}