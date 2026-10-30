"""
SIGELAB - Componente de reconocimiento facial (segundo factor del login).

API local que usa la camara del equipo y OpenCV (LBPH). Cumple el contrato que
ya consume el cliente Java FacialAuthClient:

    POST   /face/verify          {"userId": 1}
    POST   /face/enroll          {"userId": 1, "replace": true}
    GET    /face/status/<id>
    DELETE /face/profile/<id>    {"adminUserId": 1}
    GET    /health

Los usuarios se identifican por su id_usuario de la base de datos.
Solo escucha en 127.0.0.1 (no es accesible desde otras computadoras).
Las fotos se guardan en facial-api/faces/<userId>/ (NO se suben a Git).
Esta API NO implementa deteccion de vida (fuera de alcance), por eso
livenessPassed siempre es false.

Mientras registra o verifica, abre una ventana con la imagen de la camara,
un recuadro sobre el rostro detectado y el avance del proceso.
"""

import json
import os
import shutil
import threading
import time
from datetime import datetime
from pathlib import Path

import cv2
import numpy as np
from flask import Flask, jsonify, request

BASE_DIR = Path(__file__).resolve().parent
FACES_DIR = BASE_DIR / "faces"

PUERTO = int(os.environ.get("FACIAL_PORT", "8765"))
# Distancia maxima aceptada: menor distancia = mas parecido.
# Se puede ajustar sin tocar el codigo: $env:FACIAL_UMBRAL = "65"
UMBRAL = float(os.environ.get("FACIAL_UMBRAL", "55"))
# Ventana con la imagen de la camara. Para desactivarla: $env:FACIAL_VISTA_PREVIA = "0"
VISTA_PREVIA = os.environ.get("FACIAL_VISTA_PREVIA", "1") != "0"

MUESTRAS_REGISTRO = 20      # fotos que se toman al registrar un rostro
MIN_MUESTRAS = 10           # minimo de fotos validas para aceptar el registro
INTENTOS_VERIFICACION = 5   # cuadros que se comparan al verificar
ACIERTOS_REQUERIDOS = 4     # de esos cuadros, cuantos deben coincidir
PAUSA_ENTRE_MUESTRAS = 0.2  # segundos entre una muestra y la siguiente
SEGUNDOS_RESULTADO = 1.2    # tiempo que se muestra el resultado en la ventana
TAMANO_ROSTRO = (200, 200)
MODELO = "LBPH-OpenCV"
DIMENSION = 8 * 8 * 256     # rejilla 8x8 por 256 valores del histograma LBP

VENTANA = "SIGELAB - Reconocimiento facial"
BLANCO, VERDE, ROJO, AZUL = (255, 255, 255), (0, 200, 0), (0, 0, 255), (255, 200, 0)  # BGR

app = Flask(__name__)
app.json.ensure_ascii = False  # el cliente Java lee tildes tal cual, sin secuencias \uXXXX
app.json.mimetype = "application/json; charset=utf-8"
camara_lock = threading.Lock()  # evita que dos peticiones usen la camara a la vez
detector = cv2.CascadeClassifier(
    cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
)


class ErrorApi(Exception):
    """Error con el formato {"detail": "..."} que FacialAuthClient muestra al usuario."""

    def __init__(self, detalle, estado):
        super().__init__(detalle)
        self.detalle = detalle
        self.estado = estado


@app.errorhandler(ErrorApi)
def manejar_error(error):
    return jsonify(detail=error.detalle), error.estado


def cuerpo_json():
    return request.get_json(silent=True) or {}


def leer_user_id(valor):
    if isinstance(valor, bool) or not isinstance(valor, int) or valor < 1:
        raise ErrorApi("El identificador de usuario no es válido.", 400)
    return valor


def ahora():
    return datetime.now().isoformat(timespec="seconds")


def carpeta_usuario(user_id):
    return FACES_DIR / str(user_id)


def muestras_de(user_id):
    carpeta = carpeta_usuario(user_id)
    return sorted(carpeta.glob("*.jpg")) if carpeta.is_dir() else []


