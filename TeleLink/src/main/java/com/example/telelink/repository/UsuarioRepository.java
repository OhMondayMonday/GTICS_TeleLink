package com.example.telelink.repository;

import com.example.telelink.dto.admin.CantidadReservasPorDiaDto;
import com.example.telelink.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findAllByOrderByUsuarioIdAsc();

    Usuario findByNombres(String nombres);

    // Para buscar un grupo de usuarios por Rol
    List<Usuario> findAllByRol_Rol(String rol);

    /*
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
     */

    @Query(value = """
            SELECT\s
                dia_nombre AS dia,
                COUNT(*) AS cantidadReservas
            FROM (
                SELECT\s
                    CASE\s
                        WHEN DAYOFWEEK(inicio_reserva) = 1 THEN 'Domingo'
                        WHEN DAYOFWEEK(inicio_reserva) = 2 THEN 'Lunes'
                        WHEN DAYOFWEEK(inicio_reserva) = 3 THEN 'Martes'
                        WHEN DAYOFWEEK(inicio_reserva) = 4 THEN 'Miércoles'
                        WHEN DAYOFWEEK(inicio_reserva) = 5 THEN 'Jueves'
                        WHEN DAYOFWEEK(inicio_reserva) = 6 THEN 'Viernes'
                        WHEN DAYOFWEEK(inicio_reserva) = 7 THEN 'Sábado'
                    END AS dia_nombre
                FROM db_gtics.reservas
            ) AS sub
            GROUP BY dia
            ORDER BY\s
                FIELD(dia, 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo');
            
            """, nativeQuery = true)
    List<CantidadReservasPorDiaDto> obtenerCantidadReservasPorDia();

    @Query("SELECT u FROM Usuario u WHERE u.rol.rol = 'coordinador' ORDER BY u.usuarioId ASC LIMIT 1")
    Optional<Usuario> findFirstCoordinadorByOrderByUsuarioIdAsc();

    Usuario findByUsuarioId(Integer usuarioId);

    // Contar usuarios creados este mes
    @Query("SELECT COUNT(u) FROM Usuario u WHERE YEAR(u.fechaCreacion) = YEAR(CURRENT_DATE) AND MONTH(u.fechaCreacion) = MONTH(CURRENT_DATE)")
    long countUsuariosEsteMes();

    long count();

    // Contar usuarios por estado
    long countByEstadoCuenta(Usuario.EstadoCuenta estado);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE YEAR(u.fechaCreacion) = YEAR(CURRENT_DATE - 1 MONTH) AND MONTH(u.fechaCreacion) = MONTH(CURRENT_DATE - 1 MONTH)")
    long countUsuariosMesPasado();

    // Para validar unicidad del correo
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM Usuario u WHERE u.correoElectronico = :correo AND u.usuarioId != :excludeId")
    boolean existsByCorreoElectronicoAndUsuarioIdNot(
            @Param("correo") String correoElectronico,
            @Param("excludeId") Integer excludeId);

    // Para validar unicidad del teléfono
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM Usuario u WHERE u.telefono = :telefono AND u.usuarioId != :excludeId")
    boolean existsByTelefonoAndUsuarioIdNot(
            @Param("telefono") String telefono,
            @Param("excludeId") Integer excludeId);


    Usuario findByCorreoElectronico(String correoElectronico);
    Usuario findByDni(String dni);

    @Query("SELECT u FROM Usuario u WHERE u.rol.rolId = :rolId ORDER BY u.apellidos, u.nombres")
    List<Usuario> findByRolId(@Param("rolId") Integer rolId);

}