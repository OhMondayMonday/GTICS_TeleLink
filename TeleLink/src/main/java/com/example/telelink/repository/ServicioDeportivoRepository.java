package com.example.telelink.repository;

import com.example.telelink.entity.ServicioDeportivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicioDeportivoRepository extends JpaRepository<ServicioDeportivo, Integer> {
}