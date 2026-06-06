#!/usr/bin/env bash
# MapsWorkshop setup — iOS development (macOS only).
#
# Installs the JDK and Android SDK (the KMP project always configures an Android
# target, so the SDK is required even for an iOS-only workflow), the iOS
# toolchain (Xcode command-line tools + Tuist), writes local.properties with the
# workshop keys, warms up Gradle and the iOS framework, and generates the Xcode
# workspace. It does not create an Android emulator — use scripts/setup-android.sh
# if you also want to run on Android.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/common.sh
. "$SCRIPT_DIR/common.sh"

if [ "$PLATFORM" != "macos" ]; then
  printf 'iOS development requires macOS (detected: %s).\n' "$PLATFORM" >&2
  printf 'Use scripts/setup-android.sh for Android development on this machine.\n' >&2
  exit 1
fi

ensure_ios() {
  step "iOS toolchain (Xcode CLT + Tuist)"
  xcode-select -p >/dev/null 2>&1 && ok "Xcode command-line tools present" \
    || { warn "Installing Xcode command-line tools…"; xcode-select --install || true; }
  has tuist && ok "Tuist present" \
    || { has brew && brew install tuist || warn "Install Tuist: brew install tuist"; }
}

prewarm_ios() {
  step "Pre-warming iOS (Kotlin/Native, MapKit xcframework)"
  ( cd "$PROJECT_ROOT" && ./gradlew --no-daemon -q :composeApp:linkDebugFrameworkIosSimulatorArm64 -Pworkshop.enableIos=true >/dev/null ) \
    && ok "iOS framework warm-up done" \
    || warn "iOS warm-up failed — the first Xcode build will be slower."
  has tuist && { ( cd "$PROJECT_ROOT" && ./iosApp/generate.sh ) \
    && ok "Xcode workspace generated: iosApp/MapsWorkshop.xcworkspace" \
    || warn "Tuist generation failed — run ./iosApp/generate.sh manually later."; }
}

printf 'MapsWorkshop setup — iOS (platform=%s)\n' "$PLATFORM"

ensure_jdk
ensure_android_sdk
ensure_ios
write_local_properties
gradle_prewarm
prewarm_ios

step "Done"
echo "iOS setup complete."
echo "  - iOS: open iosApp/MapsWorkshop.xcworkspace in Xcode (iOS is enabled automatically for xcode builds)."
echo "  - To edit iOS-specific code in Android Studio: add  workshop.enableIos=true  to ~/.gradle/gradle.properties"
