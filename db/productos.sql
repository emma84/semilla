CREATE TABLE productos (
    id INT AUTO_INCREMENT PRIMARY KEY, -- Identificador único del producto
    nombre VARCHAR(100) NOT NULL, -- Nombre del producto
    descripcion TEXT DEFAULT NULL, -- Descripción del producto
    stock INT NOT NULL, -- Stock total disponible en gramos (para productos a granel)
    unidad_medida ENUM('UNIDAD', 'GRAMOS') NOT NULL, -- Tipo de medida del producto
    categoria_id INT NOT NULL, -- Relación con la tabla categorías
    fecha_vencimiento DATE DEFAULT NULL, -- Fecha de vencimiento (opcional)
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Fecha de creación del producto
    ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- Última actualización
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) -- Relación con la tabla `categorias`
);