/* =====================================================================
   SIGELAB - Sistema de Gestion y Control de Laboratorios Universitarios
   Script inicial para SQL Server
   Las tablas y columnas coinciden con las que usa el codigo Java
   (paquete sigelab.repository) y con src/schema.sql.
   ===================================================================== */

IF DB_ID('SIGELAB') IS NULL
    CREATE DATABASE SIGELAB;
GO

USE SIGELAB;
GO

/* ---------------------------------------------------------------------
   1. USUARIOS
   El rol se guarda como texto (enum RolUsuario en Java).
   La contrasena se guarda con BCrypt (UsuarioService), nunca en texto plano.
   --------------------------------------------------------------------- */

CREATE TABLE dbo.Usuario (
    id_usuario     INT IDENTITY(1,1) PRIMARY KEY,
    nombre         VARCHAR(80)  NOT NULL,
    apellido       VARCHAR(80)  NOT NULL,
    usuario        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    correo         VARCHAR(150) NOT NULL UNIQUE,
    rol            VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN','ENCARGADO','DOCENTE')),
    activo         BIT          NOT NULL DEFAULT 1,                 -- RN-01
    fecha_creacion DATETIME     NOT NULL DEFAULT GETDATE()
);

/* ---------------------------------------------------------------------
   2. CATALOGOS
   --------------------------------------------------------------------- */

CREATE TABLE dbo.Laboratorio (
    id_laboratorio INT IDENTITY(1,1) PRIMARY KEY,
    codigo         VARCHAR(50)  NOT NULL UNIQUE,                    -- RN-03
    nombre         VARCHAR(100) NOT NULL,
    ubicacion      VARCHAR(150) NULL,
    capacidad      INT          DEFAULT 0 CHECK (capacidad >= 0),
    activo         BIT          NOT NULL DEFAULT 1
);

CREATE TABLE dbo.TipoEquipo (
    id_tipo_equipo INT IDENTITY(1,1) PRIMARY KEY,
    nombre         VARCHAR(50)  NOT NULL UNIQUE,
    descripcion    VARCHAR(255) NULL
);

/* ---------------------------------------------------------------------
   3. EQUIPOS
   --------------------------------------------------------------------- */

CREATE TABLE dbo.Equipo (
    id_equipo         INT IDENTITY(1,1) PRIMARY KEY,
    codigo_inventario VARCHAR(50)  NOT NULL UNIQUE,                 -- RN-04 (tambien es el valor del QR)
    nombre            VARCHAR(100) NOT NULL,
    id_tipo_equipo    INT          NOT NULL,
    marca             VARCHAR(50)  NULL,
    modelo            VARCHAR(50)  NULL,
    numero_serie      VARCHAR(100) NULL,
    id_laboratorio    INT          NOT NULL,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE'    -- RN-07, RN-08: solo DISPONIBLE se presta
                      CHECK (estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA')),
    observaciones     VARCHAR(255) NULL,
    CONSTRAINT FK_Equipo_Tipo FOREIGN KEY (id_tipo_equipo) REFERENCES dbo.TipoEquipo(id_tipo_equipo),
    CONSTRAINT FK_Equipo_Lab  FOREIGN KEY (id_laboratorio) REFERENCES dbo.Laboratorio(id_laboratorio)
);

/* ---------------------------------------------------------------------
   4. PRESTAMOS (encabezado + detalle) Y DEVOLUCIONES
   Al registrar un prestamo, PrestamoRepository.crear() inserta el
   encabezado, los detalles y cambia cada equipo a PRESTADO en una sola
   transaccion (RN-09).
   --------------------------------------------------------------------- */

CREATE TABLE dbo.Prestamo (
    id_prestamo         INT IDENTITY(1,1) PRIMARY KEY,
    solicitante         VARCHAR(150) NOT NULL,                      -- quien se lleva los equipos
    fecha_prestamo      DATETIME     NOT NULL DEFAULT GETDATE(),
    fecha_esperada      DATETIME     NOT NULL,                      -- fecha esperada de devolucion
    id_usuario_registra INT          NOT NULL,                      -- quien registra el prestamo
    estado              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO'
                        CHECK (estado IN ('ACTIVO','FINALIZADO')),
    observaciones       VARCHAR(255) NULL,
    CONSTRAINT CK_Prestamo_Fechas   CHECK (fecha_esperada >= fecha_prestamo),
    CONSTRAINT FK_Prestamo_Registra FOREIGN KEY (id_usuario_registra) REFERENCES dbo.Usuario(id_usuario)
);

CREATE TABLE dbo.DetallePrestamo (                                -- RN-05: un prestamo tiene 1 o mas detalles
    id_detalle        INT IDENTITY(1,1) PRIMARY KEY,
    id_prestamo       INT         NOT NULL,
    id_equipo         INT         NOT NULL,
    estado_devolucion VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado_devolucion IN ('PENDIENTE','DEVUELTO')),
    CONSTRAINT FK_Detalle_Prestamo FOREIGN KEY (id_prestamo) REFERENCES dbo.Prestamo(id_prestamo),
    CONSTRAINT FK_Detalle_Equipo   FOREIGN KEY (id_equipo)   REFERENCES dbo.Equipo(id_equipo),
    CONSTRAINT UQ_Detalle_Prestamo_Equipo UNIQUE (id_prestamo, id_equipo)   -- RN-06
);

