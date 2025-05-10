UPDATE pagos
SET metodo_pago_id = 1
WHERE transaccion_id = 'TXN002';

select * from observaciones;

select * from pagos;

-- Nota: Se especifican valores manuales para observacion_id porque el esquema original no tiene AUTO_INCREMENT
INSERT INTO observaciones (
    observacion_id, fecha_creacion, fecha_actualizacion, descripcion, foto_url, nivel_urgencia, espacio_deportivo_id, coordinador_id, comentario_administrador, estado
) VALUES
(4, NOW(), NOW(), 'Fuga en el vestuario de la piscina.', 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=800&q=80', 'alto', 1, 4, 'Se programará reparación urgente.', 'resuelto'),
(5, NOW(), NOW(), 'Césped desgastado en la cancha de fútbol.', 'https://images.unsplash.com/photo-1517649763962-97ca4d37b74a?auto=format&fit=crop&w=800&q=80', 'medio', 4, 5, 'Se evaluará reemplazo del césped.', 'en_proceso'),
(6, NOW(), NOW(), 'Máquina rota en el gimnasio.', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80', 'alto', 3, 4, 'Reparación en curso.', 'en_proceso');