DROP DATABASE if EXISTS db_gtics;
CREATE DATABASE db_gtics;
USE db_gtics;

-- Campos para usuarios

DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    rol_id INT AUTO_INCREMENT PRIMARY KEY,
    rol VARCHAR(20) NOT NULL
);

DROP TABLE IF EXISTS usuarios;
CREATE TABLE usuarios (
    usuario_id INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100),
    apellidos VARCHAR(100),
    correo_electronico VARCHAR(100) UNIQUE NOT NULL,
    contrasenia_hash VARCHAR(255) NOT NULL,
    rol_id INT NOT NULL,
    dni VARCHAR(8) UNIQUE,
    direccion VARCHAR(255),
    telefono VARCHAR(9),
    foto_perfil_url VARCHAR(255),
    estado_cuenta ENUM('activo','eliminado','baneado', 'pendiente') default 'pendiente',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (rol_id) REFERENCES roles(rol_id)
);

CREATE INDEX idx_autenticacion_rapida ON usuarios(correo_electronico, contrasenia_hash, estado_cuenta);

-- Establecimientos que albergan los espacios deportivos. Ejemplo: Complejo deportivo San Micky
DROP TABLE IF EXISTS establecimientos_deportivos;
CREATE TABLE establecimientos_deportivos (
    establecimiento_deportivo_id INT AUTO_INCREMENT PRIMARY KEY,
    establecimiento_deportivo VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    direccion VARCHAR(255) NOT NULL,
    espacios_estacionamiento INT,
    telefono_contacto VARCHAR(20),
    correo_contacto VARCHAR(100),
    geolocalizacion VARCHAR(255) NOT NULL,
    foto_establecimiento_url VARCHAR(255),
    horario_apertura TIME NOT NULL,
    horario_cierre TIME NOT NULL,
    estado ENUM('activo', 'clausurado', 'mantenimiento') DEFAULT 'activo',
    motivo_mantenimiento TEXT DEFAULT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Servicios deportivos
DROP TABLE IF EXISTS servicios_deportivos; 
-- Son los servicios que proveen los espacios deportivos:
-- Piscina, Futbol, Pista de atletismo, etc.
CREATE TABLE servicios_deportivos (
    servicio_deportivo_id INT AUTO_INCREMENT PRIMARY KEY,
    servicio_deportivo VARCHAR(50) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS espacios_deportivos;
-- Son los espacios deportivos específicos que permiten usar servicios deportivos
-- Piscina San Miguel, Cancha 1 de F7, Gimnasio SmartPUCP, etc.
CREATE TABLE espacios_deportivos (
    -- Nombre y tipo de servicio
    espacio_deportivo_id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    servicio_deportivo_id INT NOT NULL,
    establecimiento_deportivo_id INT NOT NULL,
    -- Para piscinas
    max_personas_por_carril INT DEFAULT NULL,
    carriles_piscina INT DEFAULT NULL,
    longitud_piscina INT DEFAULT NULL,
    profundidad_piscina DECIMAL(4,2),
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
    horario_apertura TIME, 
    horario_cierre TIME,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (servicio_deportivo_id) REFERENCES servicios_deportivos(servicio_deportivo_id),
    FOREIGN KEY (establecimiento_deportivo_id) REFERENCES establecimientos_deportivos(establecimiento_deportivo_id)
);

-- Para cargar rapidamente la estructura de los horarios al entrar en una reserva para un servicio (piscina, futbol, etc)
CREATE INDEX idx_servicio_estado_horario 
ON espacios_deportivos(servicio_deportivo_id, estado_servicio, horario_apertura, horario_cierre);

-- Para poder agilizar la consulta de limitantes => Carriles llenos, aforo lleno, etc.
CREATE INDEX idx_piscina_info 
ON espacios_deportivos(
    establecimiento_deportivo_id, 
    carriles_piscina, 
    max_personas_por_carril,
    estado_servicio
);

CREATE INDEX idx_gimnasio_info 
ON espacios_deportivos(
    establecimiento_deportivo_id, 
    aforo_gimnasio, 
    estado_servicio
);

CREATE INDEX idx_pista_info 
ON espacios_deportivos(
    establecimiento_deportivo_id, 
    carriles_pista, 
    estado_servicio
);

DROP TABLE IF EXISTS reservas;
CREATE TABLE reservas (
    reserva_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    espacio_deportivo_id INT,
    inicio_reserva TIMESTAMP NOT NULL,
    fin_reserva TIMESTAMP NOT NULL,
    -- Piscina
    numero_carril_piscina INT DEFAULT NULL,
    -- Gimnasio: Solo importa el horario de inicio y fin
    -- Pista de atletismo:
    numero_carril_pista INT DEFAULT NULL,
    estado ENUM('pendiente', 'confirmada', 'cancelada') DEFAULT 'pendiente',
    razon_cancelacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id),
    FOREIGN KEY (espacio_deportivo_id) REFERENCES espacios_deportivos(espacio_deportivo_id)
);

-- Índices para poder saber ágilmente las reservas que bloquean los espacios disponibles para reservas
-- Si no existieran, se demoraría bastante en cargar los horarios disponibles y no disponibles para cada espacio
CREATE INDEX idx_reserva_piscina 
ON reservas(espacio_deportivo_id, inicio_reserva, fin_reserva, numero_carril_piscina);

CREATE INDEX idx_reserva_pista
ON reservas(espacio_deportivo_id, inicio_reserva, fin_reserva, numero_carril_pista);

CREATE INDEX idx_reserva_gimnasio_cancha
ON reservas(espacio_deportivo_id, inicio_reserva, fin_reserva);

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

DELIMITER $$

CREATE TRIGGER actualizar_estado_espacio_deportivo
AFTER UPDATE ON mantenimientos
FOR EACH ROW
BEGIN
    IF NEW.fecha_estimada_fin = CURRENT_TIMESTAMP THEN
        UPDATE espacios_deportivos
        SET estado_servicio = 'operativo'
        WHERE espacio_deportivo_id = NEW.espacio_deportivo_id;
    END IF;
END$$

DELIMITER ;

DROP TABLE IF EXISTS asistencias;
CREATE TABLE asistencias (
    asistencia_id INT AUTO_INCREMENT PRIMARY KEY,
    administrador_id INT NOT NULL,
    coordinador_id INT NOT NULL,
    -- Campos definidos por el administrador
    horario_entrada TIMESTAMP,
    horario_salida TIMESTAMP,
    -- Marcado de asistencia por parte de coordinador
    registro_entrada TIMESTAMP,
    registro_salida TIMESTAMP,
    -- Estado asistencia en la entrada y salida
    estado_entrada ENUM ('puntual', 'tarde', 'pendiente', 'inasistencia') DEFAULT 'pendiente',
    estado_salida ENUM ('realizado', 'pendiente', 'inasistencia'),
    geolocalizacion VARCHAR(100),
    observacion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coordinador_id) REFERENCES usuarios(usuario_id),
    FOREIGN KEY (administrador_id) REFERENCES usuarios(usuario_id)
);

