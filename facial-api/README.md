# Componente de reconocimiento facial – SIGELAB

API local en Python que usa la cámara del equipo para validar el rostro de un usuario. Se usa como **segundo factor del inicio de sesión**: primero se validan usuario y contraseña, y después el rostro.

El cliente Java es `FacialAuthClient` (paquete `sigelab.integration.facial`).

> La documentación completa (arquitectura, flujo, pruebas y problemas resueltos) está en
> `docs/Documentacion_Reconocimiento_Facial_SIGELAB.docx`.

## Cómo funciona

```
Java (LoginForm / UsuarioForm → FacialAuthClient)  ──HTTP──►  API Flask (app.py, 127.0.0.1:8765)
                                                                     │
                                                                     ▼
                                                 Cámara + OpenCV (detección Haar + reconocimiento LBPH)
```

- Cada usuario se identifica por su `id_usuario` de la base de datos. A la API **solo se le envía ese id**: nunca contraseñas ni imágenes.
- **Registro:** se toman 20 fotos del rostro (en gris, 200×200) y se guardan en `faces/<userId>/`. Si se obtienen menos de 10 válidas, el registro se rechaza.
- **Verificación (1 a 1):** se compara el rostro frente a la cámara **solo con el rostro registrado del usuario que escribió la contraseña**. Se evalúan hasta 5 cuadros y se aprueba si al menos 4 tienen una distancia menor o igual al umbral (55 por defecto; menor distancia = más parecido).
- Mientras trabaja, abre una ventana con la imagen de la cámara, un recuadro sobre el rostro y el resultado (verde = reconocido, rojo = no reconocido).

## Flujo del login en SIGELAB

1. El usuario escribe usuario y contraseña.
2. Si la contraseña es correcta, Java consulta `GET /face/status/<id>`.
3. **Si ya tiene rostro:** se verifica con la cámara (`POST /face/verify`).
4. **Si no tiene rostro:** se pregunta *“¿Deseas registrar tu rostro ahora?”*. Si acepta, se registra (`POST /face/enroll`), se verifica a continuación y se anota `ROSTRO_REGISTRADO` en la auditoría. Si no acepta, el acceso se deniega.
5. Solo si el rostro es reconocido se abre el menú principal.

El administrador también puede registrar, actualizar o eliminar rostros desde **Usuarios**.

## Requisitos

- Windows con cámara web.
- Python 3.12 o superior. Durante la instalación, marcar **Add python.exe to PATH**.
- Conexión a internet solo para instalar las librerías la primera vez.

## Instalación (una sola vez)

Crear el entorno virtual **fuera de OneDrive**, porque la sincronización bloquea archivos. En PowerShell, desde la carpeta del proyecto:

```powershell
py -m venv C:\venvs\sigelab-facial
cd facial-api
C:\venvs\sigelab-facial\Scripts\python.exe -m pip install -r requirements.txt
```

Si se usa otra ruta para el entorno, definir la variable `FACIAL_VENV` con esa ruta antes de iniciar la API.

`requirements.txt` fija `opencv-contrib-python<5`: el reconocedor LBPH (`cv2.face`) viene en la versión *contrib*, y OpenCV 5 no incluye el detector de rostros por defecto.

## Cómo ejecutarla

Doble clic en `iniciar_facial.bat` (o `python app.py` con el entorno activo). Debe aparecer:

```
Running on http://127.0.0.1:8765
```

La ventana debe quedar **abierta** mientras se use el sistema. Si se cierra, Java mostrará que no pudo comunicarse con el servicio facial y el acceso se denegará.

Orden de arranque: **1)** Docker con SQL Server, **2)** API facial, **3)** aplicación Java (`sigelab.Main`).

Para comprobar que está encendida: abrir `http://127.0.0.1:8765/health` en el navegador. Debe responder `{"estado": "ok"}`.

## Probar la API a mano

Con la API encendida y mirando a la cámara con buena luz de frente (reemplazar `1` por el `id_usuario` real):

```powershell
# Registrar (mover un poco la cabeza durante unos 5-10 segundos)
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8765/face/enroll -ContentType "application/json" -Body '{"userId":1,"replace":true}'

# Verificar
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8765/face/verify -ContentType "application/json" -Body '{"userId":1}'

# Consultar si hay rostro registrado
Invoke-RestMethod http://127.0.0.1:8765/face/status/1
```

Un registro correcto responde `success: True` y `samples: 20`. Una verificación correcta responde `matched: True` con un `score` aproximado de entre 30 y 50.

## Endpoints

| Método | Ruta | Cuerpo | Descripción |
|---|---|---|---|
| GET | `/health` | – | Comprueba que la API está encendida |
| POST | `/face/enroll` | `{"userId": 1, "replace": true}` | Registra (o reemplaza) el rostro del usuario |
| POST | `/face/verify` | `{"userId": 1}` | Verifica el rostro frente a la cámara |
| GET | `/face/status/<id>` | – | Indica si el usuario tiene rostro registrado |
| DELETE | `/face/profile/<id>` | `{"adminUserId": 1}` | Elimina el rostro registrado |

