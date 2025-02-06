CREATE TABLE presentations (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Identificador único de la presentación
    product_id INT NOT NULL,  -- Relación con la tabla productos
    size ENUM('250g', '500g', '1kg') NOT NULL,  -- Tamaño del paquete
    price DECIMAL(10, 2) NOT NULL,  -- Precio específico para esta presentación
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE  -- Relación con la tabla `productos`
);