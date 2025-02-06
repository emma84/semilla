CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Identificador único del pedido
    user_id INT NOT NULL,  -- Usuario que realizó el pedido
    order_date DATETIME NOT NULL,  -- Fecha y hora en que se creó el pedido
    status ENUM('pending', 'processing', 'shipped', 'canceled', 'completed') NOT NULL DEFAULT 'pending',  
    -- Estado del pedido: pendiente, en proceso, enviado, cancelado, completado
    total DECIMAL(10, 2) NOT NULL,  -- Monto total del pedido
    payment_method_id INT,  -- Método de pago asociado al pedido
    FOREIGN KEY (user_id) REFERENCES users(id),  -- Relación con la tabla de usuarios
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)  -- Relación con la tabla de métodos de pago
);