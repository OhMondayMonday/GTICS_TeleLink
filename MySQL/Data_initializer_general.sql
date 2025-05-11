-- Script de Inicialización MySQL para db_gtics
-- Generado el 10 de mayo de 2025
-- Nota: Asegúrate de que el esquema db_gtics esté creado y que la tabla establecimientos_deportivos tenga el campo geolocalizacion.

USE `db_gtics`;

-- 1. Insertar Roles (usando solo los roles existentes)
INSERT INTO roles (rol) VALUES
('superadmin'),
('administrador'),
('vecino'),
('coordinador');

-- 2. Insertar Usuarios
INSERT INTO usuarios (
    nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, foto_perfil_url, estado_cuenta, fecha_creacion
) VALUES
-- Superadmin (1)
('Alejandro', 'Torres', 'superadmin@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 1, '11111111', 'Av. Arequipa 123, Lima', '999111222', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
-- Administradores (2)
('Sofia', 'Ramirez', 'admin.sofia@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 2, '22222222', 'Calle Los Olivos 456, Lima', '999333444', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
('Miguel', 'Vega', 'admin.miguel@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 2, '33333333', 'Av. Javier Prado 789, Lima', '999555666', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
-- Coordinadores (2)
('Laura', 'García', 'coord.laura@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 4, '44444444', 'Calle Las Flores 101, Lima', '999777888', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
('Carlos', 'Mendoza', 'coord.carlos@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 4, '55555555', 'Av. Los Próceres 202, Lima', '999999000', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
-- Vecinos (5)
('Juan', 'Pérez', 'juan.perez@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 3, '66666666', 'Calle Primavera 303, Lima', '987654321', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
('María', 'Gómez', 'maria.gomez@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 3, '77777777', 'Av. Libertad 404, Lima', '987654322', 'https://images.unsplash.com/photo-1532074205216-d0e1f4b87368?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
('Ana', 'Rodríguez', 'ana.rodriguez@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 3, '88888888', 'Pasaje Luna 505, Lima', '987654323', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
('Pedro', 'López', 'pedro.lopez@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 3, '99999999', 'Jirón Sol 606, Lima', '987654324', 'https://images.unsplash.com/photo-1506794778202-6d0d3a3b0a7e?auto=format&fit=crop&w=500&q=80', 'activo', NOW()),
('Lucía', 'Fernández', 'lucia.fernandez@gtics.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV7Z1QljCzLf/6Xr8bsUQ7X1QJQb6', 3, '10101010', 'Av. Central 707, Lima', '987654325', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=500&q=80', 'activo', NOW());

-- 3. Insertar Servicios Deportivos
INSERT INTO servicios_deportivos (servicio_deportivo, fecha_creacion) VALUES
('Piscina', NOW()),
('Pista de Atletismo', NOW()),
('Gimnasio', NOW()),
('Cancha de Fútbol', NOW()),
('Cancha de Básquet', NOW()),
('Cancha de Vóley', NOW()),
('Cancha Multipropósito', NOW());

-- 4. Insertar Establecimientos Deportivos
INSERT INTO establecimientos_deportivos (
    establecimiento_deportivo, descripcion, direccion, espacios_estacionamiento, telefono_contacto, correo_contacto, 
    geolocalizacion, foto_establecimiento_url, horario_apertura, horario_cierre, estado, fecha_creacion
) VALUES
('Complejo Deportivo San Isidro', 'Complejo moderno con múltiples instalaciones deportivas.', 'Av. Javier Prado 123, San Isidro, Lima', 50, '987654321', 'contacto@sanisidro.com', '-12.098145,-77.035672', 'https://images.unsplash.com/photo-1593079831268-3381b0db4a77?auto=format&fit=crop&w=800&q=80', '06:00:00', '22:00:00', 'activo', NOW()),
('Polideportivo Surco', 'Polideportivo con instalaciones de alta calidad.', 'Av. Los Próceres 456, Surco, Lima', 30, '987654322', 'contacto@surco.com', '-12.145123,-77.002345', 'https://images.unsplash.com/photo-1540497077202-7c8a3999166f?auto=format&fit=crop&w=800&q=80', '07:00:00', '21:00:00', 'activo', NOW()),
('Centro Deportivo Miraflores', 'Centro con gimnasio y canchas al aire libre.', 'Av. Larco 789, Miraflores, Lima', 20, '987654323', 'contacto@miraflores.com', '-12.121987,-77.029876', 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=800&q=80', '08:00:00', '20:00:00', 'mantenimiento', NOW());

-- 5. Insertar Espacios Deportivos
INSERT INTO espacios_deportivos (
    nombre, servicio_deportivo_id, establecimiento_deportivo_id, max_personas_por_carril, carriles_piscina, longitud_piscina, profundidad_piscina, 
    descripcion, aforo_gimnasio, longitud_pista, carriles_pista, geolocalizacion, estado_servicio, numero_soporte, horario_apertura, horario_cierre, 
    precio_por_hora, fecha_creacion
) VALUES
-- Piscina
('Piscina Olímpica', 1, 1, 4, 8, 50, 2.00, 'Piscina reglamentaria para competencias.', NULL, NULL, NULL, '-12.098145,-77.035672', 'operativo', '987654321', '06:00:00', '20:00:00', 100.00, NOW()),
-- Pista de Atletismo
('Pista Profesional', 2, 2, NULL, NULL, NULL, NULL, 'Pista de tartán de 400m.', NULL, 400.00, 6, '-12.145123,-77.002345', 'operativo', '987654322', '07:00:00', '19:00:00', 80.00, NOW()),
-- Gimnasio
('Gimnasio Central', 3, 3, NULL, NULL, NULL, NULL, 'Gimnasio equipado con máquinas modernas.', 50, NULL, NULL, '-12.121987,-77.029876', 'mantenimiento', '987654323', '08:00:00', '21:00:00', 150.00, NOW()),
-- Cancha de Fútbol
('Cancha Principal', 4, 1, NULL, NULL, NULL, NULL, 'Cancha de césped sintético para 11 jugadores.', NULL, NULL, NULL, '-12.098145,-77.035672', 'operativo', '987654321', '07:00:00', '22:00:00', 120.00, NOW()),
-- Cancha de Básquet
('Cancha de Básquet 1', 5, 2, NULL, NULL, NULL, NULL, 'Cancha cubierta para básquet profesional.', NULL, NULL, NULL, '-12.145123,-77.002345', 'operativo', '987654322', '07:00:00', '21:00:00', 90.00, NOW()),
-- Cancha de Vóley
('Cancha de Vóley 1', 6, 3, NULL, NULL, NULL, NULL, 'Cancha cubierta para vóley.', NULL, NULL, NULL, '-12.121987,-77.029876', 'clausurado', '987654323', '08:00:00', '20:00:00', 85.00, NOW()),
-- Cancha Multipropósito
('Cancha Multiusos', 7, 1, NULL, NULL, NULL, NULL, 'Cancha para fútbol sala, básquet y vóley.', NULL, NULL, NULL, '-12.098145,-77.035672', 'operativo', '987654321', '06:00:00', '22:00:00', 110.00, NOW());

-- 6. Insertar Tipos de Actividades
INSERT INTO tipos_actividades (tipo_actividad) VALUES
('Inicio de Sesión'),
('Reserva Creada'),
('Pago Realizado'),
('Mantenimiento Reportado');

-- 7. Insertar Actividad de Usuarios
INSERT INTO actividad_usuarios (
    tipo_actividad_id, usuario_id, accion, detalles, fecha_actividad, direccion_ip, ubicacion, dispositivo
) VALUES
(1, 1, 'Login', 'Inicio de sesión exitoso', NOW(), '192.168.1.1', 'Lima, Perú', 'PC Windows'),
(2, 6, 'Reserva', 'Reserva creada para Piscina Olímpica', NOW(), '192.168.1.2', 'Lima, Perú', 'iPhone 14'),
(3, 7, 'Pago', 'Pago completado para reserva', NOW(), '192.168.1.3', 'Lima, Perú', 'Samsung Galaxy S23'),
(4, 4, 'Reporte', 'Reporte de mantenimiento para Gimnasio', NOW(), '192.168.1.4', 'Lima, Perú', 'MacBook Pro');

-- 8. Insertar Asistencias
INSERT INTO asistencias (
    administrador_id, coordinador_id, espacio_deportivo_id, horario_entrada, horario_salida, registro_entrada, registro_salida, 
    estado_entrada, estado_salida, geolocalizacion, observacion_asistencia, fecha_creacion
) VALUES
(2, 4, 1, '2025-05-10 08:00:00', '2025-05-10 12:00:00', '2025-05-10 08:05:00', '2025-05-10 12:00:00', 'puntual', 'realizado', '-12.098145,-77.035672', 'Sin observaciones', NOW()),
(2, 5, 2, '2025-05-10 09:00:00', '2025-05-10 13:00:00', '2025-05-10 09:15:00', NULL, 'tarde', 'pendiente', '-12.145123,-77.002345', 'Llegó tarde por tráfico', NOW()),
(3, 4, 3, '2025-05-09 14:00:00', '2025-05-09 18:00:00', NULL, NULL, 'inasistencia', 'inasistencia', '-12.121987,-77.029876', 'No se presentó', '2025-05-09 14:00:00'),
(3, 5, 4, '2025-05-11 10:00:00', '2025-05-11 14:00:00', NULL, NULL, 'pendiente', 'pendiente', '-12.098145,-77.035672', 'Programada', NOW());

-- 9. Insertar Avisos
INSERT INTO avisos (
    titulo_aviso, texto_aviso, foto_aviso_url, fecha_aviso, estado_aviso
) VALUES
('Cierre Temporal', 'La pista de atletismo estará en mantenimiento del 15 al 20 de mayo.', 'https://static.vecteezy.com/system/resources/previews/004/431/172/non_2x/warning-notice-on-a-white-background-free-vector.jpg', NOW(), 'activo'),
('Evento Deportivo', 'Torneo de fútbol el próximo sábado.', 'https://static.vecteezy.com/system/resources/previews/004/431/172/non_2x/warning-notice-on-a-white-background-free-vector.jpg', NOW(), 'disponible'),
('Aviso Gimnasio', 'Nuevas máquinas instaladas en el gimnasio.', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80', NOW(), 'activo');

-- 10. Insertar Conversaciones
INSERT INTO conversaciones (
    usuario_id, inicio_conversacion, fin_conversacion, estado
) VALUES
(6, '2025-05-10 10:00:00', NULL, 'en_proceso'),
(7, '2025-05-09 15:00:00', '2025-05-09 15:30:00', 'finalizada'),
(8, '2025-05-08 09:00:00', NULL, 'en_proceso');

-- 11. Insertar Mensajes
INSERT INTO mensajes (
    conversacion_id, texto_mensaje, origen, fecha
) VALUES
(1, 'Hola, necesito ayuda con una reserva.', 'usuario', '2025-05-10 10:00:00'),
(1, 'Claro, ¿para qué espacio deportivo?', 'chatbot', '2025-05-10 10:01:00'),
(2, '¿Cuál es el horario de la piscina?', 'usuario', '2025-05-09 15:00:00'),
(2, 'La piscina abre de 06:00 a 20:00.', 'chatbot', '2025-05-09 15:01:00');

-- 12. Insertar Mantenimientos
INSERT INTO mantenimientos (
    espacio_deportivo_id, motivo, fecha_inicio, fecha_estimada_fin, estado
) VALUES
(3, 'Reparación de máquinas de gimnasio.', '2025-05-10 08:00:00', '2025-05-15 18:00:00', 'en_curso'),
(6, 'Repintado de líneas de la cancha.', '2025-05-12 09:00:00', '2025-05-14 17:00:00', 'pendiente'),
(1, 'Limpieza profunda de la piscina.', '2025-05-08 06:00:00', '2025-05-09 18:00:00', 'finalizado');

-- 13. Insertar Métodos de Pago
INSERT INTO metodos_pago (metodo_pago) VALUES
('Pago Online'),
('Depósito Bancario');

-- 14. Insertar Tipos de Notificaciones
INSERT INTO tipos_notificaciones (tipo_notificacion) VALUES
('Reserva Confirmada'),
('Pago Aprobado'),
('Mantenimiento Programado'),
('Aviso General');

-- 15. Insertar Notificaciones
INSERT INTO notificaciones (
    usuario_id, titulo_notificacion, mensaje, url_redireccion, estado, fecha_creacion, tipo_notificacion_id
) VALUES
(6, 'Reserva Confirmada', 'Tu reserva para la Piscina Olímpica ha sido confirmada.', '/reservas/1', 'no_leido', NOW(), 1),
(7, 'Pago Aprobado', 'El pago para tu reserva ha sido procesado.', '/pagos/1', 'leido', NOW(), 2),
(8, 'Mantenimiento', 'El gimnasio estará en mantenimiento del 10 al 15 de mayo.', '/mantenimientos/1', 'no_leido', NOW(), 3),
(9, 'Evento Deportivo', 'No te pierdas el torneo de fútbol este sábado.', '/avisos/2', 'no_leido', NOW(), 4);

-- 16. Insertar Reservas
INSERT INTO reservas (
    usuario_id, espacio_deportivo_id, inicio_reserva, fin_reserva, numero_carril_piscina, numero_carril_pista, estado, fecha_creacion
) VALUES
(6, 1, '2025-05-11 10:00:00', '2025-05-11 11:00:00', 2, NULL, 'confirmada', NOW()),
(7, 4, '2025-05-12 15:00:00', '2025-05-12 17:00:00', NULL, NULL, 'pendiente', NOW()),
(8, 2, '2025-05-10 08:00:00', '2025-05-10 09:00:00', NULL, 3, 'confirmada', NOW()),
(9, 5, '2025-05-13 18:00:00', '2025-05-13 19:00:00', NULL, NULL, 'cancelada', NOW());

-- 17. Insertar Pagos
INSERT INTO pagos (
    reserva_id, metodo_pago_id, monto, estado_transaccion, transaccion_id, foto_comprobante_url, fecha_pago, detalles_transaccion
) VALUES
(1, 1, 100.00, 'completado', 'TXN001', NULL, NOW(), 'Pago por reserva de piscina.'),
(2, 2, 240.00, 'pendiente', 'TXN002', 'https://images.unsplash.com/photo-1580828343064-fde4fc206bc6?auto=format&fit=crop&w=600&q=80', NOW(), 'Depósito pendiente de verificación.'),
(3, 1, 80.00, 'completado', 'TXN003', NULL, NOW(), 'Pago por reserva de pista.'),
(4, 1, 90.00, 'fallido', 'TXN004', NULL, NOW(), 'Error en el procesamiento del pago.');

-- 18. Insertar Reembolsos
INSERT INTO reembolsos (
    monto, estado, motivo, fecha_reembolso, foto_comprobacion_reembolso_url, detalles_transaccion, pago_id
) VALUES
(90.00, 'pendiente', 'Cancelación de reserva por el usuario.', NOW(), NULL, 'Reembolso en revisión.', 4),
(100.00, 'completado', 'Error en la reserva.', NOW(), 'https://images.unsplash.com/photo-1580828343064-fde4fc206bc6?auto=format&fit=crop&w=600&q=80', 'Reembolso procesado.', 1);

-- 19. Insertar Reseñas
INSERT INTO resenias (
    usuario_id, calificacion, comentario, foto_resenia_url, fecha_creacion, espacio_deportivo_id
) VALUES
(6, 4, 'La piscina está limpia, pero el vestuario necesita mejoras.', 'https://images.unsplash.com/photo-1519337265831-4f1b8e788e97?auto=format&fit=crop&w=800&q=80', NOW(), 1),
(7, 5, 'Excelente cancha de fútbol, muy bien mantenida.', 'https://images.unsplash.com/photo-1517649763962-97ca4d37b74a?auto=format&fit=crop&w=800&q=80', NOW(), 4),
(8, 3, 'La pista necesita reparaciones en algunas zonas.', NULL, NOW(), 2),
(9, 5, 'El gimnasio tiene todo lo necesario para entrenar.', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80', NOW(), 3);

-- 20. Insertar Observaciones
-- Nota: Se especifican valores manuales para observacion_id porque el esquema original no tiene AUTO_INCREMENT
INSERT INTO observaciones (
    observacion_id, fecha_creacion, fecha_actualizacion, descripcion, foto_url, nivel_urgencia, espacio_deportivo_id, coordinador_id, comentario_administrador
) VALUES
(1, NOW(), NOW(), 'Fuga en el vestuario de la piscina.', 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80', 'alto', 1, 4, 'Se programará reparación urgente.'),
(2, NOW(), NOW(), 'Césped desgastado en la cancha de fútbol.', 'https://images.unsplash.com/photo-1517649763962-97ca4d37b74a?auto=format&fit=crop&w=800&q=80', 'medio', 4, 5, 'Se evaluará reemplazo del césped.'),
(3, NOW(), NOW(), 'Máquina rota en el gimnasio.', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80', 'alto', 3, 4, 'Reparación en curso.');


-- Insertar establecimientos deportivos
INSERT INTO `db_gtics`.`establecimientos_deportivos` 
(`establecimiento_deportivo`, `descripcion`, `direccion`, `espacios_estacionamiento`, `telefono_contacto`, `correo_contacto`, `geolocalizacion`, `foto_establecimiento_url`, `horario_apertura`, `horario_cierre`, `estado`) 
VALUES 
    ('Polideportivo Central', 'Polideportivo con múltiples canchas y áreas de gimnasio', 'Av. Los Olivos 123', 50, '987654321', 'contacto@polideportivo.com', '(-12.04318, -77.03061)', 'url_imagen_1.jpg', '06:00', '22:00', 'activo'),
    ('Polideportivo Sur', 'Complejo con pistas de atletismo y gimnasio', 'Av. El Sol 456', 30, '976543210', 'sur@polideportivo.com', '(-12.04622, -77.02837)', 'url_imagen_2.jpg', '06:30', '23:00', 'activo'),
    ('Polideportivo Norte', 'Zona deportiva con cancha de fútbol y piscina', 'Calle 10 N° 789', 40, '954321678', 'contacto@polideportivo-norte.com', '(-12.04011, -77.01648)', 'url_imagen_3.jpg', '07:00', '22:00', 'activo'),
    ('Estadio Atlético', 'Estadio para competencias de atletismo', 'Av. Los Deportes 101', 60, '961234567', 'estadio@atletismo.com', '(-12.04229, -77.01856)', 'url_imagen_4.jpg', '06:00', '20:00', 'activo'),
    ('Centro Deportivo del Lago', 'Centro con piscina olímpica y gimnasio', 'Calle del Lago 202', 35, '945678123', 'contacto@centrodeportivo.com', '(-12.04452, -77.02437)', 'url_imagen_5.jpg', '08:00', '20:00', 'activo');

-- Insertar espacios deportivos para Polideportivo Central
INSERT INTO `db_gtics`.`espacios_deportivos` 
(`nombre`, `servicio_deportivo_id`, `establecimiento_deportivo_id`, `max_personas_por_carril`, `carriles_piscina`, `longitud_piscina`, `profundidad_piscina`, `descripcion`, `aforo_gimnasio`, `longitud_pista`, `carriles_pista`, `geolocalizacion`, `estado_servicio`, `numero_soporte`, `horario_apertura`, `horario_cierre`, `precio_por_hora`)
VALUES
    ('Cancha de Fútbol', 2, 1, NULL, NULL, NULL, NULL, 'Cancha de fútbol para 11 jugadores por lado', NULL, NULL, NULL, '(-12.04318, -77.03061)', 'operativo', '123', '06:00', '22:00', 50.00),
    ('Gimnasio 1', 1, 1, NULL, NULL, NULL, NULL, 'Gimnasio con equipos de pesas', 40, NULL, NULL, '(-12.04318, -77.03061)', 'operativo', '124', '06:00', '22:00', 20.00),
    ('Piscina Olímpica', 4, 1, 10, 5, 50, 2.5, 'Piscina para entrenamiento profesional', NULL, NULL, NULL, '(-12.04318, -77.03061)', 'operativo', '125', '06:00', '22:00', 30.00),
    ('Pista de Atletismo', 3, 1, NULL, NULL, NULL, NULL, 'Pista para competencias y entrenamientos', NULL, 400, NULL, '(-12.04318, -77.03061)', 'operativo', '126', '06:00', '22:00', 25.00),
    ('Cancha de Tenis', 2, 1, NULL, NULL, NULL, NULL, 'Cancha para partidos de tenis', NULL, NULL, NULL, '(-12.04318, -77.03061)', 'operativo', '127', '06:00', '22:00', 15.00);

-- Insertar espacios deportivos para Polideportivo Sur
INSERT INTO `db_gtics`.`espacios_deportivos` 
(`nombre`, `servicio_deportivo_id`, `establecimiento_deportivo_id`, `max_personas_por_carril`, `carriles_piscina`, `longitud_piscina`, `profundidad_piscina`, `descripcion`, `aforo_gimnasio`, `longitud_pista`, `carriles_pista`, `geolocalizacion`, `estado_servicio`, `numero_soporte`, `horario_apertura`, `horario_cierre`, `precio_por_hora`)
VALUES
    ('Cancha de Fútbol', 2, 2, NULL, NULL, NULL, NULL, 'Cancha de fútbol para partidos recreativos', NULL, NULL, NULL, '(-12.04622, -77.02837)', 'operativo', '223', '06:30', '23:00', 50.00),
    ('Gimnasio 2', 1, 2, NULL, NULL, NULL, NULL, 'Gimnasio para ejercicios cardiovasculares', 50, NULL, NULL, '(-12.04622, -77.02837)', 'operativo', '224', '06:30', '23:00', 25.00),
    ('Piscina Cubierta', 4, 2, 8, 4, 25, 1.8, 'Piscina cubierta para entrenamientos', NULL, NULL, NULL, '(-12.04622, -77.02837)', 'operativo', '225', '06:30', '23:00', 35.00),
    ('Pista de Atletismo', 3, 2, NULL, NULL, NULL, NULL, 'Pista para competencias locales', NULL, 400, NULL, '(-12.04622, -77.02837)', 'operativo', '226', '06:30', '23:00', 20.00),
    ('Cancha de Basket', 2, 2, NULL, NULL, NULL, NULL, 'Cancha para partidos de baloncesto', NULL, NULL, NULL, '(-12.04622, -77.02837)', 'operativo', '227', '06:30', '23:00', 15.00);

-- Insertar espacios deportivos para Polideportivo Norte
INSERT INTO `db_gtics`.`espacios_deportivos` 
(`nombre`, `servicio_deportivo_id`, `establecimiento_deportivo_id`, `max_personas_por_carril`, `carriles_piscina`, `longitud_piscina`, `profundidad_piscina`, `descripcion`, `aforo_gimnasio`, `longitud_pista`, `carriles_pista`, `geolocalizacion`, `estado_servicio`, `numero_soporte`, `horario_apertura`, `horario_cierre`, `precio_por_hora`)
VALUES
    ('Cancha de Fútbol', 2, 3, NULL, NULL, NULL, NULL, 'Cancha de fútbol para partidos de campeonato', NULL, NULL, NULL, '(-12.04011, -77.01648)', 'operativo', '323', '07:00', '22:00', 45.00),
    ('Gimnasio 3', 1, 3, NULL, NULL, NULL, NULL, 'Gimnasio con zona de pesas y cardio', 60, NULL, NULL, '(-12.04011, -77.01648)', 'operativo', '324', '07:00', '22:00', 30.00),
    ('Piscina Semi-Olímpica', 4, 3, 8, 4, 30, 2.0, 'Piscina para clases de natación', NULL, NULL, NULL, '(-12.04011, -77.01648)', 'operativo', '325', '07:00', '22:00', 40.00),
    ('Pista de Atletismo', 3, 3, NULL, NULL, NULL, NULL, 'Pista para carreras de 100m y 200m', NULL, 300, NULL, '(-12.04011, -77.01648)', 'operativo', '326', '07:00', '22:00', 25.00),
    ('Cancha de Volleyball', 2, 3, NULL, NULL, NULL, NULL, 'Cancha para partidos de volleyball', NULL, NULL, NULL, '(-12.04011, -77.01648)', 'operativo', '327', '07:00', '22:00', 15.00);


-- Insertar 5 reservas para cada usuario
INSERT INTO `db_gtics`.`reservas` 
(`usuario_id`, `espacio_deportivo_id`, `inicio_reserva`, `fin_reserva`, `numero_carril_piscina`, `numero_carril_pista`, `estado`, `razon_cancelacion`)
VALUES
    -- Reservas para Juan Pérez
    (6, 2, '2025-05-01 10:00:00', '2025-05-01 12:00:00', NULL, NULL, 'confirmada', NULL),
    (5, 1, '2025-05-02 14:00:00', '2025-05-02 16:00:00', NULL, NULL, 'confirmada', NULL),
    (4, 4, '2025-05-03 11:00:00', '2025-05-03 13:00:00', NULL, NULL, 'confirmada', NULL),
    (3, 2, '2025-05-04 09:00:00', '2025-05-04 11:00:00', 5, NULL, 'confirmada', NULL),
    (2, 3, '2025-05-05 12:00:00', '2025-05-05 14:00:00', NULL, NULL, 'confirmada', NULL),

    -- Reservas para Carlos Martínez
    (8, 6, '2025-05-01 07:00:00', '2025-05-01 09:00:00', NULL, NULL, 'confirmada', NULL),
    (6, 5, '2025-05-02 12:00:00', '2025-05-02 14:00:00', NULL, NULL, 'confirmada', NULL),
    (6, 4, '2025-05-03 08:00:00', '2025-05-03 10:00:00', NULL, NULL, 'confirmada', NULL),

    -- Reservas para Luisa Fernández
    (8, 4, '2025-05-02 16:00:00', '2025-05-02 18:00:00', NULL, NULL, 'confirmada', NULL),
    (5, 6, '2025-05-03 10:30:00', '2025-05-03 12:30:00', NULL, NULL, 'confirmada', NULL),
    (4, 5, '2025-05-04 14:00:00', '2025-05-04 16:00:00', NULL, NULL, 'confirmada', NULL),
    (3, 4, '2025-05-05 18:00:00', '2025-05-05 20:00:00', NULL, NULL, 'confirmada', NULL),

    -- Reservas para Jorge Rodríguez
    (2, 1, '2025-05-01 11:00:00', '2025-05-01 13:00:00', NULL, NULL, 'confirmada', NULL),
    (5, 1, '2025-05-02 09:00:00', '2025-05-02 11:00:00', NULL, NULL, 'confirmada', NULL),
    (4, 1, '2025-05-03 12:30:00', '2025-05-03 14:30:00', NULL, NULL, 'confirmada', NULL),
    (3, 2, '2025-05-04 13:00:00', '2025-05-04 15:00:00', NULL, NULL, 'confirmada', NULL),
    (2, 3, '2025-05-05 17:00:00', '2025-05-05 19:00:00', NULL, NULL, 'confirmada', NULL);
