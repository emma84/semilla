CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- Identificador único del usuario
    first_name VARCHAR(100) NOT NULL,      -- Nombre del usuario
    last_name VARCHAR(100) NOT NULL,       -- Apellido del usuario
    email VARCHAR(150) NOT NULL UNIQUE,    -- Dirección de correo electrónico del usuario
    password VARCHAR(255) NOT NULL,        -- Contraseña del usuario
    role ENUM('ADMIN', 'CLIENT') DEFAULT 'CLIENT',  -- Rol del usuario (Administrador o Cliente)
    street VARCHAR(100) NOT NULL,          -- Calle de la dirección del usuario
    number INT NOT NULL,                   -- Número de la dirección (número de la calle)
    floor VARCHAR(10) DEFAULT NULL,        -- Piso de la dirección del usuario (opcional)
    department VARCHAR(10) DEFAULT NULL,   -- Departamento (apartamento) (opcional)
    city VARCHAR(100) NOT NULL,            -- Ciudad de la dirección del usuario
    province VARCHAR(100) NOT NULL,        -- Provincia de la dirección del usuario
    postal_code VARCHAR(20) NOT NULL,      -- Código postal de la dirección del usuario
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Marca temporal de la creación del usuario
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  -- Marca temporal de la última actualización
);