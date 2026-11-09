@echo off
echo ===================================================
echo     POLYSHOES - STARTING SERVICES
echo ===================================================
echo 1. Backend:  http://localhost:8080
echo 2. Frontend: http://localhost:5173
echo ===================================================

start "PolyShoes - Backend (8080)" cmd /k "cd /d %~dp0BE && mvnw.cmd spring-boot:run"
start "PolyShoes - Frontend (5173)" cmd /k "cd /d %~dp0FE\sd03_fe && npm run dev"

echo Started Backend and Frontend in separate windows!
