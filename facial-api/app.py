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
UMBRAL = float(os.environ.get("FACIAL_UMBRAL", "70"))

MUESTRAS_REGISTRO = 20      # fotos que se toman al registrar un rostro
MIN_MUESTRAS = 10           # minimo de fotos validas para aceptar el registro
INTENTOS_VERIFICACION = 5   # cuadros que se comparan al verificar
ACIERTOS_REQUERIDOS = 3     # de esos cuadros, cuantos deben coincidir
TAMANO_ROSTRO = (200, 200)
MODELO = "LBPH-OpenCV"
DIMENSION = 8 * 8 * 256     # rejilla 8x8 por 256 valores del histograma LBP

app = Flask(__name__)
app.json.ensure_ascii = False  # el cliente Java lee tildes tal cual, sin secuencias \uXXXX
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


def abrir_camara():
    cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
    if not cap.isOpened():
        return None
    for _ in range(20):  # los primeros cuadros salen oscuros mientras la camara se ajusta
        cap.read()
    return cap


def extraer_rostro(frame):
    """Devuelve el rostro mas grande de la imagen (gris, 200x200) o None."""
    gris = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    rostros = detector.detectMultiScale(gris, 1.3, 5, minSize=(80, 80))
    if len(rostros) == 0:
        return None
    x, y, w, h = max(rostros, key=lambda r: r[2] * r[3])
    rostro = cv2.resize(gris[y:y + h, x:x + w], TAMANO_ROSTRO)
    return cv2.equalizeHist(rostro)  # reduce el efecto de la iluminacion


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
            guardadas = 0
            limite = time.time() + 30
            while guardadas < MUESTRAS_REGISTRO and time.time() < limite:
                ok, frame = cap.read()
                if not ok:
                    continue
                rostro = extraer_rostro(frame)
                if rostro is None:
                    continue
                cv2.imwrite(str(temporal / f"{guardadas:02d}.jpg"), rostro)
                guardadas += 1
                time.sleep(0.2)  # pausa para variar un poco la pose entre fotos
        finally:
            cap.release()

    if guardadas < MIN_MUESTRAS:
        shutil.rmtree(temporal, ignore_errors=True)
        raise ErrorApi("No se capturaron suficientes muestras del rostro. "
                       "Mira a la cámara con buena iluminación e intenta de nuevo.", 422)

    previo = leer_meta(user_id)
    destino = carpeta_usuario(user_id)
    shutil.rmtree(destino, ignore_errors=True)
    temporal.rename(destino)
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
            limite = time.time() + 10
            while intentos < INTENTOS_VERIFICACION and time.time() < limite:
                ok, frame = cap.read()
                if not ok:
                    continue
                rostro = extraer_rostro(frame)
                if rostro is None:
                    continue
                etiqueta, distancia = reconocedor.predict(rostro)
                intentos += 1
                distancias.append(distancia)
                # Debe ser el usuario correcto Y estar bajo el umbral de distancia
                if etiqueta == user_id and distancia <= UMBRAL:
                    aciertos += 1
                time.sleep(0.2)
        finally:
            cap.release()

    if intentos == 0:
        return jsonify(success=False, matched=False, userId=user_id, score=None,
                       threshold=UMBRAL, livenessPassed=False, framesEvaluated=0,
                       message="No se detectó ningún rostro frente a la cámara.")

    reconocido = aciertos >= ACIERTOS_REQUERIDOS
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
    # debug=False: el modo debug reinicia el servidor y abriria la camara dos veces
    app.run(host="127.0.0.1", port=PUERTO, debug=False)
