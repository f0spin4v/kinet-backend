@echo off
rem Acceso directo: el script real está en la carpeta kinet-backend
call "%~dp0..\pruebas-funcionales.cmd" %*
exit /b %errorlevel%
