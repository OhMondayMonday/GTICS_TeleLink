DROP DATABASE if EXISTS db_gtics;
CREATE DATABASE db_gtics;
USE db_gtics;

DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    rol_id INT AUTO_INCREMENT PRIMARY KEY,
    rol NOT NULL VARCHAR (20),
);

DROP TABLE IF EXISTS usuarios;
CREATE TABLE usuarios (
    usuario_id INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100),
    apellidos VARCHAR(100)
    correo_electronico VARCHAR(100) UNIQUE NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol_id INT NOT NULL,
    dni VARCHAR(8) UNIQUE,
    direccion VARCHAR(255),
    telefono VARCHAR(9),
    foto_perfil_url VARCHAR(255),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rol_id) REFERENCES roles(rol_id)
);

DROP TABLE IF EXISTS tipos_cancha; 
CREATE TABLE tipos_cancha (
    tipo_cancha_id INT AUTO_INCREMENT PRIMARY KEY,
    tipo_cancha VARCHAR (50) NOT NULL UNIQUE
);

DROP TABLE IF EXISTS canchas;
CREATE TABLE canchas (
    cancha_id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255),
    tipo_cancha_id NOT NULL,
    ubicacion VARCHAR(255),
    estado_cancha ENUM ('operativo', 'mantenimiento', 'clausurado'),
    numero_soporte VARCHAR(9),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tipo_cancha_id) REFERENCES tipos_cancha(tipo_cancha_id)
);

-- aun falta la parte de servicios, no está completamente implementada
-- No sé si la parte de servicios deberia ir incluida en la parte de servicios.
DROP TABLE IF EXISTS servicios_deportivos;
CREATE TABLE servicios_deportivos (
    servicio_id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255),
    descripcion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS mantenimientos;
CREATE TABLE mantenimientos (
    mantenimiento_id INT AUTO_INCREMENT PRIMARY KEY,
    cancha_id INT NOT NULL,
    motivo TEXT NOT NULL,
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    estado ENUM('pendiente', 'en_curso', 'finalizado'),
    FOREIGN KEY (cancha_id) REFERENCES canchas(cancha_id) 
);

DROP TABLE IF EXISTS reservas;
CREATE TABLE reservas (
    reserva_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    cancha_id INT,
    inicio_reserva TIMESTAMP NOT NULL,
    fin_reserva TIMESTAMP NOT NULL,
    estado ENUM('pendiente', 'confirmada', 'cancelada') DEFAULT 'pendiente',
    estado_pago ENUM('pendiente', 'pagado', 'fallido') DEFAULT 'pendiente',
    imagen_pago_url VARCHAR(255),
    razon_cancelacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id),
    FOREIGN KEY (cancha_id) REFERENCES canchas(cancha_id)
);

DROP TABLE IF EXISTS notificaciones;
CREATE TABLE notificaciones (
    notificacion_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    mensaje TEXT,
    estado ENUM('no_leido', 'leido') DEFAULT 'no_leido',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)
);

DROP TABLE IF EXISTS asistencias;
CREATE TABLE asistencias (
    asistencia_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    entrada DATETIME,
    salida DATETIME,
    geolocalizacion VARCHAR(255),
    observacion TEXT,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)
);

DROP TABLE IF EXISTS pagos;
CREATE TABLE pagos (
    pago_id INT AUTO_INCREMENT PRIMARY KEY,
    reserva_id INT,  
    metodo_pago_id INT NOT NULL, 
    monto DECIMAL(10, 2) NOT NULL,  
    estado_transaccion ENUM('pendiente', 'completado', 'fallido') DEFAULT 'pendiente',   
    transaccion_id VARCHAR(255) UNIQUE,  -- Campos para pagos con Izipay
    foto_comprobante_url VARCHAR (255), 
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  
    detalles_transaccion TEXT,  
    FOREIGN KEY (reserva_id) REFERENCES reservas(reserva_id),
    FOREIGN KEY (metodo_pago_id) REFERENCES metodos_pago(metodo_pago_id)
);
DROP TABLE IF EXISTS metodos_pago
CREATE TAB (
    metodo_pago_id INT AUTO_INCREMENT PRIMARY KEY,
    metodo_pago VARCHAR (50) NOT NULL,
)