def leer_meta(user_id):
    try:
        ruta = carpeta_usuario(user_id) / "meta.json"
        return json.loads(ruta.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


# ---------------------------------------------------------------------------
# Camara y ventana de vista previa
# ---------------------------------------------------------------------------

def abrir_camara():
    cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
    if not cap.isOpened():
        return None
    for _ in range(20):  # los primeros cuadros salen oscuros mientras la camara ajusta la exposicion
        cap.read()
    return cap


def abrir_ventana():
    if not VISTA_PREVIA:
        return
    cv2.namedWindow(VENTANA, cv2.WINDOW_AUTOSIZE)
    try:
        cv2.setWindowProperty(VENTANA, cv2.WND_PROP_TOPMOST, 1)  # que quede sobre las demas
    except cv2.error:
        pass


def cerrar_ventana():
    if not VISTA_PREVIA:
        return
    try:
        cv2.destroyWindow(VENTANA)
        cv2.waitKey(1)
    except cv2.error:
        pass


def mostrar(frame, texto, color=BLANCO, caja=None):
    """Muestra el cuadro de la camara con un recuadro sobre el rostro y un mensaje.
    Los textos van sin tildes porque la fuente de OpenCV no las dibuja."""
    if not VISTA_PREVIA:
        return
    imagen = frame.copy()
    if caja is not None:
        x, y, w, h = caja
        cv2.rectangle(imagen, (x, y), (x + w, y + h), color, 2)
    cv2.putText(imagen, texto, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2, cv2.LINE_AA)
    cv2.imshow(VENTANA, imagen)
    cv2.waitKey(1)


def mostrar_resultado(frame, texto, color, caja=None):
    """Deja el resultado a la vista unos instantes antes de cerrar la ventana."""
    if not VISTA_PREVIA:
        return
    mostrar(frame, texto, color, caja)
    cv2.waitKey(int(SEGUNDOS_RESULTADO * 1000))


def extraer_rostro(frame):
    """Devuelve (rostro gris 200x200, caja) del rostro mas grande, o (None, None)."""
    gris = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    rostros = detector.detectMultiScale(gris, 1.3, 5, minSize=(80, 80))
    if len(rostros) == 0:
        return None, None
    caja = tuple(int(v) for v in max(rostros, key=lambda r: r[2] * r[3]))
    x, y, w, h = caja
    rostro = cv2.resize(gris[y:y + h, x:x + w], TAMANO_ROSTRO)
    return cv2.equalizeHist(rostro), caja  # equalizeHist reduce el efecto de la iluminacion


def reemplazar_carpeta(origen, destino):
    """Reemplaza destino por origen. En Windows, borrar una carpeta puede fallar si
    algo la tiene abierta (OneDrive, el Explorador); por eso se reintenta."""
    for _ in range(5):
        try:
            if destino.exists():
                shutil.rmtree(destino)
            origen.rename(destino)
            return
        except OSError:
            time.sleep(0.5)
    raise ErrorApi("No se pudo guardar el rostro: la carpeta de registros está bloqueada. "
                   "Cierra el Explorador de archivos y vuelve a intentarlo.", 500)


def entrenar():
    """Entrena LBPH con todos los rostros registrados. La etiqueta es el userId."""
    imagenes, etiquetas = [], []
    if FACES_DIR.exists():
        for carpeta in FACES_DIR.iterdir():
            if not (carpeta.is_dir() and carpeta.name.isdigit()):
                continue  # ignora carpetas temporales o de pruebas anteriores
            for archivo in carpeta.glob("*.jpg"):
                imagen = cv2.imread(str(archivo), cv2.IMREAD_GRAYSCALE)
                if imagen is not None:
                    imagenes.append(imagen)
                    etiquetas.append(int(carpeta.name))
    if not imagenes:
        return None
    reconocedor = cv2.face.LBPHFaceRecognizer_create()
    reconocedor.train(imagenes, np.array(etiquetas))
    return reconocedor


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/health")
def health():
    return jsonify(estado="ok")


@app.post("/face/enroll")
def enroll():
    datos = cuerpo_json()
    user_id = leer_user_id(datos.get("userId"))
    reemplazar = bool(datos.get("replace", False))
    if muestras_de(user_id) and not reemplazar:
        raise ErrorApi("El usuario ya tiene un rostro registrado.", 409)

    # Se captura en una carpeta temporal: si falla, el registro anterior no se pierde
    temporal = FACES_DIR / f"tmp_{user_id}"
    shutil.rmtree(temporal, ignore_errors=True)
    temporal.mkdir(parents=True)

    with camara_lock:
        cap = abrir_camara()
        if cap is None:
            shutil.rmtree(temporal, ignore_errors=True)
            raise ErrorApi("No se pudo abrir la cámara.", 503)
        try:
            abrir_ventana()
            guardadas, ultimo, ultimo_frame, ultima_caja = 0, 0.0, None, None
            limite = time.time() + 30
            while guardadas < MUESTRAS_REGISTRO and time.time() < limite:
                ok, frame = cap.read()
                if not ok:
                    continue
                ultimo_frame = frame
                rostro, caja = extraer_rostro(frame)
                ultima_caja = caja
                mostrar(frame, f"Registrando rostro: {guardadas}/{MUESTRAS_REGISTRO} "
                               "(mueve un poco la cabeza)", AZUL, caja)
                if rostro is None or time.time() - ultimo < PAUSA_ENTRE_MUESTRAS:
                    continue
                cv2.imwrite(str(temporal / f"{guardadas:02d}.jpg"), rostro)
                guardadas += 1
                ultimo = time.time()

            if ultimo_frame is not None:
                if guardadas >= MIN_MUESTRAS:
                    mostrar_resultado(ultimo_frame, "Rostro registrado", VERDE, ultima_caja)
                else:
                    mostrar_resultado(ultimo_frame, "Registro incompleto", ROJO, ultima_caja)
        finally:
            cerrar_ventana()
            cap.release()

    if guardadas < MIN_MUESTRAS:
        shutil.rmtree(temporal, ignore_errors=True)
        raise ErrorApi("No se capturaron suficientes muestras del rostro. "
                       "Mira a la cámara con buena iluminación e intenta de nuevo.", 422)

    previo = leer_meta(user_id)
    destino = carpeta_usuario(user_id)
    reemplazar_carpeta(temporal, destino)
    meta = {"registeredAt": previo.get("registeredAt", ahora()), "updatedAt": ahora()}
    (destino / "meta.json").write_text(json.dumps(meta), encoding="utf-8")

    return jsonify(success=True, userId=user_id, model=MODELO, dimension=DIMENSION,
                   samples=guardadas, qualityScore=None, livenessPassed=False,
                   message="Rostro registrado correctamente.")


@app.post("/face/verify")
def verify():
    user_id = leer_user_id(cuerpo_json().get("userId"))
    if not muestras_de(user_id):
        raise ErrorApi("El usuario no tiene un rostro registrado.", 404)
    reconocedor = entrenar()

    intentos, aciertos, distancias = 0, 0, []
    with camara_lock:
        cap = abrir_camara()
        if cap is None:
            raise ErrorApi("No se pudo abrir la cámara.", 503)
        try:
            abrir_ventana()
            ultimo, ultimo_frame, ultima_caja = 0.0, None, None
            limite = time.time() + 10
            while intentos < INTENTOS_VERIFICACION and time.time() < limite:
                ok, frame = cap.read()
                if not ok:
                    continue
                ultimo_frame = frame
                rostro, caja = extraer_rostro(frame)
                ultima_caja = caja
                mostrar(frame, f"Verificando... {intentos}/{INTENTOS_VERIFICACION} "
                               "(mira a la camara)", AZUL, caja)
                if rostro is None or time.time() - ultimo < PAUSA_ENTRE_MUESTRAS:
                    continue
                etiqueta, distancia = reconocedor.predict(rostro)
                intentos += 1
                distancias.append(distancia)
                # Debe ser el usuario correcto Y estar bajo el umbral de distancia
                if etiqueta == user_id and distancia <= UMBRAL:
                    aciertos += 1
                ultimo = time.time()

            reconocido = aciertos >= ACIERTOS_REQUERIDOS
            if ultimo_frame is not None:
                if intentos == 0:
                    mostrar_resultado(ultimo_frame, "No se detecto ningun rostro", ROJO)
                elif reconocido:
                    mostrar_resultado(ultimo_frame, "Rostro reconocido", VERDE, ultima_caja)
                else:
                    mostrar_resultado(ultimo_frame, "Rostro NO reconocido", ROJO, ultima_caja)
        finally:
            cerrar_ventana()
            cap.release()

    if intentos == 0:
        return jsonify(success=False, matched=False, userId=user_id, score=None,
                       threshold=UMBRAL, livenessPassed=False, framesEvaluated=0,
                       message="No se detectó ningún rostro frente a la cámara.")

    return jsonify(success=True, matched=reconocido, userId=user_id,
                   score=round(float(np.mean(distancias)), 1),  # util para calibrar el umbral
                   threshold=UMBRAL, livenessPassed=False, framesEvaluated=intentos,
                   message="Rostro reconocido." if reconocido
                   else "El rostro no corresponde al usuario.")


@app.get("/face/status/<int:user_id>")
def status(user_id):
    muestras = muestras_de(user_id)
    meta = leer_meta(user_id)
    registrado = len(muestras) > 0
    return jsonify(userId=user_id, enrolled=registrado,
                   registeredAt=meta.get("registeredAt"), updatedAt=meta.get("updatedAt"),
                   model=MODELO if registrado else None,
                   dimension=DIMENSION if registrado else 0,
                   samples=len(muestras))


@app.delete("/face/profile/<int:user_id>")
def delete_profile(user_id):
    # La API es local y confia en quien la llama; solo valida que venga el admin.
    leer_user_id(cuerpo_json().get("adminUserId"))
    if not muestras_de(user_id):
        raise ErrorApi("El usuario no tiene un rostro registrado.", 404)
    shutil.rmtree(carpeta_usuario(user_id), ignore_errors=True)
    return jsonify(success=True, userId=user_id, message="Rostro eliminado.")


if __name__ == "__main__":
    # threaded=False: cada peticion se atiende en el hilo principal, que es donde
    # OpenCV necesita dibujar su ventana (en otros hilos puede congelarse en Windows).
    # debug=False: el modo debug reinicia el servidor y abriria la camara dos veces.
    app.run(host="127.0.0.1", port=PUERTO, debug=False, threaded=False)