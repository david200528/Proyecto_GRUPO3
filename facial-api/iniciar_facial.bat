@echo off
rem Inicia la API de reconocimiento facial de SIGELAB
if "%FACIAL_VENV%"=="" set FACIAL_VENV=C:\venvs\sigelab-facial
cd /d "%~dp0"
"%FACIAL_VENV%\Scripts\python.exe" app.py
pause