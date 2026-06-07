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
# Build number for Google's standalone command-line tools (sdkmanager).
# Bump this from https://developer.android.com/studio#command-line-tools-only.
CMDLINE_TOOLS_BUILD="14742923"
SDK_DIR=""
BREW_BIN=""
BREW_NEWLY_INSTALLED=0

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

# Find brew on PATH or in its standard install locations, and load its shellenv so
# it's usable for the rest of this run (a fresh install isn't on PATH yet). Sets
# BREW_BIN. Returns 1 if Homebrew can't be found.
locate_brew() {
  if has brew; then BREW_BIN="$(command -v brew)"; return 0; fi
  local b
  for b in /opt/homebrew/bin/brew /usr/local/bin/brew /home/linuxbrew/.linuxbrew/bin/brew; do
    [ -x "$b" ] && { BREW_BIN="$b"; eval "$("$b" shellenv)"; return 0; }
  done
  return 1
}

# Install Homebrew if it's missing. brew is the backbone of the macOS setup (JDK,
# Android Studio, Xcode tooling, Tuist), so we bootstrap it instead of just
# warning. Linux uses apt/snap and Windows uses winget, so this is macOS-only.
ensure_brew() {
  [ "$PLATFORM" = "macos" ] || return 0
  step "Homebrew"
  locate_brew && { ok "Homebrew present ($BREW_BIN)"; return 0; }

  has curl || { warn "curl is required to install Homebrew — see https://brew.sh"; return 1; }
  warn "Homebrew not found — installing it (you may be prompted once for your password)."
  if NONINTERACTIVE=1 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"; then
    if locate_brew; then
      BREW_NEWLY_INSTALLED=1
      ok "Homebrew installed ($BREW_BIN)"
    else
      warn "Homebrew installed but isn't on PATH — open a new terminal and re-run."
      return 1
    fi
  else
    warn "Homebrew install failed — install it from https://brew.sh, then re-run."
    return 1
  fi
}

# Major version of the JDK at a given home (e.g. 17), or nothing if it's not a JDK.
jdk_major_of() {
  [ -x "$1/bin/java" ] || return 1
  "$1/bin/java" -version 2>&1 | head -n1 | sed -E 's/.*version "([0-9]+).*/\1/'
}

# Print a usable JDK home, preferring one >= $REQUIRED_JDK_MAJOR. Knows where the
# system, registered, and *keg-only Homebrew* JDKs live — the Homebrew openjdk is
# never on PATH or registered with the OS, which is exactly why a bare `java` can
# report "unable to find a Java runtime". Returns 1 if no JDK is found at all.
resolve_java_home() {
  local NL list="" c home="" fallback="" maj p
  NL="$(printf '\nx')"; NL="${NL%x}"   # a lone newline (bash 3.2 safe)

  # An already-valid JAVA_HOME wins.
  [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && list="$JAVA_HOME$NL"

  case "$PLATFORM" in
    macos)
      # Registered JDKs (Corretto, Temurin .pkg, Zulu, …).
      if [ -x /usr/libexec/java_home ]; then
        c="$(/usr/libexec/java_home -v "$REQUIRED_JDK_MAJOR" 2>/dev/null)" && [ -n "$c" ] && list="$list$c$NL"
        c="$(/usr/libexec/java_home 2>/dev/null)"                         && [ -n "$c" ] && list="$list$c$NL"
      fi
      # Homebrew's openjdk: prefer the proper JDK bundle (has release/jmods) over
      # the bare prefix. `brew --prefix` prints a path even when uninstalled, so
      # the bin/java check below is what actually validates these.
      if has brew; then
        for f in "openjdk@$REQUIRED_JDK_MAJOR" openjdk; do
          p="$(brew --prefix "$f" 2>/dev/null)" || continue
          [ -n "$p" ] && list="$list$p/libexec/openjdk.jdk/Contents/Home$NL$p$NL"
        done
      fi
      ;;
    linux)
      for c in "/usr/lib/jvm/java-${REQUIRED_JDK_MAJOR}-openjdk-$(uname -m)" \
               "/usr/lib/jvm/java-${REQUIRED_JDK_MAJOR}-openjdk" /usr/lib/jvm/default-java; do
        [ -d "$c" ] && list="$list$c$NL"
      done
      ;;
  esac

  # Last resort: derive the home from whatever `java` resolves to on PATH.
  if has java; then
    c="$(command -v java)"
    command -v readlink >/dev/null 2>&1 && c="$(readlink -f "$c" 2>/dev/null || printf '%s' "$c")"
    list="$list$(dirname "$(dirname "$c")")$NL"
  fi

  # First candidate that is a JDK >= required wins; keep any valid JDK as fallback.
  while IFS= read -r c; do
    [ -n "$c" ] && [ -x "$c/bin/java" ] || continue
    maj="$(jdk_major_of "$c" 2>/dev/null || echo 0)"
    if [ "${maj:-0}" -ge "$REQUIRED_JDK_MAJOR" ] 2>/dev/null; then printf '%s' "$c"; return 0; fi
    [ -z "$fallback" ] && fallback="$c"
  done <<EOF