Todos los errores responden `{"detail": "mensaje"}` con su código HTTP, y Java muestra ese mensaje al usuario:

| Código | Significado |
|---|---|
| 400 | Identificador de usuario no válido |
| 404 | El usuario no tiene rostro registrado |
| 409 | Ya tiene rostro registrado y `replace` es falso |
| 422 | No se capturaron suficientes muestras |
| 500 | Error interno (por ejemplo, carpeta bloqueada); el mensaje indica la causa |
| 503 | No se pudo abrir la cámara |

## Configuración

| Variable de entorno | Valor por defecto | Para qué sirve |
|---|---|---|
| `FACIAL_PORT` | `8765` | Puerto de la API |
| `FACIAL_UMBRAL` | `55` | Distancia máxima aceptada. Más bajo (por ejemplo 45) = más estricto; más alto = más permisivo |
| `FACIAL_VISTA_PREVIA` | `1` | `0` desactiva la ventana de la cámara |
| `FACIAL_VENV` | `C:\venvs\sigelab-facial` | Ruta del entorno virtual que usa el `.bat` |
| `FACIAL_API_URL` | `http://127.0.0.1:8765` | Dirección que usa el cliente Java (si se cambia el puerto) |

Ejemplo para ajustar el umbral antes de iniciar: `$env:FACIAL_UMBRAL = "45"`.

## Privacidad

Los rostros son datos personales. La carpeta `faces/` está en el `.gitignore` y **no debe subirse al repositorio**. Registrar el rostro de otra persona solo con su consentimiento. Para borrar un rostro: desde **Usuarios → Eliminar rostro**, con `DELETE /face/profile/<id>` o eliminando la carpeta `faces/<id>`.

## Limitaciones

- **Sin detección de vida (liveness):** una fotografía podría engañarlo. Está fuera del alcance del proyecto, por eso `livenessPassed` siempre responde `false`.
- **Sensible a la iluminación:** registrar y verificar con luz parecida, de frente.
- Como la verificación es 1 a 1, **una misma persona puede tener su rostro en varias cuentas**.
- Si la API o la cámara fallan, el cliente Java **deniega** el acceso (nunca lo permite por defecto).

## Pruebas

| Caso | Cómo probarlo | Resultado esperado |
|---|---|---|
| CP-01 | Credenciales correctas + rostro del usuario | Acceso permitido |
| CP-02 | Credenciales correctas + rostro de otra persona | Acceso denegado (`matched: False`, ventana en rojo) |
| CP-03 | Usuario sin rostro que acepta registrarlo en el login | Registra 20 fotos, verifica y entra |
| CP-04 | Usuario sin rostro que no acepta registrarlo | Acceso denegado y anotado en auditoría |
| CP-05 | Eliminar el rostro y volver a registrarlo | Registro correcto |
| CP-06 | Misma persona registrada en dos cuentas | Ambas cuentas permiten el acceso |
| CP-07 | API apagada | Acceso denegado, aviso de servicio no disponible |

## Solución de problemas

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| Java: “No se pudo comunicar con el servicio facial” | API apagada o puerto distinto | Iniciar `iniciar_facial.bat` y revisar el puerto |
| 503 o “No se pudo abrir la cámara” | Otra aplicación usa la cámara | Cerrar Teams, Zoom o el navegador |
| “No se capturaron suficientes muestras” | Poca luz o rostro fuera de cuadro | Luz de frente, rostro completo, mover poco la cabeza |
| `matched: False` con el rostro correcto | Luz distinta a la del registro | Actualizar el rostro (`replace: true`) o subir un poco `FACIAL_UMBRAL` |
| `matched: True` con el rostro de otra persona | Umbral demasiado alto | Bajar `FACIAL_UMBRAL` según los `score` medidos |
| Error 500 de carpeta bloqueada | OneDrive o el Explorador tienen abierta `faces/` | Cerrar el Explorador o pausar OneDrive y reintentar |
| `ModuleNotFoundError` (por ejemplo `cv2`) | Faltan librerías o no se usa el entorno virtual | Ejecutar `pip install -r requirements.txt` con el Python de `C:\venvs\sigelab-facial` |

### Problemas resueltos

- **Volver a registrar un rostro eliminado daba error 500 (`FileExistsError`).** OneDrive dejaba vacías y bloqueadas las carpetas `faces/<id>` y `faces/tmp_<id>`. Ahora `app.py` reutiliza las carpetas existentes (`vaciar_carpeta`) y mueve las fotos una por una en lugar de borrar y renombrar carpetas.
- **La misma persona registrada en dos cuentas no podía entrar en ninguna.** La verificación comparaba contra todos los usuarios (1 a N) y exigía que el más parecido fuera el correcto. Ahora es 1 a 1: solo se compara con el rostro del usuario que escribió la contraseña.
- **Mensajes genéricos “rechazó la solicitud (HTTP)”.** Ahora cualquier error inesperado responde `{"detail": ...}` y Java muestra la causa real.
