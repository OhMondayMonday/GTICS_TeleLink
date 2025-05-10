package com.example.telelink.repository;

import com.example.telelink.dto.admin.CantidadReservasPorDiaDto;
import com.example.telelink.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findAllByOrderByUsuarioIdAsc();
    Optional<Usuario> findByNombres(String nombres);

    // Para buscar un grupo de usuarios por Rol
    List<Usuario> findAllByRol_Rol(String rol);

    Usuario findByCorreoElectronico(String correoElectronico);

    @Query(value = """
            SELECT 
                CASE 
                    WHEN DAYOFWEEK(inicio_reserva) = 1 THEN 'Domingo'
                    WHEN DAYOFWEEK(inicio_reserva) = 2 THEN 'Lunes'
                    WHEN DAYOFWEEK(inicio_reserva) = 3 THEN 'Martes'
                    WHEN DAYOFWEEK(inicio_reserva) = 4 THEN 'Miércoles'
                    WHEN DAYOFWEEK(inicio_reserva) = 5 THEN 'Jueves'
                    WHEN DAYOFWEEK(inicio_reserva) = 6 THEN 'Viernes'
                    WHEN DAYOFWEEK(inicio_reserva) = 7 THEN 'Sábado'
                END AS dia,
                COUNT(*) AS cantidadReservas
            FROM 
                db_gtics.reservas
            GROUP BY 
                dia
            ORDER BY 
                FIELD(dia, 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo');
            """, nativeQuery = true)

    List<CantidadReservasPorDiaDto> obtenerCantidadReservasPorDia();

}