CREATE TABLE dbo.Devolucion (
    id_devolucion       INT IDENTITY(1,1) PRIMARY KEY,
    id_detalle          INT          NOT NULL UNIQUE,               -- RN-11: un detalle solo se devuelve una vez
    fecha_devolucion    DATETIME     NOT NULL DEFAULT GETDATE(),
    condicion           VARCHAR(20)  NOT NULL CHECK (condicion IN ('BUENO','DANADO')),
    observaciones       VARCHAR(255) NULL,
    id_usuario_registra INT          NOT NULL,
    CONSTRAINT FK_Devolucion_Detalle FOREIGN KEY (id_detalle)          REFERENCES dbo.DetallePrestamo(id_detalle),
    CONSTRAINT FK_Devolucion_Usuario FOREIGN KEY (id_usuario_registra) REFERENCES dbo.Usuario(id_usuario)
);

/* ---------------------------------------------------------------------
   5. MANTENIMIENTO
   --------------------------------------------------------------------- */

CREATE TABLE dbo.Mantenimiento (
    id_mantenimiento   INT IDENTITY(1,1) PRIMARY KEY,
    id_equipo          INT          NOT NULL,
    fecha_ingreso      DATETIME     NOT NULL DEFAULT GETDATE(),
    tipo_mantenimiento VARCHAR(20)  NOT NULL CHECK (tipo_mantenimiento IN ('PREVENTIVO','CORRECTIVO')),
    descripcion        VARCHAR(255) NOT NULL,
    responsable        VARCHAR(100) NOT NULL,
    fecha_salida       DATETIME     NULL,
    resultado          VARCHAR(20)  NULL CHECK (resultado IN ('REPARADO','NO_REPARABLE')),
    estado             VARCHAR(20)  NOT NULL DEFAULT 'EN_PROCESO'
                       CHECK (estado IN ('EN_PROCESO','FINALIZADO')),
    observaciones      VARCHAR(255) NULL,
    CONSTRAINT CK_Mant_Fechas CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_ingreso),
    CONSTRAINT FK_Mant_Equipo FOREIGN KEY (id_equipo) REFERENCES dbo.Equipo(id_equipo)
);

/* ---------------------------------------------------------------------
   6. AUDITORIA
   --------------------------------------------------------------------- */

CREATE TABLE dbo.bitacora_auditoria (
    id_auditoria INT IDENTITY(1,1) PRIMARY KEY,
    id_usuario   INT          NULL,          -- NULL cuando el login falla con un usuario inexistente
    usuario      VARCHAR(50)  NOT NULL,
    fecha_hora   DATETIME     NOT NULL DEFAULT GETDATE(),
    accion       VARCHAR(50)  NOT NULL,      -- ej. PRESTAMO_CREADO
    entidad      VARCHAR(50)  NULL,          -- ej. Prestamo
    id_registro  INT          NULL,
    detalle      VARCHAR(255) NULL,
    CONSTRAINT FK_Auditoria_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario)
);
GO

/* ---------------------------------------------------------------------
   7. DATOS MINIMOS DE PRUEBA
   Contrasenas de prueba: admin123 / encargado123 / docente123
   (hashes BCrypt, los mismos que valida UsuarioService.autenticar)
   --------------------------------------------------------------------- */

INSERT INTO dbo.Usuario (nombre, apellido, usuario, password_hash, correo, rol, activo) VALUES
 ('Admin',     'Sistema', 'admin',     '$2a$10$v23aQc8Mk/W0K5kwlHUni.Lw.4OVU63.dukErqlFhUK5WltUfBarO', 'admin@umg.edu.gt',     'ADMIN',     1),
 ('Encargado', 'Lab',     'encargado', '$2a$10$mGTlz8Q446hyPbpkCnYu9ugWlJD8VISFmClAjd4Ek3i0yM5gvvbtO', 'encargado@umg.edu.gt', 'ENCARGADO', 1),
 ('Docente',   'Prueba',  'docente',   '$2a$10$MXt.Qet53TLdhdlOSv1a0.XF9Xcw3/WCZMgX9hK.KOpnQmyHRWWAO', 'docente@umg.edu.gt',   'DOCENTE',   1);

INSERT INTO dbo.TipoEquipo (nombre, descripcion) VALUES
 ('Laptop',    'Computadoras portatiles'),
 ('Camara',    'Camaras fotograficas y de video'),
 ('Proyector', 'Proyectores multimedia'),
 ('Router',    'Equipos de enrutamiento'),
 ('Tablet',    'Tabletas');

INSERT INTO dbo.Laboratorio (codigo, nombre, ubicacion, capacidad) VALUES
 ('LAB-01', 'Laboratorio de Computo 1', 'Edificio A, nivel 2', 30),
 ('LAB-02', 'Laboratorio de Redes',     'Edificio B, nivel 1', 20);

INSERT INTO dbo.Equipo (codigo_inventario, nombre, id_tipo_equipo, marca, modelo, numero_serie, id_laboratorio) VALUES
 ('LAP-001', 'Laptop Dell 1',   1, 'Dell',  'Latitude 5420', 'SN-LAP-001', 1),
 ('LAP-002', 'Laptop Dell 2',   1, 'Dell',  'Latitude 5420', 'SN-LAP-002', 1),
 ('CAM-001', 'Camara Canon 1',  2, 'Canon', 'EOS 250D',      'SN-CAM-001', 1),
 ('PRO-001', 'Proyector Epson', 3, 'Epson', 'X49',           'SN-PRO-001', 1),
 ('ROU-001', 'Router Cisco',    4, 'Cisco', 'ISR 1100',      'SN-ROU-001', 2);
GO