-- Si es que el coordinador quiere marcar asistencia rápidamente, el sistema debe hacer la consulta 
-- rápida del horario de entrada y salida
CREATE INDEX idx_coordinador_horario 
ON asistencias (coordinador_id, horario_entrada, horario_salida);

-- Gestión de Pagos

DROP TABLE IF EXISTS metodos_pago;
CREATE TABLE metodos_pago (
    metodo_pago_id INT AUTO_INCREMENT PRIMARY KEY,
    metodo_pago VARCHAR(50) UNIQUE NOT NULL
);

DROP TABLE IF EXISTS pagos;
CREATE TABLE pagos (
    pago_id INT AUTO_INCREMENT PRIMARY KEY,
    reserva_id INT NOT NULL,
    metodo_pago_id INT NOT NULL, 
    monto DECIMAL(4, 2) NOT NULL,
    estado_transaccion ENUM('completado', 'fallido', 'pendiente') DEFAULT 'pendiente', -- Puede ser determinado por Izipay o Admin
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
    pago_id INT NOT NULL,  
    monto DECIMAL(4, 2) NOT NULL,
    estado ENUM('pendiente', 'completado', 'rechazado', 'cancelado') DEFAULT 'pendiente',  
    motivo TEXT,  -- Motivo del reembolso
    fecha_reembolso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    foto_comprobacion_reembolso_url VARCHAR(255), 
    detalles_transaccion TEXT,
    FOREIGN KEY (pago_id) REFERENCES pagos(pago_id)
);

-- Notificaciones y actividades

