CREATE TABLE presentaciones (
    id INT AUTO_INCREMENT PRIMARY KEY, -- Identificador único de la presentación
    producto_id INT NOT NULL, -- Relación con la tabla productos
    presentacion ENUM('250g', '500g', '1kg') NOT NULL, -- Tamaño de la presentación
    precio DECIMAL(10, 2) NOT NULL, -- Precio específico de esta presentación
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE -- Relación con `productos`
);