package com.example.telelink.repository;

import com.example.telelink.entity.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AvisoRepository extends JpaRepository<Aviso, Integer> {

    @Query("SELECT a FROM Aviso a WHERE a.fechaAviso = (SELECT MAX(a2.fechaAviso) FROM Aviso a2)")
    Aviso findLatestAviso();

}