CREATE TABLE SIGELAB.dbo.Docente (
                                     id_docente INT IDENTITY(1,1) PRIMARY KEY,
                                     codigo_empleado VARCHAR(50) NOT NULL UNIQUE,
                                     nombres VARCHAR(100) NOT NULL,
                                     apellidos VARCHAR(100) NOT NULL,
                                     correo VARCHAR(150) NOT NULL UNIQUE,
                                     activo BIT DEFAULT 1 NOT NULL
);

CREATE TABLE SIGELAB.dbo.Usuario (
                                     id_usuario INT IDENTITY(1,1) PRIMARY KEY,
                                     usuario VARCHAR(50) NOT NULL UNIQUE,
                                     password_hash VARCHAR(255) NOT NULL,
                                     rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN', 'ENCARGADO', 'DOCENTE')),
                                     id_docente INT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Docente(id_docente),
                                     activo BIT DEFAULT 1 NOT NULL,
                                     fecha_creacion DATETIME DEFAULT GETDATE() NOT NULL
);

CREATE TABLE SIGELAB.dbo.Laboratorio (
                                         id_laboratorio INT IDENTITY(1,1) PRIMARY KEY,
                                         codigo VARCHAR(50) NOT NULL UNIQUE,
                                         nombre VARCHAR(100) NOT NULL,
                                         ubicacion VARCHAR(150) NULL,
                                         capacidad INT DEFAULT 0 CHECK (capacidad >= 0),
                                         activo BIT DEFAULT 1 NOT NULL
);

CREATE TABLE SIGELAB.dbo.TipoEquipo (
                                        id_tipo_equipo INT IDENTITY(1,1) PRIMARY KEY,
                                        nombre VARCHAR(50) NOT NULL UNIQUE,
                                        descripcion VARCHAR(255) NULL
);

CREATE TABLE SIGELAB.dbo.Equipo (
                                    id_equipo INT IDENTITY(1,1) PRIMARY KEY,
                                    codigo_inventario VARCHAR(50) NOT NULL UNIQUE,
                                    nombre VARCHAR(100) NOT NULL,
                                    id_tipo_equipo INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.TipoEquipo(id_tipo_equipo),
                                    marca VARCHAR(50) NULL,
                                    modelo VARCHAR(50) NULL,
                                    numero_serie VARCHAR(100) NULL,
                                    id_laboratorio INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Laboratorio(id_laboratorio),
                                    estado VARCHAR(20) DEFAULT 'DISPONIBLE' NOT NULL CHECK (estado IN ('DISPONIBLE', 'PRESTADO', 'DAÑADO', 'MANTENIMIENTO', 'BAJA')),
                                    observaciones VARCHAR(255) NULL
);

CREATE TABLE SIGELAB.dbo.Prestamo (
                                      id_prestamo INT IDENTITY(1,1) PRIMARY KEY,
                                      solicitante VARCHAR(150) NOT NULL,
                                      fecha_prestamo DATETIME DEFAULT GETDATE() NOT NULL,
                                      fecha_esperada DATETIME NOT NULL,
                                      id_usuario_registra INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Usuario(id_usuario),
                                      estado VARCHAR(20) DEFAULT 'ACTIVO' NOT NULL CHECK (estado IN ('ACTIVO', 'FINALIZADO', 'VENCIDO')),
                                      observaciones VARCHAR(255) NULL
);

CREATE TABLE SIGELAB.dbo.DetallePrestamo (
                                             id_detalle INT IDENTITY(1,1) PRIMARY KEY,
                                             id_prestamo INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Prestamo(id_prestamo),
                                             id_equipo INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Equipo(id_equipo),
                                             estado_devolucion VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL CHECK (estado_devolucion IN ('PENDIENTE', 'DEVUELTO_BUENO', 'DEVUELTO_DAÑADO'))
);

CREATE TABLE SIGELAB.dbo.Devolucion (
                                        id_devolucion INT IDENTITY(1,1) PRIMARY KEY,
                                        id_detalle INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.DetallePrestamo(id_detalle),
                                        fecha_devolucion DATETIME DEFAULT GETDATE() NOT NULL,
                                        condicion VARCHAR(20) NOT NULL CHECK (condicion IN ('BUENO', 'DAÑADO')),
                                        observaciones VARCHAR(255) NULL,
                                        id_usuario_registra INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Usuario(id_usuario)
);

CREATE TABLE SIGELAB.dbo.Mantenimiento (
                                           id_mantenimiento INT IDENTITY(1,1) PRIMARY KEY,
                                           id_equipo INT NOT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Equipo(id_equipo),
                                           fecha_ingreso DATETIME DEFAULT GETDATE() NOT NULL,
                                           tipo_mantenimiento VARCHAR(20) NOT NULL CHECK (tipo_mantenimiento IN ('PREVENTIVO', 'CORRECTIVO')),
                                           descripcion VARCHAR(255) NOT NULL,
                                           responsable VARCHAR(100) NOT NULL,
                                           fecha_salida DATETIME NULL,
                                           resultado VARCHAR(20) NULL CHECK (resultado IN ('REPARADO', 'NO_REPARABLE')),
                                           estado VARCHAR(20) DEFAULT 'EN_PROCESO' NOT NULL CHECK (estado IN ('EN_PROCESO', 'FINALIZADO')),
                                           observaciones VARCHAR(255) NULL
);

CREATE TABLE SIGELAB.dbo.bitacora_auditoria (
                                                id_auditoria INT IDENTITY(1,1) PRIMARY KEY,
                                                id_usuario INT NULL FOREIGN KEY REFERENCES SIGELAB.dbo.Usuario(id_usuario),
                                                usuario VARCHAR(50) NOT NULL,
                                                fecha_hora DATETIME DEFAULT GETDATE() NOT NULL,
                                                accion VARCHAR(50) NOT NULL,
                                                detalle VARCHAR(255) NULL
);

INSERT INTO SIGELAB.dbo.Docente (codigo_empleado, nombres, apellidos, correo, activo)
VALUES ('DOC-ADMIN', 'Administrador', 'Sistema', 'admin@sigelab.edu', 1);

INSERT INTO SIGELAB.dbo.Usuario (usuario, password_hash, rol, id_docente, activo)
VALUES ('admin', '$2a$10$zJN60hNn9TPYV/yQLvEHXe0cP5Lk.SQJPEg6yoyLBC/S2m25yW', 'ADMIN', 1, 1);