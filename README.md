# Proyecto_GRUPO3

Aplicación de escritorio en Java (Swing) con SQL Server y JDBC. Arquitectura por capas:
`view -> controller -> service -> repository`. Las reglas de negocio (RN-01..RN-20) viven en `service`
y los permisos se validan ahí, no solo en el menú.

## Módulos
Login con segundo factor facial, usuarios y roles (ADMIN, ENCARGADO, DOCENTE), laboratorios, tipos de equipo,
equipos con código QR, préstamos, devoluciones (parciales), mantenimiento, auditoría, dashboard y reportes.

## Requisitos
- JDK 21, Maven, Docker (SQL Server 2022), Python 3 con OpenCV (solo para el reconocimiento facial).

## Puesta en marcha
Cada vez que se vaya a usar el sistema, seguir este orden:

1. **Docker corriendo.** Abrir Docker Desktop y esperar a que diga *Engine running*.
    - Primera vez: `docker run -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD=<clave> -p 1433:1433 --name sqlserver -d mcr.microsoft.com/mssql/server:2022-latest`
    - Las demás veces: comprobar con `docker ps` que el contenedor `sqlserver` esté en estado *Up*; si no aparece, iniciarlo con `docker start sqlserver`.
2. **Base de datos (solo la primera vez).** Crear la base `SIGELAB` y ejecutar `src/schema.sql` (crea el usuario inicial `admin`; la contraseña la define cada equipo).
3. **Configuración (solo la primera vez).** Copiar `src/main/resources/config.example.properties` a `config.properties` y completar `db.password`
   (ese archivo no se sube a Git).
4. **Servicio facial.**
    - Primera vez: instalar Python y las librerías (ver la sección **Instalar el servicio facial** más abajo).
    - Cada vez: hacer doble clic en `facial-api/iniciar_facial.bat`. Debe abrirse una ventana negra que dice `Running on http://127.0.0.1:8765`; **no cerrarla** mientras se use el sistema.
    - Para comprobarlo, abrir en el navegador `http://127.0.0.1:8765/health`.
5. **Ejecutar** `sigelab.Main` desde IntelliJ e iniciar sesión (usuario y contraseña, y luego mirar a la cámara). Registrar el rostro de cada usuario desde **Usuarios > Registrar rostro**.

## Instalar el servicio facial (una sola vez por computadora)
Requiere Windows con cámara web.

1. **Instalar Python 3.12 o superior** desde https://www.python.org/downloads/. En la primera pantalla del instalador, marcar **Add python.exe to PATH** antes de pulsar *Install Now*.
2. **Comprobar la instalación.** Abrir PowerShell y ejecutar `py --version`; debe mostrar la versión instalada.
3. **Crear el entorno virtual fuera de OneDrive** (la sincronización de OneDrive bloquea archivos):
   ```powershell
   py -m venv C:\venvs\sigelab-facial
   ```
4. **Instalar las librerías** (Flask, NumPy y OpenCV). Situarse en la carpeta del proyecto y ejecutar:
   ```powershell
   cd facial-api
   C:\venvs\sigelab-facial\Scripts\python.exe -m pip install -r requirements.txt
   ```
   Requiere internet solo esta vez.
5. **Probar.** Hacer doble clic en `iniciar_facial.bat`; debe aparecer `Running on http://127.0.0.1:8765`.

Si se usa otra ruta para el entorno virtual, definir la variable de entorno `FACIAL_VENV` con esa ruta antes de iniciar el `.bat`.
Más detalles del servicio en `facial-api/README.md`.

## Roles
| Rol | Alcance |
|---|---|
| ADMIN | Todo: usuarios, catálogos, equipos, préstamos, devoluciones, mantenimiento, auditoría y reportes |
| ENCARGADO | Equipos, préstamos, devoluciones, mantenimiento y consultas |
| DOCENTE | Solo consultas (equipos y reportes) |

## Estados de equipo
DISPONIBLE, PRESTADO, DANADO, MANTENIMIENTO, BAJA. "Vencido" no se guarda: se calcula (fecha esperada pasada y equipos pendientes).