DROP TABLE IF EXISTS tipos_notificaciones;
CREATE TABLE tipos_notificaciones (
    tipo_notificacion_id INT AUTO_INCREMENT PRIMARY KEY,
    tipo_notificacion VARCHAR(50) UNIQUE
);

DROP TABLE IF EXISTS notificaciones;
CREATE TABLE notificaciones (
    notificacion_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    tipo_notificacion_id INT NOT NULL,
    titulo_notificacion VARCHAR(50),
    mensaje TEXT,
    url_redireccion VARCHAR(255) DEFAULT NULL,
    estado ENUM('no_leido', 'leido') DEFAULT 'no_leido',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id),
    FOREIGN KEY (tipo_notificacion_id) REFERENCES tipos_notificaciones(tipo_notificacion_id)
);

DROP TABLE IF EXISTS avisos;
CREATE TABLE avisos (
    aviso_id INT AUTO_INCREMENT PRIMARY KEY,
    titulo_aviso VARCHAR(50) NOT NULL,
    texto_aviso TEXT NOT NULL,
    foto_aviso_url VARCHAR(255) NOT NULL,
    fecha_aviso TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
DROP TABLE IF EXISTS tipos_actividades;
CREATE TABLE tipos_actividades (
    tipo_actividad_id INT AUTO_INCREMENT PRIMARY KEY,
    tipo_actividad VARCHAR(100)
);

DROP TABLE IF EXISTS actividad_usuarios;
CREATE TABLE actividad_usuarios (
    actividad_id INT AUTO_INCREMENT PRIMARY KEY,      
    tipo_actividad_id INT NOT NULL,
    usuario_id INT NOT NULL,
    accion VARCHAR(50),                             
    detalles TEXT,                                    
    recurso_id INT DEFAULT NULL,                      
    fecha_actividad TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
    -- (Opcional)
    -- Podríamos obtener la ip de esta actividad como parte
    -- de la conexión durante el login
    direccion_ip VARCHAR(50) DEFAULT NULL,
    -- Utilizando una API, podemos saber la localizacion
    -- con la IP
    ubicacion VARCHAR(255) DEFAULT NULL,
    -- Obtenido como parte de un user-agent
    dispositivo VARCHAR(100) DEFAULT NULL,            
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id),
    FOREIGN KEY (tipo_actividad_id) REFERENCES tipos_actividades(tipo_actividad_id)
);
-- Por ejemplo, si queremos detectar cambios de ip
CREATE INDEX idx_usuario_ip ON actividad_usuarios(usuario_id, direccion_ip);

-- (opcional)
DROP TABLE IF EXISTS resenias;
CREATE TABLE resenias (
	resenia_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    espacio_deportivo_id INT NOT NULL,
    calificacion INT NOT NULL,
    comentario VARCHAR(255),
    foto_resenia_url VARCHAR(255),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)
);

-- Campos para Chatbot
-- No aplicables si es que no se requiere historial
DROP TABLE IF EXISTS conversaciones;
CREATE TABLE conversaciones (
    conversacion_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    inicio_conversacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fin_conversacion TIMESTAMP DEFAULT NULL,
    estado ENUM('en_proceso', 'finalizada') DEFAULT 'en_proceso',
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)
);
DROP TABLE IF EXISTS mensajes;
CREATE TABLE mensajes (
    mensaje_id INT AUTO_INCREMENT PRIMARY KEY,
    conversacion_id INT,
    usuario_id INT,  -- Si el mensaje fue enviado por un usuario
    texto_mensaje TEXT NOT NULL,
    origen ENUM('usuario', 'chatbot') DEFAULT 'usuario',
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversacion_id) REFERENCES conversaciones(conversacion_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(usuario_id)
);

DROP TABLE IF EXISTS observaciones;
CREATE TABLE observaciones (
    observacion_id INT AUTO_INCREMENT PRIMARY KEY,
    espacio_deportivo_id INT NOT NULL,
    coordinador_id INT NOT NULL,
    descripcion TEXT NOT NULL,
    fecha_observacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('pendiente', 'en revisión', 'resuelto') DEFAULT 'pendiente',
    FOREIGN KEY (espacio_deportivo_id) REFERENCES espacios_deportivos(espacio_deportivo_id),
    FOREIGN KEY (coordinador_id) REFERENCES usuarios(usuario_id)
);

-- qué más? 
-- faltan índices para agilizar consultas
