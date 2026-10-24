/* =====================================================================
   SIGELAB - Sistema de Gestion y Control de Laboratorios Universitarios
   Script inicial para SQL Server
   ===================================================================== */

IF DB_ID('SIGELAB') IS NULL
    CREATE DATABASE SIGELAB;
GO

USE SIGELAB;
GO


/* ---------------------------------------------------------------------
   1. CATALOGOS
   --------------------------------------------------------------------- */

CREATE TABLE Rol (
    id_rol      INT IDENTITY(1,1) PRIMARY KEY,
    nombre      NVARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE TipoEquipo (
    id_tipo     INT IDENTITY(1,1) PRIMARY KEY,
    nombre      NVARCHAR(50)  NOT NULL UNIQUE,
    descripcion NVARCHAR(150) NULL
);

CREATE TABLE Laboratorio (
    id_laboratorio INT IDENTITY(1,1) PRIMARY KEY,
    codigo         NVARCHAR(20)  NOT NULL UNIQUE,                -- RN-03
    nombre         NVARCHAR(100) NOT NULL,
    ubicacion      NVARCHAR(150) NOT NULL,
    capacidad      INT           NOT NULL CHECK (capacidad > 0),
    estado         VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO'
                   CHECK (estado IN ('ACTIVO','INACTIVO'))
);

/* ---------------------------------------------------------------------
   2. USUARIOS
   --------------------------------------------------------------------- */

CREATE TABLE Usuario (
    id_usuario    INT IDENTITY(1,1) PRIMARY KEY,
    nombre        NVARCHAR(60)  NOT NULL,
    apellido      NVARCHAR(60)  NOT NULL,
    username      NVARCHAR(40)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)   NOT NULL,        -- SHA-256 en hexadecimal, nunca texto plano
    correo        NVARCHAR(100) NOT NULL UNIQUE,
    id_rol        INT           NOT NULL,
    estado        VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO'
                  CHECK (estado IN ('ACTIVO','INACTIVO')),   -- RN-01
    CONSTRAINT FK_Usuario_Rol FOREIGN KEY (id_rol) REFERENCES Rol(id_rol)
);

/* ---------------------------------------------------------------------
   3. EQUIPOS
   --------------------------------------------------------------------- */

CREATE TABLE Equipo (
    id_equipo         INT IDENTITY(1,1) PRIMARY KEY,
    codigo_inventario NVARCHAR(30)  NOT NULL UNIQUE,         -- RN-04 (tambien es el valor del QR)
    nombre            NVARCHAR(100) NOT NULL,
    id_tipo           INT           NOT NULL,
    marca             NVARCHAR(50)  NOT NULL,
    modelo            NVARCHAR(50)  NOT NULL,
    numero_serie      NVARCHAR(60)  NOT NULL UNIQUE,
    id_laboratorio    INT           NOT NULL,
    estado            VARCHAR(15)   NOT NULL DEFAULT 'DISPONIBLE'
                      CHECK (estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA')),
    observaciones     NVARCHAR(255) NULL,
    CONSTRAINT FK_Equipo_Tipo FOREIGN KEY (id_tipo)        REFERENCES TipoEquipo(id_tipo),
    CONSTRAINT FK_Equipo_Lab  FOREIGN KEY (id_laboratorio) REFERENCES Laboratorio(id_laboratorio)
);

/* ---------------------------------------------------------------------
   4. PRESTAMOS (encabezado + detalle) Y DEVOLUCIONES
   --------------------------------------------------------------------- */

CREATE TABLE Prestamo (
    id_prestamo               INT IDENTITY(1,1) PRIMARY KEY,
    id_solicitante            INT          NOT NULL,         -- quien se lleva los equipos
    id_usuario_registra       INT          NOT NULL,         -- quien registra el prestamo
    fecha_prestamo            DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    fecha_esperada_devolucion DATETIME2    NOT NULL,
    estado                    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO'
                              CHECK (estado IN ('ACTIVO','FINALIZADO')),
    observaciones             NVARCHAR(255) NULL,
    CONSTRAINT CK_Prestamo_Fechas CHECK (fecha_esperada_devolucion >= fecha_prestamo),
    CONSTRAINT FK_Prestamo_Solicitante FOREIGN KEY (id_solicitante)      REFERENCES Usuario(id_usuario),
    CONSTRAINT FK_Prestamo_Registra    FOREIGN KEY (id_usuario_registra) REFERENCES Usuario(id_usuario)
);

CREATE TABLE DetallePrestamo (
    id_detalle  INT IDENTITY(1,1) PRIMARY KEY,
    id_prestamo INT NOT NULL,
    id_equipo   INT NOT NULL,
    CONSTRAINT FK_Detalle_Prestamo FOREIGN KEY (id_prestamo) REFERENCES Prestamo(id_prestamo),
    CONSTRAINT FK_Detalle_Equipo   FOREIGN KEY (id_equipo)   REFERENCES Equipo(id_equipo),
    CONSTRAINT UQ_Detalle_Prestamo_Equipo UNIQUE (id_prestamo, id_equipo)   -- RN-06
);

CREATE TABLE Devolucion (
    id_devolucion     INT IDENTITY(1,1) PRIMARY KEY,
    id_detalle        INT          NOT NULL UNIQUE,          -- RN-11: un detalle solo se devuelve una vez
    id_usuario_recibe INT          NOT NULL,
    fecha_devolucion  DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    condicion         VARCHAR(10)  NOT NULL CHECK (condicion IN ('BUENO','DANADO')),
    observaciones     NVARCHAR(255) NULL,
    CONSTRAINT FK_Devolucion_Detalle FOREIGN KEY (id_detalle)        REFERENCES DetallePrestamo(id_detalle),
    CONSTRAINT FK_Devolucion_Usuario FOREIGN KEY (id_usuario_recibe) REFERENCES Usuario(id_usuario)
);

/* ---------------------------------------------------------------------
   5. MANTENIMIENTO
   --------------------------------------------------------------------- */

CREATE TABLE Mantenimiento (
    id_mantenimiento INT IDENTITY(1,1) PRIMARY KEY,
    id_equipo        INT           NOT NULL,
    tipo             VARCHAR(12)   NOT NULL CHECK (tipo IN ('PREVENTIVO','CORRECTIVO')),
    fecha_ingreso    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    descripcion      NVARCHAR(255) NOT NULL,
    responsable      NVARCHAR(100) NOT NULL,
    fecha_salida     DATETIME2     NULL,
    resultado        VARCHAR(15)   NULL CHECK (resultado IN ('REPARADO','NO_REPARABLE')),
    estado           VARCHAR(12)   NOT NULL DEFAULT 'EN_PROCESO'
                     CHECK (estado IN ('EN_PROCESO','FINALIZADO')),
    observaciones    NVARCHAR(255) NULL,
    CONSTRAINT CK_Mant_Fechas CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_ingreso),
    CONSTRAINT FK_Mant_Equipo FOREIGN KEY (id_equipo) REFERENCES Equipo(id_equipo)
);

