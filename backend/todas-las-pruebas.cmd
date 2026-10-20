@echo off
rem Acceso directo: el script real está en la carpeta kinet-backend
call "%~dp0..\todas-las-pruebas.cmd" %*
exit /b %errorlevel%
