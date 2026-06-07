#!/usr/bin/env bash
# MapsWorkshop setup — iOS development (macOS only).
#
# Installs the JDK and Android SDK (the KMP project always configures an Android
# target, so the SDK is required even for an iOS-only workflow), the iOS
# toolchain (full Xcode + command-line tools + Tuist), writes local.properties with the
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

XCODE_APP_STORE_URL="https://apps.apple.com/app/xcode/id497799835"

# Full Xcode (not just the command-line tools) is required to build and run the
# iOS app on a simulator. Homebrew has no `xcode` cask — Apple ships Xcode only
# via the App Store / Developer portal — so we install it with `xcodes`, which
# downloads it from Apple. That step needs an interactive Apple ID login.
ensure_xcode() {
  if xcodebuild -version >/dev/null 2>&1; then
    ok "Xcode present ($(xcodebuild -version 2>/dev/null | head -n1))"
    return
  fi

  # Xcode.app may be installed but not selected (e.g. only CLT is active).
  local app
  app="$(ls -d /Applications/Xcode*.app 2>/dev/null | sort -V | tail -n1)"
  if [ -n "$app" ]; then
    warn "Xcode found at $app but not selected — selecting it (needs sudo)."
    sudo xcode-select -s "$app/Contents/Developer" 2>/dev/null || true
    if xcodebuild -version >/dev/null 2>&1; then
      sudo xcodebuild -license accept 2>/dev/null || true
      ok "Selected $(xcodebuild -version 2>/dev/null | head -n1)"
      return
    fi
  fi

  # Otherwise install the latest Xcode with xcodes (downloads from Apple).
  if ! has brew; then
    warn "Homebrew not found — install Xcode from the App Store: $XCODE_APP_STORE_URL"
    return
  fi
  has xcodes || brew install xcodes || { warn "Couldn't install xcodes — get Xcode from the App Store: $XCODE_APP_STORE_URL"; return; }
  has aria2  || brew install aria2 || true   # optional: xcodes uses it for much faster downloads

  step "Installing the latest Xcode via xcodes"
  warn "You'll be prompted for your Apple ID + password (and 2FA), then sudo for components."
  if xcodes install --latest; then
    sudo xcodebuild -runFirstLaunch 2>/dev/null || true
    sudo xcodebuild -license accept 2>/dev/null || true
    ok "Xcode installed: $(xcodebuild -version 2>/dev/null | head -n1)"
  else
    warn "xcodes install failed — install Xcode from the App Store: $XCODE_APP_STORE_URL"
  fi
}

ensure_ios() {
  step "iOS toolchain (Xcode + Tuist)"
  ensure_xcode
  # The command-line tools ship with Xcode, but make sure they're available
  # (Tuist and some Homebrew formulae rely on them being selectable).
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

ensure_brew
ensure_jdk
ensure_android_sdk
ensure_ios
write_local_properties
persist_env
gradle_prewarm
prewarm_ios

step "Done"
echo "iOS setup complete."
echo "  - iOS: open iosApp/MapsWorkshop.xcworkspace in Xcode (iOS is enabled automatically for xcode builds)."
echo "  - To edit iOS-specific code in Android Studio: add  workshop.enableIos=true  to ~/.gradle/gradle.properties"
