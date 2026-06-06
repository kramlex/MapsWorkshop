#!/usr/bin/env bash
# Shared helpers for the MapsWorkshop setup scripts.
#
# This file is *sourced* by setup-android.sh and setup-ios.sh — it is not meant
# to be run on its own. It holds the steps both platforms need: the JDK, the
# Android SDK (required even for iOS work, because the KMP project always
# configures an Android target), local.properties, and the Gradle warm-up.

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$COMMON_DIR/.." && pwd)"
LOCAL_PROPS="$PROJECT_ROOT/local.properties"
GIST_RAW_URL="https://gist.githubusercontent.com/tamimattafi/357d1f878d91b0a224bedf82c211fc5c/raw"
REQUIRED_JDK_MAJOR=17
ANDROID_PLATFORM="platforms;android-36"
SDK_DIR=""

step() { printf '\n==> %s\n' "$*"; }
ok()   { printf '  + %s\n' "$*"; }
warn() { printf '  ! %s\n' "$*"; }
has()  { command -v "$1" >/dev/null 2>&1; }
to_unix() { printf '%s' "$1" | tr '\\' '/'; }

case "$(uname -s)" in
  Darwin)               PLATFORM="macos" ;;
  Linux)                PLATFORM="linux" ;;
  MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
  *) printf 'Unsupported OS: %s\n' "$(uname -s)" >&2; exit 1 ;;
esac

case "$PLATFORM" in
  macos)   DEFAULT_SDK="$HOME/Library/Android/sdk" ;;
  linux)   DEFAULT_SDK="$HOME/Android/Sdk" ;;
  windows) DEFAULT_SDK="${LOCALAPPDATA:-}/Android/Sdk" ;;
esac

java_major() { java -version 2>&1 | head -n1 | sed -E 's/.*version "([0-9]+).*/\1/'; }

ensure_jdk() {
  step "Java (JDK $REQUIRED_JDK_MAJOR+)"
  if has java && [ "$(java_major 2>/dev/null || echo 0)" -ge "$REQUIRED_JDK_MAJOR" ] 2>/dev/null; then
    ok "JDK $(java_major) present"; return
  fi
  case "$PLATFORM" in
    macos)   has brew && brew install "openjdk@$REQUIRED_JDK_MAJOR" || warn "Install JDK $REQUIRED_JDK_MAJOR from https://adoptium.net/" ;;
    linux)   has apt-get && sudo apt-get update -qq && sudo apt-get install -y "openjdk-${REQUIRED_JDK_MAJOR}-jdk" || warn "Install JDK $REQUIRED_JDK_MAJOR via your package manager" ;;
    windows) has winget && winget install --id "EclipseAdoptium.Temurin.${REQUIRED_JDK_MAJOR}.JDK" --silent --accept-source-agreements --accept-package-agreements || warn "Install Temurin JDK $REQUIRED_JDK_MAJOR from https://adoptium.net/" ;;
  esac
  warn "If 'java' is still missing, open a new terminal (PATH update) and re-run."
}

resolve_sdk_dir() {
  for c in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$DEFAULT_SDK" \
           "/opt/homebrew/share/android-commandlinetools" "/usr/local/share/android-commandlinetools"; do
    [ -n "$c" ] || continue
    c="$(to_unix "$c")"
    [ -d "$c" ] && { printf '%s' "$c"; return 0; }
  done
  return 1
}

sdkmanager_bin() {
  local sdk="$1" exe="sdkmanager"
  [ "$PLATFORM" = "windows" ] && exe="sdkmanager.bat"
  for p in "$sdk/cmdline-tools/latest/bin/$exe" "$sdk/cmdline-tools/bin/$exe" "$sdk/tools/bin/$exe"; do
    [ -f "$p" ] && { printf '%s' "$p"; return 0; }
  done
  has "$exe" && { command -v "$exe"; return 0; }
  return 1
}

android_studio_installed() {
  case "$PLATFORM" in
    macos)
      [ -d "/Applications/Android Studio.app" ] && return 0
      [ -d "$HOME/Applications/Android Studio.app" ] && return 0
      ;;
    linux)
      [ -d "/opt/android-studio" ] && return 0
      [ -d "$HOME/android-studio" ] && return 0
      has snap && snap list android-studio >/dev/null 2>&1 && return 0
      ;;
    windows)
      [ -d "$(to_unix "${LOCALAPPDATA:-}")/Programs/Android Studio" ] && return 0
      ;;
  esac
  return 1
}

