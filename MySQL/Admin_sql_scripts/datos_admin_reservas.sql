select * from usuarios;

select * from espacios_deportivos;

select * from reservas;


INSERT INTO `db_gtics`.`usuarios` (`nombres`, `apellidos`, `correo_electronico`, `contrasenia_hash`, `rol_id`, `dni`, `direccion`, `telefono`, `estado_cuenta`)
VALUES
('Juan', 'Pérez', 'juan.perez@mail.com', 'hash12345', 1, '777', 'Calle Falsa 123', '987654321', 'activo'),
('Maria', 'Gomez', 'maria.gomez@mail.com', 'hash12345', 2, '23456789', 'Av. Siempre Viva 456', '987654322', 'activo'),
('Carlos', 'Lopez', 'carlos.lopez@mail.com', 'hash12345', 3, '34567890', 'Calle Luna 789', '987654323', 'activo'),
('Ana', 'Martinez', 'ana.martinez@mail.com', 'hash12345', 1, '45678901', 'Av. Sol 101', '987654324', 'activo'),
('Luis', 'Ramirez', 'luis.ramirez@mail.com', 'hash12345', 2, '56789012', 'Calle del Sol 202', '987654325', 'activo');


INSERT INTO `db_gtics`.`servicios_deportivos` (`servicio_deportivo`)
VALUES
('Piscina'),
('Gimnasio'),
('Pista de Atletismo'),
('Fútbol'),
('Tenis');


INSERT INTO `db_gtics`.`establecimientos_deportivos` (`establecimiento_deportivo`, `descripcion`, `direccion`, `telefono_contacto`, `correo_contacto`, `geolocalizacion`, `horario_apertura`, `horario_cierre`, `estado`)
VALUES
('Centro Deportivo A', 'Centro deportivo con piscina y gimnasio', 'Calle 123', '987654321', 'contacto@deportiva.com', '40.712776,-74.005974', '06:00:00', '22:00:00', 'activo'),
('Polideportivo B', 'Polideportivo con pista de atletismo y fútbol', 'Calle 456', '987654322', 'contacto@polideportivo.com', '40.730610,-73.935242', '08:00:00', '20:00:00', 'activo');


INSERT INTO `db_gtics`.`espacios_deportivos` 
(`nombre`, `servicio_deportivo_id`, `establecimiento_deportivo_id`, `descripcion`, `geolocalizacion`, `estado_servicio`, `numero_soporte`, `horario_apertura`, `horario_cierre`)
VALUES
('Piscina A', 1, 1, 'Piscina cubierta', '40.712776,-74.005974', 'operativo', '001', '06:00:00', '22:00:00'),
('Gimnasio A', 2, 1, 'Gimnasio con pesas y cardio', '40.712776,-74.005974', 'operativo', '002', '06:00:00', '22:00:00'),
('Pista A', 3, 2, 'Pista de atletismo de 400 metros', '40.730610,-73.935242', 'operativo', '003', '08:00:00', '20:00:00'),
('Cancha de Fútbol A', 4, 2, 'Cancha de fútbol 11', '40.730610,-73.935242', 'operativo', '004', '08:00:00', '20:00:00');




