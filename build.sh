#!/bin/bash
echo "Đang bien dich ma nguon Java..."
mkdir -p build/classes
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build/classes @sources.txt

if [ $? -ne 0 ]; then
    echo "[LOI] Bien dich that bai! Vui long kiem tra loi code o tren."
    rm sources.txt
    exit 1
fi

echo "Đang dong goi file 20.jar..."
jar cvfm 20.jar manifest.mf -C build/classes .
rm sources.txt
echo "[THANH CONG] Đa build xong file 20.jar moi! Ban co the chay ./run.sh de bat server."
