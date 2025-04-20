package com.example.telelink.repository;

import com.example.telelink.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Integer> {
}