$list
EOF
  [ -n "$fallback" ] && { printf '%s' "$fallback"; return 0; }
  return 1
}

# macOS: register a keg-only JDK with the OS so `java -version` works in *every*
# terminal and app — not just inside this script. Homebrew's openjdk is keg-only:
# it's never on PATH and the system `/usr/bin/java` stub can't see it, so a fresh
# shell reports "Unable to locate a Java Runtime" even right after `brew install`.
# Symlinking the JDK bundle into ~/Library/Java/JavaVirtualMachines (no sudo —
# this is the per-user spot `/usr/libexec/java_home` already scans) fixes that.
register_macos_jdk() {
  [ "$PLATFORM" = "macos" ] || return 0
  local home="$1" maj bundle dir dest
  # Already visible to the OS Java launcher (any registered JDK)? Nothing to do.
  /usr/libexec/java_home -V 2>&1 | grep -qF "$home" && return 0
  # Only a proper bundle (…/X.jdk/Contents/Home) can be symlinked into place.
  case "$home" in *.jdk/Contents/Home) ;; *) return 0 ;; esac
  bundle="${home%/Contents/Home}"                 # …/openjdk.jdk
  maj="$(jdk_major_of "$home" 2>/dev/null || echo "$REQUIRED_JDK_MAJOR")"
  dir="$HOME/Library/Java/JavaVirtualMachines"
  dest="$dir/mapsworkshop-jdk${maj}.jdk"
  mkdir -p "$dir" 2>/dev/null || { warn "Couldn't create $dir — register the JDK in Android Studio."; return 0; }
  if ln -sfn "$bundle" "$dest" 2>/dev/null; then
    ok "Registered JDK with macOS: $dest -> $bundle"
    ok "\`java -version\` now works in any terminal."
  else
    warn "Couldn't symlink the JDK into $dir."
  fi
}

# Make $JAVA_HOME effective for the rest of this run (export + PATH), register it
# with the OS so plain \`java\` works everywhere, and persist it later via persist_env.
use_java_home() {
  local home="$1"
  register_macos_jdk "$home"
  export JAVA_HOME="$home"
  case ":$PATH:" in *":$home/bin:"*) ;; *) export PATH="$home/bin:$PATH" ;; esac
  ok "JDK $(jdk_major_of "$home") at $home"
  ok "JAVA_HOME=$home"
}

# Export the Android SDK location for the rest of this run so sdkmanager,
# avdmanager, the emulator, adb, and Gradle all agree on a single SDK root — and
# so the emulator install (which calls those Java tools) targets the right place.
use_android_home() {
  [ -n "${SDK_DIR:-}" ] || return 0
  export ANDROID_HOME="$SDK_DIR"
  export ANDROID_SDK_ROOT="$SDK_DIR"   # legacy name some tools still read
  local d
  for d in "$SDK_DIR/platform-tools" "$SDK_DIR/emulator" "$SDK_DIR/cmdline-tools/latest/bin"; do
    case ":$PATH:" in *":$d:"*) ;; *) export PATH="$d:$PATH" ;; esac
  done
  ok "ANDROID_HOME=$SDK_DIR"
}

