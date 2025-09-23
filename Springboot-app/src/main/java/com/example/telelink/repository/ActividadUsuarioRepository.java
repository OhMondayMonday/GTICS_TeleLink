package com.example.telelink.repository;

import com.example.telelink.entity.ActividadUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActividadUsuarioRepository extends JpaRepository<ActividadUsuario, Integer> {
    List<ActividadUsuario> findByUsuario_UsuarioId(Integer usuarioId);
}