Claude


























Readme · MD
Componente de reconocimiento facial – SIGELAB
API local en Python que usa la cámara del equipo para validar el rostro de un usuario. Se usa únicamente como segundo factor del inicio de sesión: primero se validan usuario y contraseña, y después el rostro. El cliente Java es FacialAuthClient (paquete sigelab.integration.facial).

Cómo funciona
Java (LoginForm → FacialAuthClient)  ──HTTP──►  API Flask (app.py, 127.0.0.1:8765)
│
▼
Cámara + OpenCV (detección Haar + reconocimiento LBPH)
Cada usuario se identifica por su id_usuario de la base de datos.
Al registrar, se toman 20 fotos del rostro (en gris, 200x200) y se guardan en faces/<userId>/.
Al verificar, se comparan hasta 5 cuadros de la cámara contra los rostros registrados. Se aprueba si al menos 4 cuadros corresponden al usuario correcto con una distancia menor o igual al umbral (55 por defecto; menor distancia = más parecido).
Mientras trabaja, abre una ventana con la imagen de la cámara, un recuadro sobre el rostro y el resultado (verde = reconocido, rojo = no reconocido).
Requisitos
Windows con cámara web.
Python 3.12 o superior (probado con Python 3.14 y OpenCV 4.14). Durante la instalación, marcar Add python.exe to PATH.
Conexión a internet solo para instalar las librerías la primera vez.
Instalación (una sola vez)
Se recomienda crear el entorno virtual fuera de OneDrive, porque la sincronización bloquea archivos. En PowerShell:

powershell
py -m venv C:\venvs\sigelab-facial
C:\venvs\sigelab-facial\Scripts\python.exe -m pip install -r requirements.txt
Si prefieres otra ruta para el entorno, define la variable FACIAL_VENV con esa ruta antes de iniciar la API.

requirements.txt fija opencv-contrib-python<5: el módulo de reconocimiento (LBPH) viene en la versión contrib, y OpenCV 5 no incluye el detector de rostros por defecto.

Cómo ejecutarla
Doble clic en iniciar_facial.bat (o, desde PowerShell, python app.py con el entorno activo). Debe aparecer Running on http://127.0.0.1:8765.

La ventana negra debe quedar abierta mientras se use el login con rostro. Si se cierra, Java mostrará que no pudo comunicarse con el servicio facial y el acceso se denegará.

Antes de usar el sistema, el orden es: 1) base de datos encendida, 2) API facial encendida, 3) aplicación Java.

Registrar y verificar un rostro
Con la API encendida, mirando a la cámara con buena luz de frente (reemplazar 1 por el id_usuario real):

powershell
# Registrar (mover un poco la cabeza durante unos 5-10 segundos)
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8765/face/enroll -ContentType "application/json" -Body '{"userId":1,"replace":true}'

# Verificar
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8765/face/verify -ContentType "application/json" -Body '{"userId":1}'

# Consultar si hay rostro registrado
Invoke-RestMethod http://127.0.0.1:8765/face/status/1
El registro correcto responde success: True y samples: 20. Una verificación correcta responde matched: True con un score de entre 30 y 50, aproximadamente.

Endpoints
Método	Ruta	Cuerpo	Descripción
GET	/health	–	Comprueba que la API está encendida
POST	/face/enroll	{"userId": 1, "replace": true}	Registra (o reemplaza) el rostro del usuario
POST	/face/verify	{"userId": 1}	Verifica el rostro frente a la cámara
GET	/face/status/<id>	–	Indica si el usuario tiene rostro registrado
DELETE	/face/profile/<id>	{"adminUserId": 1}	Elimina el rostro registrado
Los errores responden {"detail": "mensaje"} con el código HTTP correspondiente (404 sin rostro registrado, 409 ya registrado y replace en falso, 503 cámara no disponible).

Configuración
Variable de entorno	Valor por defecto	Para qué sirve
FACIAL_PORT	8765	Puerto de la API
FACIAL_UMBRAL	55	Distancia máxima aceptada. Bajarlo (por ejemplo 45) hace la verificación más estricta; subirlo la hace más permisiva
FACIAL_VISTA_PREVIA	1	0 desactiva la ventana de la cámara
FACIAL_VENV	C:\venvs\sigelab-facial	Ruta del entorno virtual que usa el .bat
FACIAL_API_URL	http://127.0.0.1:8765	Dirección que usa el cliente Java (si se cambia el puerto)
Ejemplo para ajustar el umbral antes de iniciar: $env:FACIAL_UMBRAL = "45".

Privacidad
Los rostros son datos personales. La carpeta faces/ está en el .gitignore y no debe subirse al repositorio. Registrar el rostro de otra persona solo con su consentimiento. Para borrar un rostro: DELETE /face/profile/<id> o eliminar la carpeta faces/<id>.

Limitaciones
No incluye detección de vida (liveness): una fotografía podría engañarlo. Está fuera del alcance del proyecto, por eso livenessPassed siempre responde false.
LBPH es sensible a la iluminación. Registrar y verificar con luz parecida, de frente.
Si la API o la cámara fallan, el cliente Java deniega el acceso (nunca lo permite por defecto).
Pruebas (casos de la guía)
Caso	Cómo probarlo	Resultado esperado
CP-01	Credenciales correctas + rostro del usuario	Acceso permitido
CP-02	Credenciales correctas + rostro de otra persona	Acceso denegado (matched: False, ventana en rojo)
Extra	API apagada	Acceso denegado, aviso de servicio no disponible
Solución de problemas
Síntoma	Causa probable	Qué hacer
Java: "No se pudo comunicar con el servicio facial"	API apagada o puerto distinto	Iniciar iniciar_facial.bat y revisar el puerto
503 o "No se pudo abrir la cámara"	Otra aplicación usa la cámara	Cerrar Teams, Zoom o el navegador
samples menor a 20 o "No se capturaron suficientes muestras"	Poca luz o rostro fuera de cuadro	Luz de frente, rostro completo, mover poco la cabeza
matched: False con el rostro correcto	Luz distinta a la del registro	Volver a registrar con replace: true, o subir un poco FACIAL_UMBRAL
matched: True con el rostro de otra persona	Umbral demasiado alto o solo hay un rostro registrado	Bajar FACIAL_UMBRAL según los score medidos y registrar a más personas
500 al registrar y el log muestra FileExistsError o PermissionError	Carpeta faces bloqueada (OneDrive o el Explorador)	Ejecutar la API desde una carpeta fuera de OneDrive y cerrar el Explorador
ModuleNotFoundError	No se usa el entorno virtual	Revisar que exista C:\venvs\sigelab-facial o definir FACIAL_VENV

Claude ha terminado la respuesta
