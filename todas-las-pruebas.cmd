@echo off
chcp 65001 > nul
rem ==========================================================================
rem  TODAS las pruebas del backend de Kinet:
rem   - los escenarios BDD de src\test\resources\features (no solo @principal)
rem   - las pruebas de integración JUnit de src\test\java
rem
rem  Uso, desde la carpeta kinet-backend:   .\todas-las-pruebas.cmd
rem  Tarda unos minutos. Para solo las principales: .\pruebas-funcionales.cmd
rem ==========================================================================
setlocal
set "BACKEND=%~dp0backend"
set "MAVEN_OPTS=%MAVEN_OPTS% -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
java --sun-misc-unsafe-memory-access=allow -version > nul 2> nul && set "MAVEN_OPTS=%MAVEN_OPTS% --sun-misc-unsafe-memory-access=allow"

echo.
echo  Todas las pruebas del backend (BDD + JUnit)
echo.
call "%BACKEND%\mvnw.cmd" -B -f "%BACKEND%\pom.xml" test
if errorlevel 1 (
    echo.
    echo [ERROR] Fallaron pruebas. Detalle: backend\target\surefire-reports
    exit /b 1
)
echo.
echo [OK] Todas las pruebas pasaron. Cobertura: backend\target\site\jacoco\index.html
exit /b 0