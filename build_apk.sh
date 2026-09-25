#!/usr/bin/env bash
set -euo pipefail

TARGET="${1:-}"
ANDROID_DIR="/home/nedp/Desarrollo/Proyectos/Guia-Farmaceutica/android"

cd "$ANDROID_DIR"

# Validar target
if [[ -z "$TARGET" ]]; then
    echo "Uso: $0 <debug|release>"
    exit 1
fi

if [[ "$TARGET" != "debug" && "$TARGET" != "release" ]]; then
    echo "Target inválido: $TARGET (debug|release)"
    exit 1
fi

# Limpiar builds anteriores (para asegurar que recursos reciclados no persistan)
./gradlew clean

# Compilar
./gradlew assemble${TARGET^}

APK_DIR="app/build/outputs/apk/${TARGET}/"
APK_PATH="$ANDROID_DIR/$APK_DIR"

echo "APK generado en: $APK_PATH"
ls -la "$APK_PATH"