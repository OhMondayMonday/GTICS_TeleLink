package com.example.telelink.repository;

import com.example.telelink.entity.EstablecimientoDeportivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstablecimientoDeportivoRepository extends JpaRepository<EstablecimientoDeportivo, Integer> {
}