select * from observaciones;

select * from usuarios where rol_id = 3;

SELECT o.* FROM observaciones o 
            JOIN espacios_deportivos e ON o.espacio_deportivo_id = e.espacio_deportivo_id 
            JOIN establecimientos_deportivos ed ON e.establecimiento_deportivo_id = ed.establecimiento_deportivo_id 
            JOIN usuarios c ON o.coordinador_id = c.usuario_id 
            WHERE o.nivel_urgencia = "alto";
            
SELECT o.* FROM observaciones o 
            JOIN espacios_deportivos e ON o.espacio_deportivo_id = e.espacio_deportivo_id 
            JOIN establecimientos_deportivos ed ON e.establecimiento_deportivo_id = ed.establecimiento_deportivo_id 
            JOIN usuarios c ON o.coordinador_id = c.usuario_id 
            WHERE o.nivel_urgencia = "alto";
            
INSERT INTO `db_gtics`.`observaciones` (`observacion_id`, `fecha_creacion`, `fecha_actualizacion`, `descripcion`, `foto_url`, `nivel_urgencia`, `espacio_deportivo_id`, `coordinador_id`, `comentario_administrador`)
VALUES
(1, '2025-04-01 08:30:00', '2025-04-02 10:00:00', 'Fugas de agua en la piscina olímpica', 'http://example.com/foto1.jpg', 'alto', 1, 21, 'Requiere reparación urgente'),
(2, '2025-04-03 14:00:00', '2025-04-03 14:00:00', 'Máquinas en mal estado en el gimnasio', 'http://example.com/foto2.jpg', 'medio', 1, 22, 'Puedo esperar a la próxima revisión'),
(3, '2025-04-05 09:00:00', '2025-04-05 09:00:00', 'Luz apagada en la cancha de tenis', 'http://example.com/foto3.jpg', 'bajo', 1, 23, 'Poco urgente, solo afecta al espacio por las noches');

select * from espacios_deportivos;