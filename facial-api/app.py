"""
SIGELAB - Componente de reconocimiento facial (segundo factor del login).

API local que usa la camara del equipo y OpenCV (LBPH).
Endpoints:
    GET  /health  -> comprueba que la API esta encendida
    POST /enroll  -> registra el rostro de un usuario   {"username": "admin"}
    POST /verify  -> verifica el rostro de un usuario   {"username": "admin"}

Solo escucha en 127.0.0.1 (no es accesible desde otras computadoras).
Las fotos del rostro se guardan en facial-api/faces/ (NO se suben a Git).
"""

import os
import re
import threading
import time
from pathlib import Path

import cv2
import numpy as np
from flask import Flask, jsonify, request

BASE_DIR = Path(__file__).resolve().parent
FACES_DIR = BASE_DIR / "faces"

# Distancia maxima aceptada: menor distancia = mas parecido.
# Se puede ajustar sin tocar el codigo: $env:FACIAL_UMBRAL = "65"
UMBRAL = float(os.environ.get("FACIAL_UMBRAL", "70"))

MUESTRAS_REGISTRO = 20      # fotos que se toman al registrar un rostro
MIN_MUESTRAS = 10           # minimo de fotos validas para aceptar el registro
INTENTOS_VERIFICACION = 5   # cuadros que se comparan al verificar
ACIERTOS_REQUERIDOS = 3     # de esos cuadros, cuantos deben coincidir
TAMANO_ROSTRO = (200, 200)
USUARIO_VALIDO = re.compile(r"^[A-Za-z0-9_.-]{1,40}$")

app = Flask(__name__)
camara_lock = threading.Lock()  # evita que dos peticiones usen la camara a la vez
detector = cv2.CascadeClassifier(
    cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
)


def abrir_camara():
    cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
    if not cap.isOpened():
        return None
    for _ in range(5):  # los primeros cuadros salen oscuros mientras la camara se ajusta
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
    """Entrena LBPH con los rostros registrados. Devuelve (reconocedor, nombres)."""
    nombres, imagenes, etiquetas = [], [], []
    if not FACES_DIR.exists():
        return None, nombres
    carpetas = sorted(p for p in FACES_DIR.iterdir() if p.is_dir())
    for etiqueta, carpeta in enumerate(carpetas):
        nombres.append(carpeta.name)
        for archivo in carpeta.glob("*.jpg"):
            imagen = cv2.imread(str(archivo), cv2.IMREAD_GRAYSCALE)
            if imagen is not None:
                imagenes.append(imagen)
                etiquetas.append(etiqueta)
    if not imagenes:
        return None, nombres
    reconocedor = cv2.face.LBPHFaceRecognizer_create()
    reconocedor.train(imagenes, np.array(etiquetas))
    return reconocedor, nombres


def username_de_la_peticion():
    datos = request.get_json(silent=True) or {}
    username = str(datos.get("username", ""))
    return username if USUARIO_VALIDO.match(username) else None


@app.get("/health")
def health():
    return jsonify(estado="ok")


@app.post("/enroll")
def enroll():
    username = username_de_la_peticion()
    if username is None:
        return jsonify(ok=False, motivo="Nombre de usuario invalido"), 400

    with camara_lock:
        cap = abrir_camara()
        if cap is None:
            return jsonify(ok=False, motivo="No se pudo abrir la camara"), 503
        try:
            carpeta = FACES_DIR / username
            carpeta.mkdir(parents=True, exist_ok=True)
            for anterior in carpeta.glob("*.jpg"):  # un nuevo registro reemplaza al anterior
                anterior.unlink()

            guardadas = 0
            limite = time.time() + 30
            while guardadas < MUESTRAS_REGISTRO and time.time() < limite:
                ok, frame = cap.read()
                if not ok:
                    continue
                rostro = extraer_rostro(frame)
                if rostro is None:
                    continue
                cv2.imwrite(str(carpeta / f"{guardadas:02d}.jpg"), rostro)
                guardadas += 1
                time.sleep(0.2)  # pausa para variar un poco la pose entre fotos
        finally:
            cap.release()

    if guardadas < MIN_MUESTRAS:
        return jsonify(ok=False, muestras=guardadas,
                       motivo="No se capturaron suficientes muestras del rostro"), 422
    return jsonify(ok=True, muestras=guardadas)


@app.post("/verify")
def verify():
    username = username_de_la_peticion()
    if username is None:
        return jsonify(reconocido=False, motivo="Nombre de usuario invalido"), 400

    reconocedor, nombres = entrenar()
    if reconocedor is None or username not in nombres:
        return jsonify(reconocido=False, motivo="El usuario no tiene rostro registrado")
    etiqueta_usuario = nombres.index(username)

    intentos, aciertos, distancias = 0, 0, []
    with camara_lock:
        cap = abrir_camara()
        if cap is None:
            return jsonify(reconocido=False, motivo="No se pudo abrir la camara"), 503
        try:
            limite = time.time() + 6
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
                if etiqueta == etiqueta_usuario and distancia <= UMBRAL:
                    aciertos += 1
                time.sleep(0.2)
        finally:
            cap.release()

    if intentos == 0:
        return jsonify(reconocido=False, motivo="No se detecto ningun rostro")

    distancia_media = round(float(np.mean(distancias)), 1)  # util para calibrar el umbral
    if aciertos >= ACIERTOS_REQUERIDOS:
        return jsonify(reconocido=True, motivo="Rostro reconocido", distancia=distancia_media)
    return jsonify(reconocido=False, motivo="El rostro no corresponde al usuario",
                   distancia=distancia_media)


if __name__ == "__main__":
    # debug=False: el modo debug reinicia el servidor y abriria la camara dos veces
    app.run(host="127.0.0.1", port=5000, debug=False)