INSERT INTO `db_gtics`.`reservas` (`usuario_id`, `espacio_deportivo_id`, `inicio_reserva`, `fin_reserva`, `estado`, `fecha_creacion`, `fecha_actualizacion`)
VALUES
(21, 1, '2025-04-06 06:00:00', '2025-04-06 07:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 2, '2025-04-06 07:00:00', '2025-04-06 08:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 13, '2025-04-06 08:00:00', '2025-04-06 09:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(24, 14, '2025-04-06 09:00:00', '2025-04-06 10:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 11, '2025-04-06 10:00:00', '2025-04-06 11:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 12, '2025-04-06 11:00:00', '2025-04-06 12:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 13, '2025-04-06 12:00:00', '2025-04-06 13:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 14, '2025-04-06 13:00:00', '2025-04-06 14:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 1, '2025-04-06 14:00:00', '2025-04-06 15:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 2, '2025-04-07 06:00:00', '2025-04-07 07:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 13, '2025-04-07 07:00:00', '2025-04-07 08:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 14, '2025-04-07 08:00:00', '2025-04-07 09:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(24, 1, '2025-04-07 09:00:00', '2025-04-07 10:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 2, '2025-04-07 10:00:00', '2025-04-07 11:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 13, '2025-04-07 11:00:00', '2025-04-07 12:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 14, '2025-04-07 12:00:00', '2025-04-07 13:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 1, '2025-04-07 13:00:00', '2025-04-07 14:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 2, '2025-04-07 14:00:00', '2025-04-07 15:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 13, '2025-04-08 06:00:00', '2025-04-08 07:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 14, '2025-04-08 07:00:00', '2025-04-08 08:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 1, '2025-04-08 08:00:00', '2025-04-08 09:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(24, 2, '2025-04-08 09:00:00', '2025-04-08 10:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 13, '2025-04-08 10:00:00', '2025-04-08 11:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 14, '2025-04-08 11:00:00', '2025-04-08 12:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 11, '2025-04-08 12:00:00', '2025-04-08 13:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 12, '2025-04-08 13:00:00', '2025-04-08 14:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 13, '2025-04-08 14:00:00', '2025-04-08 15:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 14, '2025-04-09 06:00:00', '2025-04-09 07:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 11, '2025-04-09 07:00:00', '2025-04-09 08:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 12, '2025-04-09 08:00:00', '2025-04-09 09:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(24, 13, '2025-04-09 09:00:00', '2025-04-09 10:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 14, '2025-04-09 10:00:00', '2025-04-09 11:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 1, '2025-04-09 11:00:00', '2025-04-09 12:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 2, '2025-04-09 12:00:00', '2025-04-09 13:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 13, '2025-04-09 13:00:00', '2025-04-09 14:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 14, '2025-04-09 14:00:00', '2025-04-09 15:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 1, '2025-04-10 06:00:00', '2025-04-10 07:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 2, '2025-04-10 07:00:00', '2025-04-10 08:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 13, '2025-04-10 08:00:00', '2025-04-10 09:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(24, 14, '2025-04-10 09:00:00', '2025-04-10 10:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 1, '2025-04-10 10:00:00', '2025-04-10 11:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 2, '2025-04-10 11:00:00', '2025-04-10 12:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 13, '2025-04-10 12:00:00', '2025-04-10 13:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 14, '2025-04-10 13:00:00', '2025-04-10 14:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 1, '2025-04-10 14:00:00', '2025-04-10 15:00:00', 'confirmada', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO db_gtics.reservas (usuario_id, inicio_reserva, fin_reserva)
VALUES
(21, '2025-04-21 09:00:00', '2025-04-21 10:00:00'), -- Lunes
(22, '2025-04-22 10:00:00', '2025-04-22 11:00:00'), -- Martes
(23, '2025-04-23 14:00:00', '2025-04-23 15:00:00'), -- Miércoles
(24, '2025-04-24 16:00:00', '2025-04-24 17:00:00'), -- Jueves
(25, '2025-04-25 08:00:00', '2025-04-25 09:00:00'), -- Viernes
(26, '2025-04-26 11:00:00', '2025-04-26 12:00:00'), -- Sábado
(27, '2025-04-27 13:00:00', '2025-04-27 14:00:00'), -- Domingo
(28, '2025-04-21 10:00:00', '2025-04-21 11:00:00'), -- Lunes
(29, '2025-04-22 12:00:00', '2025-04-22 13:00:00'), -- Martes
(21, '2025-04-23 15:00:00', '2025-04-23 16:00:00'), -- Miércoles
(22, '2025-04-24 09:00:00', '2025-04-24 10:00:00'), -- Jueves
(23, '2025-04-25 12:00:00', '2025-04-25 13:00:00'), -- Viernes
(24, '2025-04-26 14:00:00', '2025-04-26 15:00:00'), -- Sábado
(25, '2025-04-27 09:00:00', '2025-04-27 10:00:00'), -- Domingo
(26, '2025-04-21 14:00:00', '2025-04-21 15:00:00'), -- Lunes
(27, '2025-04-22 16:00:00', '2025-04-22 17:00:00'), -- Martes
(21, '2025-04-23 08:00:00', '2025-04-23 09:00:00'), -- Miércoles
(22, '2025-04-24 11:00:00', '2025-04-24 12:00:00'), -- Jueves
(23, '2025-04-25 13:00:00', '2025-04-25 14:00:00'), -- Viernes
(24, '2025-04-26 12:00:00', '2025-04-26 13:00:00'), -- Sábado
(25, '2025-04-27 15:00:00', '2025-04-27 16:00:00'); -- Domingo


SELECT 
    DATE(inicio_reserva) AS fecha_reserva,
    COUNT(*) AS cantidad_reservas
FROM 
    db_gtics.reservas
GROUP BY 
    DATE(inicio_reserva)
ORDER BY 
    fecha_reserva DESC;
    
SELECT 
    DATE(inicio_reserva) AS fecha_reserva,
    COUNT(*) AS cantidad_reservas
FROM 
    db_gtics.reservas
WHERE 
    DAYOFWEEK(inicio_reserva) BETWEEN 2 AND 7  -- Excluye domingo (1)
GROUP BY 
    DATE(inicio_reserva)
ORDER BY 
    fecha_reserva DESC;


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

