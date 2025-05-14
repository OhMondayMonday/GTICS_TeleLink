package com.example.telelink.repository;

import com.example.telelink.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    VerificationToken findByToken(String token);

    @Query("SELECT COUNT(t) FROM VerificationToken t WHERE t.usuario.usuarioId = :usuarioId AND t.expiryDate > CURRENT_TIMESTAMP")
    int countActiveTokensByUsuarioId(Integer usuarioId);

}