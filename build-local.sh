#!/bin/sh
# Gradle merges the scanner SDK's resources/manifest and resolves its dependencies.
# Use JDK 17 and set ANDROID_HOME to a local Android SDK (platform 36, tools 36.0.0).
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR=${BUILD_DIR:-"$ROOT/build-local"}
mkdir -p "$BUILD_DIR"
if [ -z "${SUNWARD_KEYSTORE:-}" ]; then
    SUNWARD_KEYSTORE="$BUILD_DIR/debug.keystore"
    if [ ! -f "$SUNWARD_KEYSTORE" ]; then
        keytool -genkeypair -keystore "$SUNWARD_KEYSTORE" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Sunward local development' -keyalg RSA -keysize 2048 -validity 10000
    fi
    export SUNWARD_KEYSTORE
fi
"$ROOT/gradlew" -p "$ROOT" :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
cp "$ROOT/app/build/outputs/apk/debug/app-debug.apk" "$BUILD_DIR/Sunward.apk"
printf 'APK: %s\n' "$BUILD_DIR/Sunward.apk"
