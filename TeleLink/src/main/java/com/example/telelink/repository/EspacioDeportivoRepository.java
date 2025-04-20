package com.example.telelink.repository;

import com.example.telelink.entity.EspacioDeportivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EspacioDeportivoRepository extends JpaRepository<EspacioDeportivo, Integer> {
}