# Persist the dev-env vars we configured (JAVA_HOME, ANDROID_HOME, …) so new
# terminals — and, on macOS, GUI-launched Android Studio — find the JDK and
# Android SDK. Writes one idempotent managed block to the shell profile (replaced,
# never duplicated). On macOS also publishes to launchd so apps started afterwards
# (Android Studio via `open`) inherit the vars; GUI apps don't read shell profiles.
persist_env() {
  step "Persisting environment (JAVA_HOME / ANDROID_HOME)"

  if [ "$PLATFORM" = "macos" ] && has launchctl; then
    [ -n "${JAVA_HOME:-}" ]        && launchctl setenv JAVA_HOME        "$JAVA_HOME"        2>/dev/null || true
    [ -n "${ANDROID_HOME:-}" ]     && launchctl setenv ANDROID_HOME     "$ANDROID_HOME"     2>/dev/null || true
    [ -n "${ANDROID_SDK_ROOT:-}" ] && launchctl setenv ANDROID_SDK_ROOT "$ANDROID_SDK_ROOT" 2>/dev/null || true
    ok "Published env to launchd (GUI apps launched from now on inherit it)"
  fi

  if [ "$PLATFORM" = "windows" ]; then
    if has setx; then
      [ -n "${JAVA_HOME:-}" ]    && setx JAVA_HOME "$JAVA_HOME" >/dev/null 2>&1
      [ -n "${ANDROID_HOME:-}" ] && { setx ANDROID_HOME "$ANDROID_HOME" >/dev/null 2>&1; setx ANDROID_SDK_ROOT "$ANDROID_HOME" >/dev/null 2>&1; }
      ok "Persisted env vars for new terminals (Windows user environment)"
    else
      warn "Set JAVA_HOME and ANDROID_HOME in your user environment variables."
    fi
    return
  fi

  local rc tmp begin="# >>> MapsWorkshop environment >>>" end="# <<< MapsWorkshop environment <<<"
  case "$(basename "${SHELL:-bash}")" in
    zsh)  rc="$HOME/.zshrc" ;;
    bash) rc="$HOME/.bashrc"; [ -f "$rc" ] || rc="$HOME/.bash_profile" ;;
    *)    rc="$HOME/.profile" ;;
  esac
  touch "$rc" 2>/dev/null || { warn "Couldn't write $rc — set JAVA_HOME/ANDROID_HOME there yourself."; return; }

  tmp="$(mktemp)" || { warn "Couldn't persist env — set JAVA_HOME/ANDROID_HOME in $rc yourself."; return; }
  awk -v b="$begin" -v e="$end" '$0==b{skip=1} !skip{print} $0==e{skip=0}' "$rc" > "$tmp" && mv "$tmp" "$rc" || rm -f "$tmp"
  {
    printf '%s\n' "$begin"
    # If we just installed Homebrew, its installer doesn't touch the profile —
    # add its shellenv so new terminals (and the exports below) find brew.
    [ "$BREW_NEWLY_INSTALLED" -eq 1 ] && [ -n "$BREW_BIN" ] \
      && printf 'eval "$(%s shellenv)"\n' "$BREW_BIN"
    if [ -n "${JAVA_HOME:-}" ]; then
      printf 'export JAVA_HOME="%s"\n' "$JAVA_HOME"
      printf 'export PATH="$JAVA_HOME/bin:$PATH"\n'
    fi
    if [ -n "${ANDROID_HOME:-}" ]; then
      printf 'export ANDROID_HOME="%s"\n' "$ANDROID_HOME"
      printf 'export ANDROID_SDK_ROOT="%s"\n' "$ANDROID_HOME"
      printf 'export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"\n'
    fi
    printf '%s\n' "$end"
  } >> "$rc"
  ok "Persisted JAVA_HOME / ANDROID_HOME to $rc (open a new terminal to pick it up)"
}

ensure_jdk() {
  step "Java (JDK $REQUIRED_JDK_MAJOR+)"

  local home
  if home="$(resolve_java_home)" && [ "$(jdk_major_of "$home" 2>/dev/null || echo 0)" -ge "$REQUIRED_JDK_MAJOR" ] 2>/dev/null; then
    use_java_home "$home"; return
  fi

  case "$PLATFORM" in
    macos)   has brew && brew install "openjdk@$REQUIRED_JDK_MAJOR" || warn "Install JDK $REQUIRED_JDK_MAJOR from https://adoptium.net/" ;;
    linux)   has apt-get && sudo apt-get update -qq && sudo apt-get install -y "openjdk-${REQUIRED_JDK_MAJOR}-jdk" || warn "Install JDK $REQUIRED_JDK_MAJOR via your package manager" ;;
    windows) has winget && winget install --id "EclipseAdoptium.Temurin.${REQUIRED_JDK_MAJOR}.JDK" --silent --accept-source-agreements --accept-package-agreements || warn "Install Temurin JDK $REQUIRED_JDK_MAJOR from https://adoptium.net/" ;;
  esac

  # Resolve again now that it's installed — Homebrew's openjdk is keg-only, so it
  # won't appear on PATH, but resolve_java_home knows to look in the brew prefix.
  if home="$(resolve_java_home)" && [ "$(jdk_major_of "$home" 2>/dev/null || echo 0)" -ge "$REQUIRED_JDK_MAJOR" ] 2>/dev/null; then
    use_java_home "$home"
  else
    warn "JDK $REQUIRED_JDK_MAJOR still not found — install it, then re-run."
    warn "If you just installed it, open a new terminal (PATH update) and re-run."
  fi
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
    ok "Android Studio present."
    return
  fi
  step "Installing Android Studio"
  case "$PLATFORM" in
    macos)   has brew  && brew install --cask android-studio && ok "Android Studio installed" || warn "Install Android Studio: https://developer.android.com/studio" ;;
    linux)   has snap  && sudo snap install android-studio --classic && ok "Android Studio installed" || warn "Install Android Studio: https://developer.android.com/studio" ;;
    windows) has winget && winget install --id Google.AndroidStudio --silent --accept-source-agreements --accept-package-agreements && ok "Android Studio installed" || warn "Install Android Studio: https://developer.android.com/studio" ;;
  esac
}

