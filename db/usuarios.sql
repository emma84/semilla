CREATE table USUARIOS (id int auto_increment primary key,
nombre varchar(50) not null,
apellido varchar(50) not null,
email varchar(100) not null,
password varchar(255) not null,
rol enum ('ADMIN', 'USER') default 'USER',
direccion varchar(255) not null);