/* ---------------------------------------------------------------------
   6. AUDITORIA
   --------------------------------------------------------------------- */

CREATE TABLE Auditoria (
    id_auditoria INT IDENTITY(1,1) PRIMARY KEY,
    id_usuario   INT           NULL,           -- NULL cuando el login falla con un usuario inexistente
    fecha_hora   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    accion       VARCHAR(50)   NOT NULL,       -- ej. PRESTAMO_CREADO
    entidad      VARCHAR(50)   NOT NULL,       -- ej. Prestamo
    id_registro  INT           NULL,
    descripcion  NVARCHAR(255) NULL,
    CONSTRAINT FK_Auditoria_Usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);
GO
/* ---------------------------------------------------------------------
   7. DATOS MINIMOS DE PRUEBA
   Contrasenas de prueba: admin123 / encargado123 / docente123
   --------------------------------------------------------------------- */

INSERT INTO Rol (nombre) VALUES ('ADMIN'), ('ENCARGADO'), ('DOCENTE');

INSERT INTO Usuario (nombre, apellido, username, password_hash, correo, id_rol)
VALUES
 (N'Admin',     N'Sistema', 'admin',     CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','admin123'),2),     'admin@umg.edu.gt',     1),
 (N'Encargado', N'Lab',     'encargado', CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','encargado123'),2), 'encargado@umg.edu.gt', 2),
 (N'Docente',   N'Prueba',  'docente',   CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','docente123'),2),   'docente@umg.edu.gt',   3);

INSERT INTO TipoEquipo (nombre, descripcion) VALUES
 (N'Laptop',    N'Computadoras portatiles'),
 (N'Camara',    N'Camaras fotograficas y de video'),
 (N'Proyector', N'Proyectores multimedia'),
 (N'Router',    N'Equipos de enrutamiento'),
 (N'Tablet',    N'Tabletas');

INSERT INTO Laboratorio (codigo, nombre, ubicacion, capacidad) VALUES
 ('LAB-01', N'Laboratorio de Computo 1', N'Edificio A, nivel 2', 30),
 ('LAB-02', N'Laboratorio de Redes',     N'Edificio B, nivel 1', 20);

INSERT INTO Equipo (codigo_inventario, nombre, id_tipo, marca, modelo, numero_serie, id_laboratorio) VALUES
 ('LAP-001', N'Laptop Dell 1',   1, 'Dell',  'Latitude 5420', 'SN-LAP-001', 1),
 ('LAP-002', N'Laptop Dell 2',   1, 'Dell',  'Latitude 5420', 'SN-LAP-002', 1),
 ('CAM-001', N'Camara Canon 1',  2, 'Canon', 'EOS 250D',      'SN-CAM-001', 1),
 ('PRO-001', N'Proyector Epson', 3, 'Epson', 'X49',           'SN-PRO-001', 1),
 ('ROU-001', N'Router Cisco',    4, 'Cisco', 'ISR 1100',      'SN-ROU-001', 2);