ensure_android_studio() {
  if android_studio_installed; then
    ok "Android Studio present (no SDK yet — run its first-launch wizard)."
    return
  fi
  step "Installing Android Studio"
  case "$PLATFORM" in
    macos)   has brew  && brew install --cask android-studio && ok "Android Studio installed" || warn "Install Android Studio: https://developer.android.com/studio" ;;
    linux)   has snap  && sudo snap install android-studio --classic && ok "Android Studio installed" || warn "Install Android Studio: https://developer.android.com/studio" ;;
    windows) has winget && winget install --id Google.AndroidStudio --silent --accept-source-agreements --accept-package-agreements && ok "Android Studio installed" || warn "Install Android Studio: https://developer.android.com/studio" ;;
  esac
}

ensure_android_sdk() {
  step "Android SDK"
  if SDK_DIR="$(resolve_sdk_dir)"; then
    ok "SDK at $SDK_DIR"
  else
    SDK_DIR=""
    ensure_android_studio
    warn "Open Android Studio once to finish SDK setup, then re-run this script."
    return
  fi
  local sm
  if sm="$(sdkmanager_bin "$SDK_DIR")"; then
    yes 2>/dev/null | "$sm" --licenses >/dev/null 2>&1 || true
    "$sm" "platform-tools" "$ANDROID_PLATFORM" >/dev/null 2>&1 \
      && ok "platform-tools + $ANDROID_PLATFORM ready" \
      || warn "Install platform-tools and $ANDROID_PLATFORM via Android Studio > SDK Manager."
  else
    warn "sdkmanager not found — install components via Android Studio > SDK Manager."
  fi
}

prop_from() {
  [ -f "$1" ] || return 0
  grep -E "^[[:space:]]*$2[[:space:]]*=" "$1" 2>/dev/null | tail -n1 | tr -d '\r' \
    | sed -E "s/^[[:space:]]*$2[[:space:]]*=[[:space:]]*//; s/[[:space:]]+$//" || true
}

write_local_properties() {
  step "local.properties"
  local gist="" key val
  local mapkitToken openAiApiKey openAiModel openAiBaseUrl

  if has curl; then
    gist="$(mktemp)"
    curl -fsSL "$GIST_RAW_URL" -o "$gist" 2>/dev/null && [ -s "$gist" ] \
      && ok "Fetched keys from Gist" \
      || { warn "Couldn't fetch the Gist — keeping existing keys."; gist=""; }
  else
    warn "curl not found — keeping existing keys."
  fi

  for key in mapkitToken openAiApiKey openAiModel openAiBaseUrl; do
    val=""
    [ -n "$gist" ] && val="$(prop_from "$gist" "$key")"
    [ -z "$val" ] && val="$(prop_from "$LOCAL_PROPS" "$key")"
    eval "$key=\$val"
  done
  [ -n "$gist" ] && rm -f "$gist"
  [ -z "$openAiModel" ]   && openAiModel="deepseek-v4-flash"
  [ -z "$openAiBaseUrl" ] && openAiBaseUrl="https://api.deepseek.com/chat/completions"

  {
    echo "# Generated by scripts/setup-android.sh / scripts/setup-ios.sh"
    [ -n "$SDK_DIR" ] && echo "sdk.dir=$SDK_DIR"
    echo "mapkitToken=$mapkitToken"
    echo "openAiApiKey=$openAiApiKey"
    echo "openAiModel=$openAiModel"
    echo "openAiBaseUrl=$openAiBaseUrl"
  } > "$LOCAL_PROPS"

  ok "Wrote $LOCAL_PROPS"
  [ -z "$mapkitToken" ]  && warn "mapkitToken is empty — add it from the workshop Gist."
  [ -z "$openAiApiKey" ] && warn "openAiApiKey is empty — add it from the workshop Gist."
}

gradle_prewarm() {
  step "Pre-warming Gradle"
  ( cd "$PROJECT_ROOT" && ./gradlew --no-daemon -q help >/dev/null ) \
    && ok "Gradle warm-up done" \
    || warn "Gradle warm-up failed — the IDE sync will still work, just colder."
}
