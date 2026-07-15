@echo off
echo Đang bien dich ma nguon Java...
if not exist "build\classes" mkdir "build\classes"
dir /s /B src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt

if %errorlevel% neq 0 (
    echo.
    echo [LOI] Bien dich that bai! Vui long kiem tra loi code o tren.
    del sources.txt
    pause
    exit /b %errorlevel%
)

echo.
echo Đang dong goi file 20.jar...
"C:\Program Files\Java\jdk-21.0.11\bin\jar.exe" cvfm 20.jar manifest.mf -C build/classes .

del sources.txt
echo.
echo [THANH CONG] Đa build xong file 20.jar moi! Ban co the chay run.bat de bat server.
pause
