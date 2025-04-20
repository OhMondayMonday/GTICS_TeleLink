package com.example.telelink.repository;

import com.example.telelink.entity.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReembolsoRepository extends JpaRepository<Reembolso, Integer> {
}