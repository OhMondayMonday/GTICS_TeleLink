INSERT INTO roles (rol) VALUES ('vecino');

-- Vecino 1
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('Juan', 'Pérez', 'juan.perez@example.com', '$2a$10$ejemploDeHash1234567890', 1, '12345678', 'Calle Primavera 123', '987654321', 'activo');

-- Vecino 2
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('María', 'Gómez', 'maria.gomez@example.com', '$2a$10$ejemploDeHash1234567890', 1, '23456789', 'Avenida Libertad 456', '987654322', 'activo');

-- Vecino 3
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('Carlos', 'López', 'carlos.lopez@example.com', '$2a$10$ejemploDeHash1234567890', 1, '34567890', 'Jirón Sol 789', '987654323', 'activo');

-- Vecino 4
INSERT INTO usuarios 
(nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, estado_cuenta)
VALUES 
('Ana', 'Rodríguez', 'ana.rodriguez@example.com', '$2a$10$ejemploDeHash1234567890', 1, '45678901', 'Pasaje Luna 101', '987654324', 'activo');

