
INSERT INTO metodos_pago (metodo_pago) VALUES 
('Izipay'),
('Transaccion'),
('Yape');

# crea datos de reservas asociados a los usuarios
INSERT INTO reservas (usuario_id, espacio_deportivo_id, inicio_reserva, fin_reserva, estado)
VALUES
(11, 1, '2025-04-21 08:00:00', '2025-04-21 09:00:00', 'pendiente'),
(12, 1, '2025-04-21 09:00:00', '2025-04-21 10:00:00', 'pendiente'),
(13, 1, '2025-04-21 10:00:00', '2025-04-21 11:00:00', 'pendiente'),
(14, 1, '2025-04-21 11:00:00', '2025-04-21 12:00:00', 'pendiente'),
(15, 1, '2025-04-21 12:00:00', '2025-04-21 13:00:00', 'pendiente'),
(16, 1, '2025-04-21 13:00:00', '2025-04-21 14:00:00', 'pendiente'),
(17, 1, '2025-04-21 14:00:00', '2025-04-21 15:00:00', 'pendiente'),
(18, 1, '2025-04-21 15:00:00', '2025-04-21 16:00:00', 'pendiente'),
(19, 1, '2025-04-21 16:00:00', '2025-04-21 17:00:00', 'pendiente'),
(20, 1, '2025-04-21 17:00:00', '2025-04-21 18:00:00', 'pendiente');

# Creacion de pagos 

INSERT INTO pagos (reserva_id, metodo_pago_id, monto, estado_transaccion, transaccion_id, foto_comprobante_url, detalles_transaccion)
VALUES
(1, 1, 20.00, 'pendiente', 'TXN001', 'https://via.placeholder.com/150?text=Comprobante1', 'Pago exitoso vía Izipay'),
(2, 2, 20.00, 'pendiente', 'TXN002', 'https://via.placeholder.com/150?text=Comprobante2', 'Pago exitoso vía Transaccion'),
(3, 3, 20.00, 'pendiente', 'TXN003', 'https://via.placeholder.com/150?text=Comprobante3', 'Pago exitoso vía Yape'),
(4, 2, 20.00, 'pendiente', 'TXN004', 'https://via.placeholder.com/150?text=Comprobante4', 'Pago exitoso vía Izipay'),
(5, 2, 20.00, 'pendiente', 'TXN005', 'https://via.placeholder.com/150?text=Comprobante5', 'Pago exitoso vía Transaccion'),
(6, 2, 20.00, 'pendiente', 'TXN006', 'https://via.placeholder.com/150?text=Comprobante6', 'Pago exitoso vía Yape'),
(7, 1, 20.00, 'pendiente', 'TXN007', 'https://via.placeholder.com/150?text=Comprobante7', 'Pago exitoso vía Izipay'),
(8, 2, 20.00, 'pendiente', 'TXN008', 'https://via.placeholder.com/150?text=Comprobante8', 'Pago exitoso vía Transaccion'),
(9, 3, 20.00, 'pendiente', 'TXN009', 'https://via.placeholder.com/150?text=Comprobante9', 'Pago exitoso vía Yape'),
(10, 2, 20.00, 'pendiente', 'TXN010', 'https://via.placeholder.com/150?text=Comprobante10', 'Pago exitoso vía Izipay');


# -----------------------------
# Verificaciones
# -----------------------------

select * from espacios_deportivos;

select * from reservas;

select * from pagos;

select * from metodos_pago;


#Verificamos que todos los datos que cumplen con la
# condicion del query method aparecen

SELECT p.*
FROM pagos p
JOIN metodos_pago mp ON p.metodo_pago_id = mp.metodo_pago_id
WHERE p.estado_transaccion = 'pendiente'
  AND mp.metodo_pago = 'Transaccion';

# Verificamos que los datos que se muestran son correctos,
# detalles del pago
SELECT 
    p.pago_id,
    p.monto,
    p.fecha_pago,
    p.estado_transaccion,
    p.foto_comprobante_url,

    u.nombres AS usuario_nombres,
    u.apellidos AS usuario_apellidos,
    u.correo_electronico AS usuario_correo,

    sd.servicio_deportivo AS servicio_deportivo,
    ed.nombre AS espacio_deportivo,
    
    est.establecimiento_deportivo AS establecimiento_deportivo,
    est.direccion AS direccion_establecimiento,

    r.inicio_reserva AS fecha_reserva

FROM pagos p
JOIN reservas r ON p.reserva_id = r.reserva_id
JOIN usuarios u ON r.usuario_id = u.usuario_id
JOIN espacios_deportivos ed ON r.espacio_deportivo_id = ed.espacio_deportivo_id
JOIN servicios_deportivos sd ON ed.servicio_deportivo_id = sd.servicio_deportivo_id
JOIN establecimientos_deportivos est ON ed.establecimiento_deportivo_id = est.establecimiento_deportivo_id

ORDER BY p.fecha_pago DESC;

