#!/usr/bin/env bash
# MapsWorkshop setup — Android development.
#
# Installs the JDK and Android SDK, creates and tunes an emulator, writes
# local.properties with the workshop keys, and warms up Gradle.
#
# Runs on macOS, Linux, and Windows (Git Bash). For iOS development on macOS,
# use scripts/setup-ios.sh instead (it also covers everything here plus Xcode).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/common.sh
. "$SCRIPT_DIR/common.sh"

EMULATOR_API=35
EMULATOR_AVD_NAME="WorkshopAVD"
EMULATOR_AVD_READY=0

emulator_abi() {
  case "$(uname -m)" in
    arm64|aarch64) printf 'arm64-v8a' ;;
    *)             printf 'x86_64' ;;
  esac
}

set_avd_prop() {
  local file="$1" key="$2" value="$3"
  if grep -q "^$key=" "$file" 2>/dev/null; then
    sed -i.bak "s|^$key=.*|$key=$value|" "$file" && rm -f "$file.bak"
  else
    printf '%s=%s\n' "$key" "$value" >> "$file"
  fi
}

ensure_emulator() {
  step "Android emulator"
  [ -n "$SDK_DIR" ] || { warn "No Android SDK — skipping emulator."; return; }

  local avd_home; avd_home="$(to_unix "${ANDROID_AVD_HOME:-$HOME/.android/avd}")"
  if ls "$avd_home"/*.avd >/dev/null 2>&1; then
    ok "Emulator already configured — skipping."
    EMULATOR_AVD_READY=1
    return
  fi

  local sm bindir avdm abi image cfg
  sm="$(sdkmanager_bin "$SDK_DIR")" || { warn "sdkmanager not found — create an emulator in Android Studio > Device Manager."; return; }
  bindir="$(dirname "$sm")"
  avdm="$bindir/avdmanager"
  [ "$PLATFORM" = "windows" ] && avdm="$bindir/avdmanager.bat"

  abi="$(emulator_abi)"
  image="system-images;android-${EMULATOR_API};google_apis_playstore;${abi}"

  step "Installing emulator + Google Play image ($image)"
  yes 2>/dev/null | "$sm" --sdk_root="$SDK_DIR" --licenses >/dev/null 2>&1 || true
  if ! "$sm" --sdk_root="$SDK_DIR" "emulator" "$image" >/dev/null 2>&1; then
    warn "Couldn't install $image — create an emulator in Android Studio > Device Manager."
    return
  fi
  if [ ! -f "$avdm" ]; then
    warn "avdmanager not found — create the AVD in Android Studio > Device Manager."
    return
  fi

  if echo "no" | "$avdm" create avd -n "$EMULATOR_AVD_NAME" -k "$image" -d pixel_6 --force >/dev/null 2>&1; then
    ok "Created AVD '$EMULATOR_AVD_NAME' (Google Play, $abi)"
    EMULATOR_AVD_READY=1
  else
    warn "Couldn't create the AVD — create one in Android Studio > Device Manager."
    return
  fi

  cfg="$avd_home/$EMULATOR_AVD_NAME.avd/config.ini"
  if [ -f "$cfg" ]; then
    set_avd_prop "$cfg" "hw.ramSize" "2048"
    set_avd_prop "$cfg" "vm.heapSize" "256"
    set_avd_prop "$cfg" "disk.dataPartition.size" "2048M"
    ok "Tuned AVD to 2 GB RAM / 256 MB heap / 2 GB storage"
  fi
  [ "$PLATFORM" = "windows" ] && warn "On Windows, enable virtualization (WHPX) to boot the emulator."
}

# Mirrors the iOS script, where `tuist generate` opens Xcode at the end — here we
# open the project in Android Studio so Gradle can start syncing right away.
open_android_studio() {
  step "Opening Android Studio"
  android_studio_installed || { warn "Android Studio not found — open the project manually."; return; }
  case "$PLATFORM" in
    macos)
      open -a "Android Studio" "$PROJECT_ROOT" >/dev/null 2>&1 \
        && ok "Opening project in Android Studio" \
        || warn "Couldn't launch Android Studio — open the project manually." ;;
    linux)
      local bin=""
      for c in "/opt/android-studio/bin/studio.sh" "$HOME/android-studio/bin/studio.sh"; do
        [ -x "$c" ] && { bin="$c"; break; }
      done
      if [ -n "$bin" ]; then
        ( "$bin" "$PROJECT_ROOT" >/dev/null 2>&1 & ); ok "Opening project in Android Studio"
      elif has android-studio; then
        ( android-studio "$PROJECT_ROOT" >/dev/null 2>&1 & ); ok "Opening project in Android Studio (snap)"
      else
        warn "Couldn't find the Android Studio launcher — open the project manually."
      fi ;;
    windows)
      local exe; exe="$(to_unix "${LOCALAPPDATA:-}")/Programs/Android Studio/bin/studio64.exe"
      if [ -f "$exe" ]; then
        ( "$exe" "$PROJECT_ROOT" >/dev/null 2>&1 & ); ok "Opening project in Android Studio"
      else
        warn "Couldn't find studio64.exe — open the project manually."
      fi ;;
  esac
}

printf 'MapsWorkshop setup — Android (platform=%s)\n' "$PLATFORM"

ensure_brew
ensure_jdk
ensure_android_sdk
ensure_emulator
write_local_properties
persist_env
gradle_prewarm
open_android_studio

step "Done"
echo "Android setup complete."
echo "  - Let Gradle sync once Android Studio finishes loading the project."
echo "  - For terminal builds (./gradlew, adb), open a NEW terminal so JAVA_HOME/ANDROID_HOME load."
[ "$EMULATOR_AVD_READY" -eq 1 ] && echo "  - Run on Android: start an emulator from Android Studio > Device Manager."
