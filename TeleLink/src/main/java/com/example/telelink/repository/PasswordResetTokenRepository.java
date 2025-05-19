package com.example.telelink.repository;

import com.example.telelink.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    PasswordResetToken findByToken(String token);

    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.usuario.usuarioId = :usuarioId AND t.expiryDate > CURRENT_TIMESTAMP")
    int countActiveTokensByUsuarioId(Integer usuarioId);

}