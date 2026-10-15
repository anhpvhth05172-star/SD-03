@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================
echo    POLYSHOES - Khoi chay he thong
echo ============================================
echo.

REM --- 1. Doc MAIL_USERNAME / MAIL_PASSWORD tu BE\.env (file nay da bi gitignore) ---
if not exist "BE\.env" (
    echo [LOI] Thieu file BE\.env
    echo       Hay tao file BE\.env voi 2 dong:
    echo           MAIL_USERNAME=tai_khoan_gmail
    echo           MAIL_PASSWORD=mat_khau_ung_dung
    echo.
    pause
    exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("BE\.env") do (
    if not "%%B"=="" set "%%A=%%B"
)

if "%MAIL_USERNAME%"=="" (
    echo [LOI] Chua doc duoc MAIL_USERNAME tu BE\.env
    pause
    exit /b 1
)
if "%MAIL_PASSWORD%"=="" (
    echo [LOI] Chua doc duoc MAIL_PASSWORD tu BE\.env
    pause
    exit /b 1
)
echo [OK] MAIL_USERNAME=%MAIL_USERNAME%

REM --- 2. Neu chua co JAR thi build ---
if not exist "BE\target\DATN-0.0.1-SNAPSHOT.jar" (
    echo [..] Chua co JAR, dang build...
    pushd BE
    call mvnw.cmd -q package -DskipTests
    if errorlevel 1 (
        echo [LOI] Build that bai - xem lai loi compile o tren.
        popd
        pause
        exit /b 1
    )
    popd
)

REM --- 3. Dung backend cu neu dang giu port 8080 (de khong trung port) ---
echo [..] Kiem tra port 8080...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r ":8080 .*LISTENING"') do (
    echo     Dung process cu PID %%P tren 8080
    taskkill /F /PID %%P >nul 2>&1
)
ping -n 3 127.0.0.1 >nul 2>&1

REM --- 4. Khoi chay Backend (port 8080 - FE mac dinh goi http://localhost:8080/api) ---
REM     Bien MAIL_* da set o tren -> process java con keo theo tu dong.
echo [..] Khoi chay Backend tren http://localhost:8080 ...
start "PolyShoes BE :8080" cmd /k "cd /d "%~dp0BE" && java -jar target\DATN-0.0.1-SNAPSHOT.jar --server.port=8080"

REM --- 5. Khoi chay Frontend neu chua chay ---
netstat -ano | findstr /r ":5173 .*LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [OK]  Frontend da chay tren 5173 - bo qua
) else (
    echo [..] Khoi chay Frontend tren http://localhost:5173 ...
    start "PolyShoes FE :5173" cmd /k "cd /d "%~dp0FE\sd03_fe" && npm run dev"
)

echo.
echo ------------------------------------------------------------------
echo Backend mat khoang 15 giay de boot. Mo FE tai http://localhost:5173
echo LAM Y: dung dong cua so Backend (o so nay) khi con muon gui mail.
echo        Moi lan chay lai chay chay.bat nay la du.
echo ------------------------------------------------------------------
pause
