@echo off
chcp 65001 > nul
rem ==========================================================================
rem  Pruebas funcionales principales de Kinet (escenarios BDD marcados @principal)
rem
rem  Uso, desde la carpeta kinet-backend:   .\pruebas-funcionales.cmd
rem  Para correr TODAS las pruebas:          .\todas-las-pruebas.cmd
rem
rem  No necesita Oracle, ni el backend encendido, ni Maven instalado:
rem  usa H2 en memoria y el Maven Wrapper (backend\mvnw.cmd). Solo pide Java.
rem ==========================================================================
setlocal
set "BACKEND=%~dp0backend"
rem Maven reenvía la salida de las pruebas en UTF-8 (tildes de los escenarios)
set "MAVEN_OPTS=%MAVEN_OPTS% -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
rem En Java 24 o superior, silencia el aviso de Lombok sobre sun.misc.Unsafe (Java 17 no conoce la opción)
java --sun-misc-unsafe-memory-access=allow -version > nul 2> nul && set "MAVEN_OPTS=%MAVEN_OPTS% --sun-misc-unsafe-memory-access=allow"

echo.
echo  Pruebas funcionales principales (escenarios @principal)
echo.
echo  Compilando y levantando la aplicación en memoria... (tarda alrededor de 1 minuto)
call "%BACKEND%\mvnw.cmd" -B -q -f "%BACKEND%\pom.xml" test -Dtest=EjecutarPruebasFuncionalesTest -Dsurefire.failIfNoSpecifiedTests=false "-Dcucumber.filter.tags=@principal"
if errorlevel 1 (
    echo.
    echo [ERROR] Fallaron pruebas. Detalle: backend\target\cucumber\reporte.html
    exit /b 1
)
echo.
echo [OK] Reporte: backend\target\cucumber\reporte.html
exit /b 0