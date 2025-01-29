CREATE TABLE detalle_pedidos (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY, -- Identificador único del detalle.
    id_pedido INT NOT NULL,                    -- Relación con el pedido al que pertenece.
    id_producto INT NOT NULL,                  -- Relación con el producto.
    cantidad DECIMAL(10, 2) NOT NULL,          -- Cantidad del producto en el pedido.
    precio_unitario DECIMAL(10, 2) NOT NULL,   -- Precio unitario del producto al momento del pedido.
    subtotal DECIMAL(10, 2) NOT NULL,          -- Cantidad * precio_unitario = subtotal.
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),    -- Relación con la tabla pedidos.
    FOREIGN KEY (id_producto) REFERENCES productos(id) -- Relación con la tabla productos.
);