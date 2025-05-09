package com.example.telelink.repository;

import com.example.telelink.entity.Resenia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReseniaRepository extends JpaRepository<Resenia, Integer> {
    List<Resenia> findByEspacioDeportivo_EspacioDeportivoId(Integer espacioDeportivoId);
}