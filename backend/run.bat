@echo off
echo ========================================================
echo   Starting Factory Traffic Management System Backend...
echo   Web Dashboard will be available at: http://localhost:8080/
echo   Swagger UI will be available at: http://localhost:8080/swagger-ui.html
echo ========================================================
cd /d "%~dp0"
call mvnw.cmd spring-boot:run
pause
