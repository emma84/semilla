CREATE TABLE pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY, -- Identificador único del pedido.
    id_usuario INT NOT NULL,                  -- Usuario que realizó el pedido.
    fecha_pedido DATETIME NOT NULL,           -- Fecha y hora en la que se creó el pedido.
    estado ENUM('pendiente', 'en_proceso', 'enviado', 'cancelado', 'completado') NOT NULL DEFAULT 'pendiente',
                                               -- Estado del pedido: pendiente, completado, etc.
    total DECIMAL(10, 2) NOT NULL,            -- Monto total del pedido.
    id_metodo_pago INT,                       -- Método de pago asociado al pedido.
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id), -- Relación con la tabla usuarios.
    FOREIGN KEY (id_metodo_pago) REFERENCES metodos_pago(id_metodo_pago) -- Relación con métodos de pago.
);