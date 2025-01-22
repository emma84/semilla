CREATE TABLE productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    precio_por_unidad DECIMAL(10, 2),
    precio_por_kg DECIMAL(10, 2),
    stock_unidades INT DEFAULT 0,
    stock_kg DECIMAL(10, 3) DEFAULT 0,
    tipo ENUM('UNIDAD', 'GRANEL') NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_vencimiento DATE DEFAULT NULL, -- Fecha de vencimiento (LocalDate)
    categoria_id INT, -- Relación con tabla categorías
    imagen_url VARCHAR(255), -- URL para la imagen del producto
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Fecha de creación (LocalDateTime)
    ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- Última actualización (LocalDateTime)
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) -- Relación con la tabla categorías
);