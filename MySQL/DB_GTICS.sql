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

DROP TABLE IF EXISTS servicios_deportivos; 
CREATE TABLE tipos_cancha (
    servicio_deportivo_id INT AUTO_INCREMENT PRIMARY KEY,
    servicio_deportivo VARCHAR (50) NOT NULL UNIQUE,
    descripcion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS espacios_deportivos;
CREATE TABLE espacios_deportivos (
    -- Nombre y tipo de servicio
    espacio_deportivo_id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    servicio_deportivo_id INT NOT NULL,
    -- Para piscinas
    max_personas_por_carril INT DEFAULT NULL,
    carriles_piscina INT DEFAULT NULL,
    longitud_piscina INT DEFAULT NULL,
    profundidad_piscina DECIMAL(1,2) DEFAULT NULL,
    descripcion TEXT,
    -- Para gimnasio
    aforo_gimnasio INT DEFAULT NULL,
    -- Pista de atletismo
    longitud_pista DECIMAL(4,2) DEFAULT NULL,
    carriles_pista INT DEFAULT NULL,
    -- Detalles importantes generales
    ubicacion VARCHAR(255),
    estado_servicio ENUM ('operativo', 'mantenimiento', 'clausurado'),
    numero_soporte VARCHAR(9),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tipo_cancha_id) REFERENCES tipos_cancha(tipo_cancha_id),
    FOREIGN KEY (servicio_deportivo_id) REFERENCES servicios_deportivos(servicio_deportivo_id)
);

DROP TABLE IF EXISTS reservas;
CREATE TABLE reservas (
    reserva_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    espacio_deportivo_id INT,
    inicio_reserva TIMESTAMP NOT NULL,
    fin_reserva TIMESTAMP NOT NULL,
    -- Piscina
    numero_carril INT DEFAULT NULL,
    -- Gimnasio: Solo importa el horario de inicio y fin
    -- Pista de atletismo: No se reserva 
    estado ENUM('pendiente', 'confirmada', 'cancelada') DEFAULT 'pendiente',
    razon_cancelacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id),
    FOREIGN KEY (espacio_deportivo_id) REFERENCES espacios_deportivos(espacio_deportivo_id)
);

DROP TABLE IF EXISTS mantenimientos;
CREATE TABLE mantenimientos (
    mantenimiento_id INT AUTO_INCREMENT PRIMARY KEY,
    espacio_deportivo_id INT NOT NULL,
    motivo TEXT NOT NULL,
    fecha_inicio TIMESTAMP,
    fecha_estimada_fin TIMESTAMP,
    estado ENUM('pendiente', 'en_curso', 'finalizado'),
    FOREIGN KEY (espacio_deportivo_id) REFERENCES espacios_deportivos(espacio_deportivo_id)
);

DROP TABLE IF EXISTS asistencias;
CREATE TABLE asistencias (
    asistencia_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    entrada TIMESTAMP,
    salida TIMESTAMP,
    geolocalizacion VARCHAR(100),
    observacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)
);

DROP TABLE IF EXISTS metodos_pago
CREATE TAB (
    metodo_pago_id INT AUTO_INCREMENT PRIMARY KEY,
    metodo_pago VARCHAR (50) NOT NULL,
)


DROP TABLE IF EXISTS pagos;
CREATE TABLE pagos (
    pago_id INT AUTO_INCREMENT PRIMARY KEY,
    reserva_id INT NOT NULL,
    metodo_pago_id INT NOT NULL, 
    monto DECIMAL(4, 2) NOT NULL,
    estado_pago ENUM('pendiente', 'completado', 'fallido') DEFAULT 'pendiente', --Estado de pago revisado por admin
    estado_transaccion ENUM('completado', 'fallido') DEFAULT 'pendiente', -- Estado de transacción para Izipay  
    transaccion_id VARCHAR(255) UNIQUE,  -- Campos para pagos con Izipay
    foto_comprobante_url VARCHAR (255), 
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  
    detalles_transaccion TEXT,  
    FOREIGN KEY (reserva_id) REFERENCES reservas(reserva_id),
    FOREIGN KEY (metodo_pago_id) REFERENCES metodos_pago(metodo_pago_id)
);

DROP TABLE IF EXISTS reembolsos;
CREATE TABLE reembolsos (
    reembolso_id INT AUTO_INCREMENT PRIMARY KEY,
    reserva_id INT NOT NULL,
    pago_id INT NOT NULL,  
    monto DECIMAL(4, 2) NOT NULL,
    estado ENUM('pendiente', 'completado', 'rechazado', 'cancelado') DEFAULT 'pendiente',  
    motivo TEXT,  -- Motivo del reembolso
    fecha_reembolso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    foto_comprobacion_reembolso_url VARCHAR(255), 
    detalles_transaccion TEXT,
    FOREIGN KEY (reserva_id) REFERENCES reservas(reserva_id),
    FOREIGN KEY (pago_id) REFERENCES pagos(pago_id)
);

DROP TABLE IF EXISTS tipos_notificaciones;
CREATE TABLE tipos_notificaciones (
    tipo_notificacion_id INT AUTO_INCREMENT PRIMARY KEY,
    tipo_notificacion VARCHAR (50),
)

DROP TABLE IF EXISTS notificaciones;
CREATE TABLE notificaciones (
    notificacion_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    tipo_notificacion_id INT NOT NULL,
    titulo_notificacion VARCHAR(50),
    mensaje TEXT,
    url_redireccion VARCHAR(255) DEFAULT NULL,
    estado ENUM('no_leido', 'leido') DEFAULT 'no_leido',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)
);

DROP TABLE IF EXISTS avisos 
CREATE TABLE avisos (
    aviso_id INT AUTO_INCREMENT PRIMARY KEY,
    titulo_aviso VARCHAR(50) NOT NULL,
    texto_aviso TEXT NOT NULL,
    foto_aviso_url VARCHAR(255) NOT NULL,
    fecha_aviso TIMESTAMP DEFAULT CURRENT TIMESTAMP ON UPDATE CURRENT TIMESTAMP,
)

DROP TABLE IF EXISTS actividad_usuarios
CREATE TABLE actividad_usuarios (
    actividad_id INT AUTO_INCREMENT PRIMARY KEY,      
    usuario_id INT,                                  
    accion VARCHAR(50),                             
    detalles TEXT,                                    
    recurso_id INT DEFAULT NULL,                      
    fecha_actividad TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
    direccion_ip VARCHAR(50) DEFAULT NULL,            
    estado ENUM('exitoso', 'fallido', 'pendiente') DEFAULT 'exitoso', 
    dispositivo VARCHAR(100) DEFAULT NULL,            
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)  -- Relación con la tabla de usuarios
);
