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
    telefono,
    estado_cuenta
) VALUES 
('Carlos', 'Mendoza', 'coordinador1@email.com', '$2a$10$xJwL5v5Jz5U6Z5b5Z5b5Z.', 4, '02345678','987654321', 'activo'),
('Laura', 'García', 'coordinador2@email.com', '$2a$10$xJwL5v5Jz5U6Z5b5Z5b5Z.', 4, '87654321', '987654320', 'activo');

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
 
-- aviso de prueba

INSERT INTO db_gtics.avisos (titulo_aviso, texto_aviso, foto_aviso_url, fecha_aviso)
VALUES ('Aviso de prueba', 'Este es un aviso de prueba para el coordinador.', 'https://example.com/imagen.jpg', CURRENT_TIMESTAMP);

INSERT INTO db_gtics.avisos (titulo_aviso, texto_aviso, foto_aviso_url, fecha_aviso)
VALUES ('Aviso de prueba 2', 'Este es un aviso de prueba para el coordinador.', 'https://static.vecteezy.com/system/resources/previews/004/431/172/non_2x/warning-notice-on-a-white-background-free-vector.jpg', CURRENT_TIMESTAMP);


-- 1. Insertar reseñas para probar el rating dinámico
INSERT INTO resenias (usuario_id, calificacion, comentario, espacio_deportivo_id, fecha_creacion)
VALUES 
(2, 4, 'Buena cancha, pero necesita mantenimiento.', 2, NOW()),
(3, 5, 'Excelente lugar para jugar fútbol.', 2, NOW()),
(4, 3, 'Regular, el césped está desgastado.', 2, NOW()),
(5, 5, 'La piscina está muy limpia.', 1, NOW()),
(6, 4, 'Buena experiencia en la piscina.', 1, NOW());

-- 2. Actualizar EspacioDeportivo para agregar precio_por_hora
UPDATE espacios_deportivos 
SET precio_por_hora = 100.00
WHERE espacio_deportivo_id = 1;

UPDATE espacios_deportivos 
SET precio_por_hora = 120.00
WHERE espacio_deportivo_id = 2;

UPDATE espacios_deportivos 
SET precio_por_hora = 80.00
WHERE espacio_deportivo_id = 3;

UPDATE espacios_deportivos 
SET precio_por_hora = 150.00
WHERE espacio_deportivo_id = 4;

-- 3. Actualizar EstablecimientoDeportivo para agregar foto_establecimiento_url
UPDATE establecimientos_deportivos 
SET foto_establecimiento_url = 'https://i.ytimg.com/vi/3hXz2cSdYdI/hq720.jpg?sqp=-oaymwEhCK4FEIIDSFryq4qpAxMIARUAAAAAGAElAADIQj0AgKJD&rs=AOn4CLANRtqmrsU0GKt1AhImtSNaMBz_YA'
WHERE establecimiento_deportivo_id = 1;

UPDATE establecimientos_deportivos 
SET foto_establecimiento_url = 'https://i.ytimg.com/vi/3hXz2cSdYdI/hq720.jpg?sqp=-oaymwEhCK4FEIIDSFryq4qpAxMIARUAAAAAGAElAADIQj0AgKJD&rs=AOn4CLANRtqmrsU0GKt1AhImtSNaMBz_YA'
WHERE establecimiento_deportivo_id = 2;

-- 4. Cambiar estado_servicio de algunos EspacioDeportivo para probar diferentes estados
UPDATE espacios_deportivos 
SET estado_servicio = 'mantenimiento'
WHERE espacio_deportivo_id = 3;

UPDATE espacios_deportivos 
SET estado_servicio = 'clausurado'
WHERE espacio_deportivo_id = 4;

-- Nuevas asistencias
INSERT INTO asistencias (coordinador_id, administrador_id, espacio_deportivo_id, horario_entrada, horario_salida, fecha_creacion)
VALUES (6, 1, 1, '2025-05-09 08:00:00', '2025-05-09 15:00:00', '2025-05-09 07:00:00');

INSERT INTO asistencias (coordinador_id, administrador_id, espacio_deportivo_id, horario_entrada, horario_salida, geolocalizacion, fecha_creacion)
VALUES (6, 1, 1, '2025-05-09 08:00:00', '2025-05-09 18:00:00', '-12.046374,-77.042793', '2025-05-09 16:00:00');


-- coordenadas para los espacios deportivos
UPDATE espacios_deportivos 
SET geolocalizacion = 
    CASE 
        WHEN espacio_deportivo_id = 1 THEN '-12.098145,-77.035672' -- Piscina Olímpica (San Isidro)
        WHEN espacio_deportivo_id = 2 THEN '-12.145123,-77.002345' -- Cancha de Fútbol 1 (Surco)
        WHEN espacio_deportivo_id = 3 THEN '-12.076543,-77.054321' -- Pista de Atletismo (Jesús María)
        WHEN espacio_deportivo_id = 4 THEN '-12.121987,-77.029876' -- Gimnasio Principal (Miraflores)
    END
WHERE espacio_deportivo_id IN (1, 2, 3, 4);