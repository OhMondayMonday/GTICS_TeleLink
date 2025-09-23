package com.example.telelink.repository;

import com.example.telelink.entity.TipoActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoActividadRepository extends JpaRepository<TipoActividad, Integer> {
}