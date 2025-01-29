CREATE TABLE metodos_pago (
    id_metodo_pago INT AUTO_INCREMENT PRIMARY KEY, -- Identificador único del método de pago.
    nombre VARCHAR(100) NOT NULL,                 -- Nombre del método de pago (ej.: "Tarjeta de crédito").
    descripcion VARCHAR(255)                      -- Descripción adicional (opcional).
);