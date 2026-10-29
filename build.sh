#!/bin/sh
# GHOST GOLD build without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
#
#   ANDROID_HOME=/path/to/sdk sh build.sh      -> out/GhostGold.apk
#
# Needs: Android SDK (platforms;android-33 and build-tools), JDK 17+.
set -e
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-$ANDROID_SDK_ROOT}
[ -d "$SDK" ] || { echo "Set ANDROID_HOME to the Android SDK" >&2; exit 1; }
JAR=$SDK/platforms/${ANDROID_PLATFORM:-android-33}/android.jar
BT=$SDK/build-tools/${BUILD_TOOLS:-$(ls "$SDK/build-tools" | sort -V | tail -n 1)}
KS=${KEYSTORE:-ci/release.keystore}

rm -rf out && mkdir -p out/gen out/classes out/dex

"$BT/aapt2" compile --dir res -o out/res.zip
ASSETS=""
[ -d assets ] && ASSETS="-A assets"
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml \
    --min-sdk-version 25 --target-sdk-version 28 $ASSETS \
    --java out/gen -o out/base.apk out/res.zip

javac -nowarn -Xlint:-options --release 8 -encoding UTF-8 -classpath "$JAR" -d out/classes \
    $(find src out/gen -name '*.java')
"$BT/d8" --min-api 25 --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
(cd out/dex && jar uf ../base.apk classes.dex)

"$BT/zipalign" -f 4 out/base.apk out/aligned.apk
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:${KEYSTORE_PASS:-android} \
    --ks-key-alias ${KEY_ALIAS:-androiddebugkey} --out out/GhostGold.apk out/aligned.apk
echo "APK: out/GhostGold.apk"
