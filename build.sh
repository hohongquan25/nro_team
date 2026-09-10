#!/bin/bash

# Auto-detect jar / javac path if not in standard PATH
if ! command -v jar &> /dev/null; then
    if [ -f "/c/Program Files/Java/jdk-21.0.11/bin/jar.exe" ]; then
        export PATH="/c/Program Files/Java/jdk-21.0.11/bin:$PATH"
    elif [ -n "$JAVA_HOME" ] && [ -f "$JAVA_HOME/bin/jar" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

echo "Đang bien dich ma nguon Java..."
mkdir -p build/classes
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt

if [ $? -ne 0 ]; then
    echo "[LOI] Bien dich that bai! Vui long kiem tra loi code o tren."
    rm -f sources.txt
    exit 1
fi

echo "Đang dong goi file 20.jar..."
jar cvfm 20.jar manifest.mf -C build/classes .

if [ $? -ne 0 ]; then
    echo "[LOI] Dong goi file 20.jar that bai! Hay tat server dang chay (Ctrl+C o terminal ./run.sh) truoc khi build vi file 20.jar dang bi khoa."
    rm -f sources.txt
    exit 1
fi

rm -f sources.txt
echo "[THANH CONG] Đa build xong file 20.jar moi! Ban co the chay ./run.sh de bat server."
