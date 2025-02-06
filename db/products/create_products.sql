CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- Identificador único del producto
    name VARCHAR(100) NOT NULL,            -- Nombre del producto
    description TEXT DEFAULT NULL,         -- Descripción del producto
    price DECIMAL(10, 2) NOT NULL,         -- Precio del producto (ej. 100.99)
    stock INT NOT NULL,                    -- Stock total disponible en gramos (para productos a granel)
    unit_measure ENUM('UNIT', 'GRAMS') NOT NULL,  -- Unidad de medida del producto
    category_id BIGINT NOT NULL,           -- Referencia a la tabla categorías
    expiration_date DATE DEFAULT NULL,     -- Fecha de vencimiento (opcional)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Fecha y hora de creación del producto
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- Fecha y hora de la última actualización
    FOREIGN KEY (category_id) REFERENCES categories(id)  -- Relación con la tabla `categories`
);