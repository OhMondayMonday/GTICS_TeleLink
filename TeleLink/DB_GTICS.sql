-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema db_gtics
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `db_gtics` ;

-- -----------------------------------------------------
-- Schema db_gtics
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `db_gtics` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `db_gtics` ;

-- -----------------------------------------------------
-- Table `db_gtics`.`roles`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`roles` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`roles` (
  `rol_id` INT NOT NULL AUTO_INCREMENT,
  `rol` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`rol_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`usuarios`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`usuarios` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`usuarios` (
  `usuario_id` INT NOT NULL AUTO_INCREMENT,
  `nombres` VARCHAR(100) NULL DEFAULT NULL,
  `apellidos` VARCHAR(100) NULL DEFAULT NULL,
  `correo_electronico` VARCHAR(100) NOT NULL,
  `contrasenia_hash` VARCHAR(255) NOT NULL,
  `rol_id` INT NOT NULL,
  `dni` VARCHAR(8) NULL DEFAULT NULL,
  `direccion` VARCHAR(255) NULL DEFAULT NULL,
  `telefono` VARCHAR(9) NULL DEFAULT NULL,
  `foto_perfil_url` VARCHAR(255) NULL DEFAULT NULL,
  `estado_cuenta` ENUM('activo', 'eliminado', 'baneado', 'pendiente') NULL DEFAULT 'pendiente',
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`usuario_id`),
  UNIQUE INDEX `correo_electronico` (`correo_electronico` ASC) VISIBLE,
  UNIQUE INDEX `dni` (`dni` ASC) VISIBLE,
  INDEX `rol_id` (`rol_id` ASC) VISIBLE,
  INDEX `idx_autenticacion_rapida` (`correo_electronico` ASC, `contrasenia_hash` ASC, `estado_cuenta` ASC) VISIBLE,
  CONSTRAINT `usuarios_ibfk_1`
    FOREIGN KEY (`rol_id`)
    REFERENCES `db_gtics`.`roles` (`rol_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`tipos_actividades`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`tipos_actividades` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`tipos_actividades` (
  `tipo_actividad_id` INT NOT NULL AUTO_INCREMENT,
  `tipo_actividad` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`tipo_actividad_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`actividad_usuarios`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`actividad_usuarios` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`actividad_usuarios` (
  `actividad_id` INT NOT NULL AUTO_INCREMENT,
  `tipo_actividad_id` INT NOT NULL,
  `usuario_id` INT NOT NULL,
  `accion` VARCHAR(50) NULL DEFAULT NULL,
  `detalles` TEXT NULL DEFAULT NULL,
  `fecha_actividad` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `direccion_ip` VARCHAR(50) NULL DEFAULT NULL,
  `ubicacion` VARCHAR(255) NULL DEFAULT NULL,
  `dispositivo` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`actividad_id`),
  INDEX `tipo_actividad_id` (`tipo_actividad_id` ASC) VISIBLE,
  INDEX `idx_usuario_ip` (`usuario_id` ASC, `direccion_ip` ASC) VISIBLE,
  CONSTRAINT `actividad_usuarios_ibfk_1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`),
  CONSTRAINT `actividad_usuarios_ibfk_2`
    FOREIGN KEY (`tipo_actividad_id`)
    REFERENCES `db_gtics`.`tipos_actividades` (`tipo_actividad_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`servicios_deportivos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`servicios_deportivos` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`servicios_deportivos` (
  `servicio_deportivo_id` INT NOT NULL AUTO_INCREMENT,
  `servicio_deportivo` VARCHAR(50) NOT NULL,
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`servicio_deportivo_id`),
  UNIQUE INDEX `servicio_deportivo` (`servicio_deportivo` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`establecimientos_deportivos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`establecimientos_deportivos` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`establecimientos_deportivos` (
  `establecimiento_deportivo_id` INT NOT NULL AUTO_INCREMENT,
  `establecimiento_deportivo` VARCHAR(100) NOT NULL,
  `descripcion` TEXT NULL DEFAULT NULL,
  `direccion` VARCHAR(255) NOT NULL,
  `espacios_estacionamiento` INT NULL DEFAULT NULL,
  `telefono_contacto` VARCHAR(20) NULL DEFAULT NULL,
  `correo_contacto` VARCHAR(100) NULL DEFAULT NULL,
  `geolocalizacion` VARCHAR(255) NOT NULL,
  `foto_establecimiento_url` VARCHAR(255) NULL DEFAULT NULL,
  `horario_apertura` TIME NOT NULL,
  `horario_cierre` TIME NOT NULL,
  `estado` ENUM('activo', 'clausurado', 'mantenimiento') NULL DEFAULT 'activo',
  `motivo_mantenimiento` TEXT NULL DEFAULT NULL,
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`establecimiento_deportivo_id`),
  UNIQUE INDEX `establecimiento_deportivo` (`establecimiento_deportivo` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`espacios_deportivos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`espacios_deportivos` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`espacios_deportivos` (
  `espacio_deportivo_id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(255) NOT NULL,
  `servicio_deportivo_id` INT NOT NULL,
  `establecimiento_deportivo_id` INT NOT NULL,
  `max_personas_por_carril` INT NULL DEFAULT NULL,
  `carriles_piscina` INT NULL DEFAULT NULL,
  `longitud_piscina` INT NULL DEFAULT NULL,
  `profundidad_piscina` DECIMAL(6,2) NULL DEFAULT NULL,
  `descripcion` TEXT NULL DEFAULT NULL,
  `foto_espacio_deportivo_url` VARCHAR(255) NULL DEFAULT NULL,
  `aforo_gimnasio` INT NULL DEFAULT NULL,
  `longitud_pista` DECIMAL(6,2) NULL DEFAULT NULL,
  `carriles_pista` INT NULL DEFAULT NULL,
  `geolocalizacion` VARCHAR(255) NULL DEFAULT NULL,
  `estado_servicio` ENUM('operativo', 'mantenimiento', 'clausurado') NULL DEFAULT NULL,
  `numero_soporte` VARCHAR(9) NULL DEFAULT NULL,
  `horario_apertura` TIME NULL DEFAULT NULL,
  `horario_cierre` TIME NULL DEFAULT NULL,
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `precio_por_hora` DECIMAL(6,2) NULL,
  PRIMARY KEY (`espacio_deportivo_id`),
  INDEX `idx_servicio_estado_horario` (`servicio_deportivo_id` ASC, `estado_servicio` ASC, `horario_apertura` ASC, `horario_cierre` ASC) VISIBLE,
  INDEX `idx_piscina_info` (`establecimiento_deportivo_id` ASC, `carriles_piscina` ASC, `max_personas_por_carril` ASC, `estado_servicio` ASC) VISIBLE,
  INDEX `idx_gimnasio_info` (`establecimiento_deportivo_id` ASC, `aforo_gimnasio` ASC, `estado_servicio` ASC) VISIBLE,
  INDEX `idx_pista_info` (`establecimiento_deportivo_id` ASC, `carriles_pista` ASC, `estado_servicio` ASC) VISIBLE,
  CONSTRAINT `espacios_deportivos_ibfk_1`
    FOREIGN KEY (`servicio_deportivo_id`)
    REFERENCES `db_gtics`.`servicios_deportivos` (`servicio_deportivo_id`),
  CONSTRAINT `espacios_deportivos_ibfk_2`
    FOREIGN KEY (`establecimiento_deportivo_id`)
    REFERENCES `db_gtics`.`establecimientos_deportivos` (`establecimiento_deportivo_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`asistencias`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`asistencias` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`asistencias` (
  `asistencia_id` INT NOT NULL AUTO_INCREMENT,
  `administrador_id` INT NOT NULL,
  `coordinador_id` INT NOT NULL,
  `horario_entrada` TIMESTAMP NULL DEFAULT NULL,
  `horario_salida` TIMESTAMP NULL DEFAULT NULL,
  `registro_entrada` TIMESTAMP NULL DEFAULT NULL,
  `registro_salida` TIMESTAMP NULL DEFAULT NULL,
  `estado_entrada` ENUM('puntual', 'tarde', 'pendiente', 'inasistencia', 'cancelada') NULL DEFAULT 'pendiente',
  `estado_salida` ENUM('realizado', 'pendiente', 'inasistencia') NULL DEFAULT NULL,
  `geolocalizacion` VARCHAR(100) NULL DEFAULT NULL,
  `observacion_asistencia` TEXT NULL DEFAULT NULL,
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `espacio_deportivo_id` INT NOT NULL,
  PRIMARY KEY (`asistencia_id`),
  INDEX `administrador_id` (`administrador_id` ASC) VISIBLE,
  INDEX `idx_coordinador_horario` (`coordinador_id` ASC, `horario_entrada` ASC, `horario_salida` ASC) VISIBLE,
  INDEX `fk_asistencias_espacios_deportivos1_idx` (`espacio_deportivo_id` ASC) VISIBLE,
  CONSTRAINT `asistencias_ibfk_1`
    FOREIGN KEY (`coordinador_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`),
  CONSTRAINT `asistencias_ibfk_2`
    FOREIGN KEY (`administrador_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`),
  CONSTRAINT `fk_asistencias_espacios_deportivos1`
    FOREIGN KEY (`espacio_deportivo_id`)
    REFERENCES `db_gtics`.`espacios_deportivos` (`espacio_deportivo_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`avisos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`avisos` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`avisos` (
  `aviso_id` INT NOT NULL AUTO_INCREMENT,
  `titulo_aviso` VARCHAR(50) NOT NULL,
  `texto_aviso` TEXT NOT NULL,
  `foto_aviso_url` VARCHAR(255) NOT NULL,
  `fecha_aviso` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `estado_aviso` enum('activo', 'disponible', 'eliminado', 'default') NULL DEFAULT 'disponible',
  PRIMARY KEY (`aviso_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`conversaciones`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`conversaciones` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`conversaciones` (
  `conversacion_id` INT NOT NULL AUTO_INCREMENT,
  `usuario_id` INT NULL DEFAULT NULL,
  `inicio_conversacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `fin_conversacion` TIMESTAMP NULL DEFAULT NULL,
  `estado` ENUM('en_proceso', 'finalizada') NULL DEFAULT 'en_proceso',
  PRIMARY KEY (`conversacion_id`),
  INDEX `usuario_id` (`usuario_id` ASC) VISIBLE,
  CONSTRAINT `conversaciones_ibfk_1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`mantenimientos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`mantenimientos` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`mantenimientos` (
  `mantenimiento_id` INT NOT NULL AUTO_INCREMENT,
  `espacio_deportivo_id` INT NOT NULL,
  `motivo` TEXT NOT NULL,
  `fecha_inicio` TIMESTAMP NULL DEFAULT NULL,
  `fecha_estimada_fin` TIMESTAMP NULL DEFAULT NULL,
  `estado` ENUM('pendiente', 'en_curso', 'finalizado') NULL DEFAULT NULL,
  `descripcion` TEXT,
  `fecha_creacion` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`mantenimiento_id`),
  INDEX `espacio_deportivo_id` (`espacio_deportivo_id` ASC) VISIBLE,
  CONSTRAINT `mantenimientos_ibfk_1`
    FOREIGN KEY (`espacio_deportivo_id`)
    REFERENCES `db_gtics`.`espacios_deportivos` (`espacio_deportivo_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `db_gtics`.`mensajes`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`mensajes` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`mensajes` (
  `mensaje_id` INT NOT NULL AUTO_INCREMENT,
  `conversacion_id` INT NULL DEFAULT NULL,
  `texto_mensaje` TEXT NOT NULL,
  `origen` ENUM('usuario', 'chatbot') NULL DEFAULT 'usuario',
  `fecha` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`mensaje_id`),
  INDEX `conversacion_id` (`conversacion_id` ASC) VISIBLE,
  CONSTRAINT `mensajes_ibfk_1`
    FOREIGN KEY (`conversacion_id`)
    REFERENCES `db_gtics`.`conversaciones` (`conversacion_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`metodos_pago`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`metodos_pago` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`metodos_pago` (
  `metodo_pago_id` INT NOT NULL AUTO_INCREMENT,
  `metodo_pago` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`metodo_pago_id`),
  UNIQUE INDEX `metodo_pago` (`metodo_pago` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`tipos_notificaciones`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`tipos_notificaciones` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`tipos_notificaciones` (
  `tipo_notificacion_id` INT NOT NULL AUTO_INCREMENT,
  `tipo_notificacion` VARCHAR(50) NULL DEFAULT NULL,
  PRIMARY KEY (`tipo_notificacion_id`),
  UNIQUE INDEX `tipo_notificacion` (`tipo_notificacion` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`notificaciones`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`notificaciones` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`notificaciones` (
  `notificacion_id` INT NOT NULL AUTO_INCREMENT,
  `usuario_id` INT NOT NULL,
  `titulo_notificacion` VARCHAR(50) NULL DEFAULT NULL,
  `mensaje` TEXT NULL DEFAULT NULL,
  `url_redireccion` VARCHAR(255) NULL DEFAULT NULL,
  `estado` ENUM('no_leido', 'leido') NULL DEFAULT 'no_leido',
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `tipo_notificacion_id` INT NOT NULL,
  PRIMARY KEY (`notificacion_id`),
  INDEX `usuario_id` (`usuario_id` ASC) VISIBLE,
  INDEX `fk_notificaciones_tipos_notificaciones1_idx` (`tipo_notificacion_id` ASC) VISIBLE,
  CONSTRAINT `notificaciones_ibfk_1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`),
  CONSTRAINT `fk_notificaciones_tipos_notificaciones1`
    FOREIGN KEY (`tipo_notificacion_id`)
    REFERENCES `db_gtics`.`tipos_notificaciones` (`tipo_notificacion_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`reservas`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`reservas` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`reservas` (
  `reserva_id` INT NOT NULL AUTO_INCREMENT,
  `usuario_id` INT NOT NULL,
  `espacio_deportivo_id` INT NULL,
  `inicio_reserva` TIMESTAMP NOT NULL,
  `fin_reserva` TIMESTAMP NOT NULL,
  `numero_carril_piscina` INT NULL DEFAULT NULL,
  `numero_carril_pista` INT NULL DEFAULT NULL,
  `numero_participantes_piscina` INT NULL DEFAULT 1,
  `estado` ENUM('pendiente', 'confirmada', 'cancelada','completada', 'en_proceso') NULL DEFAULT 'pendiente',
  `razon_cancelacion` TEXT NULL DEFAULT NULL,
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`reserva_id`),
  INDEX `usuario_id` (`usuario_id` ASC) VISIBLE,
  INDEX `idx_reserva_piscina` (`espacio_deportivo_id` ASC, `inicio_reserva` ASC, `fin_reserva` ASC, `numero_carril_piscina` ASC) VISIBLE,
  INDEX `idx_reserva_participantes` (`espacio_deportivo_id` ASC, `numero_carril_piscina` ASC, `inicio_reserva` ASC, `fin_reserva` ASC, `numero_participantes_piscina` ASC) VISIBLE,
  INDEX `idx_reserva_pista` (`espacio_deportivo_id` ASC, `inicio_reserva` ASC, `fin_reserva` ASC, `numero_carril_pista` ASC) VISIBLE,
  INDEX `idx_reserva_gimnasio_cancha` (`espacio_deportivo_id` ASC, `inicio_reserva` ASC, `fin_reserva` ASC) VISIBLE,
  CONSTRAINT `reservas_ibfk_1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`),
  CONSTRAINT `reservas_ibfk_2`
    FOREIGN KEY (`espacio_deportivo_id`)
    REFERENCES `db_gtics`.`espacios_deportivos` (`espacio_deportivo_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE reservas 
ADD COLUMN numero_participantes_piscina INT DEFAULT 1 NOT NULL;

select * from asistencias
-- -----------------------------------------------------
-- Table `db_gtics`.`pagos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`pagos` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`pagos` (
  `pago_id` INT NOT NULL AUTO_INCREMENT,
  `reserva_id` INT NOT NULL,
  `metodo_pago_id` INT NOT NULL,
  `monto` DECIMAL(6,2) NOT NULL,
  `estado_transaccion` ENUM('completado', 'fallido', 'pendiente') NULL DEFAULT 'pendiente',
  `transaccion_id` VARCHAR(255) NULL DEFAULT NULL,
  `foto_comprobante_url` VARCHAR(255) NULL DEFAULT NULL,
  `fecha_pago` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `detalles_transaccion` TEXT NULL DEFAULT NULL,
  `motivo_rechazo` TEXT NULL,
  PRIMARY KEY (`pago_id`),
  UNIQUE INDEX `transaccion_id` (`transaccion_id` ASC) VISIBLE,
  INDEX `reserva_id` (`reserva_id` ASC) VISIBLE,
  INDEX `metodo_pago_id` (`metodo_pago_id` ASC) VISIBLE,
  CONSTRAINT `pagos_ibfk_1`
    FOREIGN KEY (`reserva_id`)
    REFERENCES `db_gtics`.`reservas` (`reserva_id`),
  CONSTRAINT `pagos_ibfk_2`
    FOREIGN KEY (`metodo_pago_id`)
    REFERENCES `db_gtics`.`metodos_pago` (`metodo_pago_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`reembolsos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`reembolsos` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`reembolsos` (
  `reembolso_id` INT NOT NULL AUTO_INCREMENT,
  `monto` DECIMAL(6,2) NOT NULL,
  `estado` ENUM('pendiente', 'completado', 'rechazado', 'cancelado') NULL DEFAULT 'pendiente',
  `motivo` TEXT NULL DEFAULT NULL,
  `fecha_reembolso` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `foto_comprobacion_reembolso_url` VARCHAR(255) NULL DEFAULT NULL,
  `detalles_transaccion` TEXT NULL DEFAULT NULL,
  `pago_id` INT NOT NULL,
  PRIMARY KEY (`reembolso_id`),
  INDEX `fk_reembolsos_pagos1_idx` (`pago_id` ASC) VISIBLE,
  CONSTRAINT `fk_reembolsos_pagos1`
    FOREIGN KEY (`pago_id`)
    REFERENCES `db_gtics`.`pagos` (`pago_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `db_gtics`.`resenias`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`resenias` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`resenias` (
  `resenia_id` INT NOT NULL AUTO_INCREMENT,
  `usuario_id` INT NOT NULL,
  `calificacion` INT NOT NULL,
  `comentario` VARCHAR(255) NULL DEFAULT NULL,
  `foto_resenia_url` VARCHAR(255) NULL DEFAULT NULL,
  `fecha_creacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_actualizacion` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `espacio_deportivo_id` INT NOT NULL,
  PRIMARY KEY (`resenia_id`),
  INDEX `usuario_id` (`usuario_id` ASC) VISIBLE,
  INDEX `fk_resenias_espacios_deportivos1_idx` (`espacio_deportivo_id` ASC) VISIBLE,
  CONSTRAINT `resenias_ibfk_1`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`),
  CONSTRAINT `fk_resenias_espacios_deportivos1`
    FOREIGN KEY (`espacio_deportivo_id`)
    REFERENCES `db_gtics`.`espacios_deportivos` (`espacio_deportivo_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;



-- -----------------------------------------------------
-- Table `db_gtics`.`verification_tokens`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`verification_tokens`;

CREATE TABLE `db_gtics`.`verification_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(255) NOT NULL,
  `usuario_id` INT NOT NULL,
  `expiry_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `token` (`token` ASC) VISIBLE,
  CONSTRAINT `fk_verification_tokens_usuario`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;



-- -----------------------------------------------------
-- Table `db_gtics`.`password_reset_tokens`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`password_reset_tokens` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`password_reset_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(255) NOT NULL,
  `usuario_id` INT NOT NULL,
  `expiry_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `token` (`token` ASC) VISIBLE,
  CONSTRAINT `fk_password_reset_tokens_usuario`
    FOREIGN KEY (`usuario_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `db_gtics`.`observaciones`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `db_gtics`.`observaciones` ;

CREATE TABLE IF NOT EXISTS `db_gtics`.`observaciones` (
  `observacion_id` INT NOT NULL AUTO_INCREMENT,
  `fecha_creacion` TIMESTAMP NULL,
  `fecha_actualizacion` TIMESTAMP NULL,
  `descripcion` TEXT NULL,
  `foto_url` VARCHAR(255) NULL,
  `nivel_urgencia` ENUM('alto', 'medio', 'bajo') NULL,
  `espacio_deportivo_id` INT NOT NULL,
  `coordinador_id` INT NOT NULL,
  `comentario_administrador` TEXT NULL,
  `estado` ENUM('resuelto', 'en_proceso', 'pendiente') NULL DEFAULT 'pendiente',
  PRIMARY KEY (`observacion_id`),
  INDEX `fk_observaciones_espacios_deportivos1_idx` (`espacio_deportivo_id` ASC) VISIBLE,
  INDEX `fk_observaciones_usuarios1_idx` (`coordinador_id` ASC) VISIBLE,
  CONSTRAINT `fk_observaciones_espacios_deportivos1`
    FOREIGN KEY (`espacio_deportivo_id`)
    REFERENCES `db_gtics`.`espacios_deportivos` (`espacio_deportivo_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_observaciones_usuarios1`
    FOREIGN KEY (`coordinador_id`)
    REFERENCES `db_gtics`.`usuarios` (`usuario_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

USE `db_gtics`;

DELIMITER $$

USE `db_gtics`$$
DROP TRIGGER IF EXISTS `db_gtics`.`actualizar_estado_espacio_deportivo` $$
USE `db_gtics`$$
CREATE
DEFINER=`root`@`localhost`
TRIGGER `db_gtics`.`actualizar_estado_espacio_deportivo`
AFTER UPDATE ON `db_gtics`.`mantenimientos`
FOR EACH ROW
BEGIN
    IF NEW.fecha_estimada_fin <= CURRENT_TIMESTAMP THEN
        UPDATE espacios_deportivos
        SET estado_servicio = 'operativo'
        WHERE espacio_deportivo_id = NEW.espacio_deportivo_id;
    END IF;
END$$


DELIMITER ;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

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
        TIMESTAMPADD(MINUTE, 30, horario_entrada) < CURRENT_TIMESTAMP
        AND estado_entrada = 'pendiente';
END$$

DELIMITER ;

DELIMITER //

CREATE EVENT IF NOT EXISTS `actualizar_reservas_completadas`
ON SCHEDULE EVERY 1 MINUTE  -- Se ejecuta cada minuto (puedes cambiarlo a 1 HOUR si prefieres)
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    -- Actualiza reservas CONFIRMADAS que ya pasaron su hora de fin
    UPDATE `db_gtics`.`reservas`
    SET 
        `estado` = 'completada',
        `fecha_actualizacion` = CURRENT_TIMESTAMP
    WHERE 
        `estado` = 'confirmada' 
        AND `fin_reserva` < CURRENT_TIMESTAMP;  -- Si ya pasó la hora de fin
END//

DELIMITER ;

-- Habilitar el event scheduler
SET GLOBAL event_scheduler = ON;

-- Event scheduler para cancelar reservas pendientes después de 5 minutos
DELIMITER $$

CREATE EVENT IF NOT EXISTS `cancelar_reservas_pendientes_timeout`
ON SCHEDULE EVERY 1 MINUTE
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    -- Cancelar reservas que están en estado 'pendiente' por más de 5 minutos
    UPDATE `db_gtics`.`reservas`
    SET 
        `estado` = 'cancelada',
        `razon_cancelacion` = 'Reserva cancelada automáticamente por timeout de pago (5 minutos)',
        `fecha_actualizacion` = CURRENT_TIMESTAMP
    WHERE 
        `estado` = 'pendiente' 
        AND TIMESTAMPDIFF(MINUTE, `fecha_creacion`, CURRENT_TIMESTAMP) >= 5;
END$$

DELIMITER ;

-- Event scheduler para cancelar reservas en_proceso/pendiente que ya iniciaron
DELIMITER $$

CREATE EVENT IF NOT EXISTS `cancelar_reservas_vencidas`
ON SCHEDULE EVERY 1 MINUTE
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    -- Cancelar reservas en estado 'en_proceso' o 'pendiente' cuya hora de inicio ya pasó
    UPDATE `db_gtics`.`reservas`
    SET 
        `estado` = 'cancelada',
        `razon_cancelacion` = 'Reserva cancelada automáticamente - hora de inicio superada',
        `fecha_actualizacion` = CURRENT_TIMESTAMP
    WHERE 
        `estado` IN ('en_proceso', 'pendiente')
        AND `inicio_reserva` < CURRENT_TIMESTAMP;
END$$

DELIMITER ;

-- Event scheduler para completar reservas confirmadas que ya terminaron
DELIMITER $$

CREATE EVENT IF NOT EXISTS `completar_reservas_terminadas`
ON SCHEDULE EVERY 1 MINUTE
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    -- Completar reservas confirmadas cuya hora de fin ya pasó
    UPDATE `db_gtics`.`reservas`
    SET 
        `estado` = 'completada',
        `fecha_actualizacion` = CURRENT_TIMESTAMP
    WHERE 
        `estado` = 'confirmada'
        AND `fin_reserva` < CURRENT_TIMESTAMP;
END$$

DELIMITER ;


select * from db_gtics.usuarios;

SELECT 
    'Event schedulers configurados correctamente' as mensaje,
    COUNT(*) as reservas_actualizadas
FROM reservas 
WHERE fecha_actualizacion >= NOW() - INTERVAL 1 MINUTE;