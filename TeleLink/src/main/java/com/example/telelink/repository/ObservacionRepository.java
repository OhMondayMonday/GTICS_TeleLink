package com.example.telelink.repository;

import com.example.telelink.entity.Observacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObservacionRepository extends JpaRepository<Observacion, Integer> {
}