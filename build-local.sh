#!/bin/sh
# Uses an existing Android SDK. All generated material remains in BUILD_DIR.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
: "${ANDROID_JAR:?Set ANDROID_JAR to platforms/android-36/android.jar}"
: "${BUILD_TOOLS:?Set BUILD_TOOLS to build-tools/36.0.0}"
BUILD_DIR=${BUILD_DIR:-"$ROOT/build-local"}
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/dex" "$BUILD_DIR/tests"
"$BUILD_TOOLS/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$BUILD_DIR/resources.zip"
sed 's/<manifest /<manifest package="app.sunward" /' "$ROOT/app/src/main/AndroidManifest.xml" > "$BUILD_DIR/AndroidManifest.xml"
"$BUILD_TOOLS/aapt2" link --version-code 3 --version-name 1.2 -I "$ANDROID_JAR" --manifest "$BUILD_DIR/AndroidManifest.xml" --java "$BUILD_DIR/generated" -o "$BUILD_DIR/base.apk" "$BUILD_DIR/resources.zip"
find "$ROOT/app/src/main/java" "$BUILD_DIR/generated" -name '*.java' > "$BUILD_DIR/sources.txt"
javac --release 8 -classpath "$ANDROID_JAR" -d "$BUILD_DIR/classes" @"$BUILD_DIR/sources.txt"
jar cf "$BUILD_DIR/classes.jar" -C "$BUILD_DIR/classes" .
"$BUILD_TOOLS/d8" --min-api 26 --lib "$ANDROID_JAR" --output "$BUILD_DIR/dex" "$BUILD_DIR/classes.jar"
cp "$BUILD_DIR/base.apk" "$BUILD_DIR/unsigned.apk"
(cd "$BUILD_DIR/dex" && zip -q "$BUILD_DIR/unsigned.apk" classes*.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"
if [ ! -f "$BUILD_DIR/debug.keystore" ]; then
keytool -genkeypair -keystore "$BUILD_DIR/debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Sunward local development' -keyalg RSA -keysize 2048 -validity 10000
fi
"$BUILD_TOOLS/apksigner" sign --ks "$BUILD_DIR/debug.keystore" --ks-pass pass:android --out "$BUILD_DIR/Sunward.apk" "$BUILD_DIR/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$BUILD_DIR/Sunward.apk"
javac --release 8 -d "$BUILD_DIR/tests" "$ROOT/app/src/main/java/app/sunward/Rules.java" "$ROOT/tests/RulesTest.java"
java -cp "$BUILD_DIR/tests" app.sunward.RulesTest
printf 'APK: %s\n' "$BUILD_DIR/Sunward.apk"
