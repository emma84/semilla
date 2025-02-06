CREATE TABLE payment_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- Identificador único del método de pago
    name VARCHAR(100) NOT NULL,            -- Nombre del método de pago (por ejemplo, "Tarjeta de Crédito")
    description VARCHAR(255)               -- Descripción adicional (opcional)
);
