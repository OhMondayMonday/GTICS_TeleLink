INSERT INTO roles (rol) VALUES ('superadmin');
INSERT INTO roles (rol) VALUES ('administrador');
INSERT INTO roles (rol) VALUES ('vecino');
INSERT INTO roles (rol) VALUES ('coordinador');

-- Admin
INSERT INTO usuarios (
    nombres,
    apellidos,
    correo_electronico,
    contrasenia_hash,
    rol_id,
    dni,
    direccion,
    telefono,
    estado_cuenta,
    fecha_creacion
) VALUES (
    'José',
    'Guevara',
    'adminJose@telelink.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6',
    2,
    NULL,
    'Av. Marino Cornejo 123',
    '987654321',
    'activo',
    NOW()
);

-- Vecino 1
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('Juan', 'Pérez', 'juan.perez@example.com', '$2a$10$ejemploDeHash1234567890', 3, '12345678', 'Calle Primavera 123', '987654321', 'activo');

-- Vecino 2
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('María', 'Gómez', 'maria.gomez@example.com', '$2a$10$ejemploDeHash1234567890', 3, '23456789', 'Avenida Libertad 456', '987654322', 'activo');

-- Vecino 3
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('Carlos', 'López', 'carlos.lopez@example.com', '$2a$10$ejemploDeHash1234567890', 3, '34567890', 'Jirón Sol 789', '987654323', 'activo');

-- Vecino 4
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('Ana', 'Rodríguez', 'ana.rodriguez@example.com', '$2a$10$ejemploDeHash1234567890', 3, '45678901', 'Pasaje Luna 101', '987654324', 'activo');

-- 2. Insertar servicios deportivos
INSERT INTO servicios_deportivos (servicio_deportivo_id, servicio_deportivo) VALUES 
(1, 'Natación'),
(2, 'Fútbol'),
(3, 'Atletismo'),
(4, 'Gimnasio');

-- 3. Insertar establecimientos deportivos
INSERT INTO establecimientos_deportivos (
    establecimiento_deportivo_id, 
    establecimiento_deportivo, 
    direccion, 
    geolocalizacion, 
    horario_apertura, 
    horario_cierre
) VALUES 
(1, 'Complejo Deportivo Bancario', 'Av. Javier Prado 123', '-12.123456,-77.123456', '06:00:00', '22:00:00'),
(2, 'Polideportivo Villa El Salvador', 'Av. Central 456', '-12.234567,-77.234567', '07:00:00', '21:00:00');

-- 4. Insertar espacios deportivos
INSERT IGNORE INTO espacios_deportivos (
    espacio_deportivo_id, 
    nombre, 
    servicio_deportivo_id, 
    establecimiento_deportivo_id, 
    estado_servicio,
    horario_apertura,
    horario_cierre
) VALUES 
(1, 'Piscina Olímpica', 1, 1, 'operativo', '06:00:00', '20:00:00'),
(2, 'Cancha de Fútbol 1', 2, 1, 'operativo', '07:00:00', '22:00:00'),
(3, 'Pista de Atletismo', 3, 2, 'operativo', '06:00:00', '20:00:00'),
(4, 'Gimnasio Principal', 4, 2, 'operativo', '08:00:00', '21:00:00');

-- 5. Insertar usuarios coordinadores
INSERT INTO usuarios (
    nombres, 
    apellidos, 
    correo_electronico, 
    contrasenia_hash, 
    rol_id, 
    dni, 
    estado_cuenta
) VALUES 
('Carlos', 'Mendoza', 'coordinador1@email.com', '$2a$10$xJwL5v5Jz5U6Z5b5Z5b5Z.', 4, '02345678', 'activo'),
('Laura', 'García', 'coordinador2@email.com', '$2a$10$xJwL5v5Jz5U6Z5b5Z5b5Z.', 4, '87654321', 'activo');

-- 6. Insertar asistencias para la fecha actual y días cercanos
INSERT INTO asistencias (
    administrador_id,
    coordinador_id,
    espacio_deportivo_id,
    horario_entrada,
    horario_salida,
    estado_entrada,
    estado_salida
) VALUES 
-- Asistencias para hoy
(1, 6, 1, 
 TIMESTAMP(CURRENT_DATE, '08:00:00'), 
 TIMESTAMP(CURRENT_DATE, '12:00:00'), 
 'puntual', 'realizado'),
 
(1, 7, 2, 
 TIMESTAMP(CURRENT_DATE, '09:00:00'), 
 TIMESTAMP(CURRENT_DATE, '13:00:00'), 
 'tarde', 'realizado'),
 
(1, 6, 3, 
 TIMESTAMP(CURRENT_DATE, '14:00:00'), 
 TIMESTAMP(CURRENT_DATE, '18:00:00'), 
 'puntual', 'pendiente'),
 
-- Asistencias para mañana
(1, 7, 4, 
 TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '10:00:00'), 
 TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '14:00:00'), 
 'pendiente', 'pendiente'),
 
-- Asistencias para ayer
(1, 6, 1, 
 TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '07:30:00'), 
 TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '11:30:00'), 
 'puntual', 'realizado'),
 
(1, 7, 2, 
 TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '15:00:00'), 
 TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '19:00:00'), 
 'inasistencia', 'inasistencia');

-- Eventos programados

DELIMITER $$

CREATE EVENT `actualizar_asistencias_inasistencia`
ON SCHEDULE EVERY 1 MINUTE
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    UPDATE asistencias
    SET 
        estado_entrada = 'inasistencia',
        estado_salida = 'inasistencia'
    WHERE 
        horario_salida < CURRENT_TIMESTAMP
        AND estado_entrada = 'pendiente';
END$$

DELIMITER ;