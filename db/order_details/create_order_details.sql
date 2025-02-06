CREATE TABLE order_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- identificador único para cada detalle de pedido (clave primaria).
    order_id INT NOT NULL,                  -- Es la clave foránea que hace referencia al pedido (tabla orders).
    product_id INT NOT NULL,                -- clave foránea que hace referencia al producto (tabla products).
    quantity DECIMAL(10, 2) NOT NULL,       -- cantidad del producto en ese detalle de pedido.
    unit_price DECIMAL(10, 2) NOT NULL,     -- precio unitario del producto en el momento del pedido.
    subtotal DECIMAL(10, 2) NOT NULL,       -- total para ese producto, calculado como cantidad * precio_unitario.
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,  -- hace referencia a la tabla orders (con la clave foránea order_id).
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE -- hace referencia a la tabla products (con la clave foránea product_id).
);