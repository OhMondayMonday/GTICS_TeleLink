select * from roles;
select * from usuarios;

INSERT INTO roles (rol)
VALUES 
('admin'),
('vecino'),
('coordinador');


-- Insertar 2 usuarios con rol Admin (rol_id = 1)
INSERT INTO usuarios (nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, foto_perfil_url, estado_cuenta)
VALUES 
('Carlos', 'Ramírez Díaz', 'admin1@gtics.com', SHA2('admin123', 256), 1, '87654321', 'Av. Siempre Viva 742', '987654321', 'https://i.pravatar.cc/150?img=1', 'activo'),
('Lucía', 'Gonzales Prado', 'admin2@gtics.com', SHA2('admin123', 256), 1, '87654322', 'Calle Falsa 123', '987654322', 'https://i.pravatar.cc/150?img=2', 'activo');

-- Insertar 9 usuarios con rol Vecino (rol_id = 2)
INSERT INTO usuarios (nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, foto_perfil_url, estado_cuenta)
VALUES 
('María', 'Fernández Soto', 'vecino1@gtics.com', SHA2('vecino123', 256), 2, '12345670', 'Jr. Los Cedros 111', '912345670', 'https://i.pravatar.cc/150?img=3', 'activo'),
('Pedro', 'Martínez Rojas', 'vecino2@gtics.com', SHA2('vecino123', 256), 2, '12345671', 'Av. Las Palmeras 222', '912345671', 'https://i.pravatar.cc/150?img=4', 'activo'),
('Rosa', 'Delgado Luján', 'vecino3@gtics.com', SHA2('vecino123', 256), 2, '12345672', 'Calle Jacarandá 333', '912345672', 'https://i.pravatar.cc/150?img=5', 'activo'),
('Javier', 'Salas Pinto', 'vecino4@gtics.com', SHA2('vecino123', 256), 2, '12345673', 'Av. Miraflores 444', '912345673', 'https://i.pravatar.cc/150?img=6', 'activo'),
('Andrea', 'Cuba Torres', 'vecino5@gtics.com', SHA2('vecino123', 256), 2, '12345674', 'Jr. Primavera 555', '912345674', 'https://i.pravatar.cc/150?img=7', 'activo'),
('Luis', 'Benavente López', 'vecino6@gtics.com', SHA2('vecino123', 256), 2, '12345675', 'Calle Algarrobos 666', '912345675', 'https://i.pravatar.cc/150?img=8', 'activo'),
('Paola', 'Reyes Salcedo', 'vecino7@gtics.com', SHA2('vecino123', 256), 2, '12345676', 'Av. Lima 777', '912345676', 'https://i.pravatar.cc/150?img=9', 'activo'),
('Renzo', 'Morales Castañeda', 'vecino8@gtics.com', SHA2('vecino123', 256), 2, '12345677', 'Jr. Arequipa 888', '912345677', 'https://i.pravatar.cc/150?img=10', 'activo'),
('Sofía', 'Vargas León', 'vecino9@gtics.com', SHA2('vecino123', 256), 2, '12345678', 'Calle Cusco 999', '912345678', 'https://i.pravatar.cc/150?img=11', 'activo');

-- Insertar 9 usuarios con rol Coordinador (rol_id = 3)
INSERT INTO usuarios (nombres, apellidos, correo_electronico, contrasenia_hash, rol_id, dni, direccion, telefono, foto_perfil_url, estado_cuenta)
VALUES 
('Ana', 'Sánchez García', 'coordinador1@gtics.com', SHA2('coord123', 256), 3, '22345670', 'Jr. Lima 1001', '922345670', 'https://i.pravatar.cc/150?img=12', 'activo'),
('Marco', 'Paredes Ruiz', 'coordinador2@gtics.com', SHA2('coord123', 256), 3, '22345671', 'Av. Perú 1002', '922345671', 'https://i.pravatar.cc/150?img=13', 'activo'),
('Laura', 'Guevara Peña', 'coordinador3@gtics.com', SHA2('coord123', 256), 3, '22345672', 'Calle Bolívar 1003', '922345672', 'https://i.pravatar.cc/150?img=14', 'activo'),
('Diego', 'Silva Núñez', 'coordinador4@gtics.com', SHA2('coord123', 256), 3, '22345673', 'Av. San Martín 1004', '922345673', 'https://i.pravatar.cc/150?img=15', 'activo'),
('Camila', 'Torres Ibáñez', 'coordinador5@gtics.com', SHA2('coord123', 256), 3, '22345674', 'Jr. Grau 1005', '922345674', 'https://i.pravatar.cc/150?img=16', 'activo'),
('Hugo', 'Ramos Pino', 'coordinador6@gtics.com', SHA2('coord123', 256), 3, '22345675', 'Calle Zela 1006', '922345675', 'https://i.pravatar.cc/150?img=17', 'activo'),
('Natalia', 'Campos Ríos', 'coordinador7@gtics.com', SHA2('coord123', 256), 3, '22345676', 'Av. La Marina 1007', '922345676', 'https://i.pravatar.cc/150?img=18', 'activo'),
('Mario', 'Quispe Vela', 'coordinador8@gtics.com', SHA2('coord123', 256), 3, '22345677', 'Jr. Túpac Amaru 1008', '922345677', 'https://i.pravatar.cc/150?img=19', 'activo'),
('Isabel', 'Chávez Ramos', 'coordinador9@gtics.com', SHA2('coord123', 256), 3, '22345678', 'Calle Libertad 1009', '922345678', 'https://i.pravatar.cc/150?img=20', 'activo');
