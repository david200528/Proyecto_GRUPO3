IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'SIGELAB')
    CREATE DATABASE SIGELAB;
GO

USE SIGELAB;
GO

CREATE TABLE usuarios (
                          id_usuario INT IDENTITY(1,1) PRIMARY KEY,
                          carnet_dpi VARCHAR(20) NOT NULL UNIQUE,
                          nombre VARCHAR(100) NOT NULL,
                          correo VARCHAR(100) NOT NULL UNIQUE,
                          password VARCHAR(255) NOT NULL,
                          rol VARCHAR(30) NOT NULL
);

CREATE TABLE laboratorios (
                              id_laboratorio INT IDENTITY(1,1) PRIMARY KEY,
                              codigo VARCHAR(20) NOT NULL UNIQUE,
                              nombre VARCHAR(100) NOT NULL,
                              id_catedratico INT FOREIGN KEY REFERENCES usuarios(id_usuario)
);

CREATE TABLE equipos (
                         id_equipo INT IDENTITY(1,1) PRIMARY KEY,
                         codigo_inventario VARCHAR(50) NOT NULL UNIQUE,
                         nombre VARCHAR(100) NOT NULL,
                         id_laboratorio INT FOREIGN KEY REFERENCES laboratorios(id_laboratorio),
                         estado VARCHAR(20) DEFAULT 'DISPONIBLE'
);

CREATE TABLE prestamos (
                           id_prestamo INT IDENTITY(1,1) PRIMARY KEY,
                           id_estudiante INT FOREIGN KEY REFERENCES usuarios(id_usuario),
                           id_equipo INT FOREIGN KEY REFERENCES equipos(id_equipo),
                           fecha_prestamo DATETIME DEFAULT GETDATE(),
                           estado VARCHAR(20) DEFAULT 'ACTIVO'
);

CREATE TABLE asistencias (
                             id_asistencia INT IDENTITY(1,1) PRIMARY KEY,
                             id_estudiante INT FOREIGN KEY REFERENCES usuarios(id_usuario),
                             id_laboratorio INT FOREIGN KEY REFERENCES laboratorios(id_laboratorio),
                             fecha_hora DATETIME DEFAULT GETDATE(),
                             estado VARCHAR(20) NOT NULL
);

CREATE TABLE bitacora_auditoria (
                                    id_bitacora INT IDENTITY(1,1) PRIMARY KEY,
                                    id_usuario INT FOREIGN KEY REFERENCES usuarios(id_usuario),
                                    accion VARCHAR(100) NOT NULL,
                                    fecha_hora DATETIME DEFAULT GETDATE()
);