cmdline_tools_url() {
  local os
  case "$PLATFORM" in
    macos)   os="mac"   ;;
    linux)   os="linux" ;;
    windows) os="win"   ;;
  esac
  printf 'https://dl.google.com/android/repository/commandlinetools-%s-%s_latest.zip' \
    "$os" "$CMDLINE_TOOLS_BUILD"
}

# Download Google's standalone command-line tools into $SDK_DIR and lay them out
# as cmdline-tools/latest — the location sdkmanager expects (and the same one
# Android Studio uses). This is what lets us install the SDK headlessly instead
# of deferring to Studio's first-launch wizard.
bootstrap_cmdline_tools() {
  has curl  || { warn "curl is required to download the Android command-line tools."; return 1; }
  has unzip || { warn "unzip is required to extract the Android command-line tools."; return 1; }

  local url tmp zip
  url="$(cmdline_tools_url)"
  tmp="$(mktemp -d)"
  zip="$tmp/cmdline-tools.zip"

  step "Downloading Android command-line tools"
  if ! curl -fsSL "$url" -o "$zip"; then
    warn "Download failed: $url"; rm -rf "$tmp"; return 1
  fi
  if ! unzip -q "$zip" -d "$tmp"; then
    warn "Couldn't unzip the command-line tools."; rm -rf "$tmp"; return 1
  fi

  mkdir -p "$SDK_DIR/cmdline-tools"
  rm -rf "$SDK_DIR/cmdline-tools/latest"
  if ! mv "$tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"; then
    warn "Couldn't install the command-line tools into $SDK_DIR."; rm -rf "$tmp"; return 1
  fi
  rm -rf "$tmp"
  ok "Command-line tools at $SDK_DIR/cmdline-tools/latest"
}

ensure_android_sdk() {
  step "Android SDK"

  # Install the IDE if it's missing — it's still expected for day-to-day work.
  ensure_android_studio

  # Use an existing SDK if we can find one; otherwise target the platform default.
  local fresh=0
  if SDK_DIR="$(resolve_sdk_dir)"; then
    ok "SDK at $SDK_DIR"
  else
    SDK_DIR="$(to_unix "$DEFAULT_SDK")"
    fresh=1
    warn "No Android SDK found — bootstrapping one at $SDK_DIR"
  fi
  mkdir -p "$SDK_DIR"

  # Ensure sdkmanager exists; bootstrap the command-line tools if it doesn't.
  local sm
  if ! sm="$(sdkmanager_bin "$SDK_DIR")"; then
    if bootstrap_cmdline_tools && sm="$(sdkmanager_bin "$SDK_DIR")"; then
      ok "Installed command-line tools (sdkmanager)"
    else
      warn "Couldn't obtain sdkmanager — open Android Studio once to finish SDK setup, then re-run."
      [ "$fresh" -eq 1 ] && SDK_DIR=""
      return
    fi
  fi

  yes 2>/dev/null | "$sm" --sdk_root="$SDK_DIR" --licenses >/dev/null 2>&1 || true
  "$sm" --sdk_root="$SDK_DIR" "platform-tools" "$ANDROID_PLATFORM" >/dev/null 2>&1 \
    && ok "platform-tools + $ANDROID_PLATFORM ready" \
    || warn "Couldn't install platform-tools/$ANDROID_PLATFORM — install via Android Studio > SDK Manager."

  # Export ANDROID_HOME/PATH now so the emulator install and Gradle find this SDK.
  use_android_home
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
