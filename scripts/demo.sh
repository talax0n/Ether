#!/usr/bin/env bash
# One-command demo: local Supabase with seed data, the `ether` emulator, and the debug app installed and open.
# Safe to re-run. HEADLESS=1 boots the emulator without a window.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "error: $*" >&2; exit 1; }

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
SUPABASE="${SUPABASE:-$(command -v supabase || echo "$HOME/.local/bin/supabase")}"
AVD=ether
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$PATH"

docker info >/dev/null 2>&1 || fail "Docker is not running. Start Docker Desktop and try again."
[ -x "$SUPABASE" ] || fail "Supabase CLI not found. Install it (https://supabase.com/docs/guides/cli) or set SUPABASE=/path/to/supabase."
[ -d "$SDK/platform-tools" ] || fail "Android SDK not found at $SDK. Set ANDROID_HOME."
command -v adb >/dev/null || fail "adb missing from $SDK/platform-tools."
command -v emulator >/dev/null || fail "Android emulator missing from $SDK/emulator."
java -version 2>&1 | grep -q '"17' || fail "JDK 17 is required (java -version)."

"$SUPABASE" status >/dev/null 2>&1 || "$SUPABASE" start
"$SUPABASE" db reset

if [ ! -f local.properties ]; then
  anon=$("$SUPABASE" status -o env 2>/dev/null | sed -n 's/^ANON_KEY="\(.*\)"$/\1/p')
  [ -n "$anon" ] || fail "Could not read ANON_KEY from 'supabase status -o env'."
  cat > local.properties <<PROPS
sdk.dir=$SDK
SUPABASE_URL=http://10.0.2.2:54321
SUPABASE_ANON_KEY=$anon
USE_FAKE_BACKEND=false
PROPS
  echo "Wrote local.properties"
fi

if ! adb devices | awk 'NR > 1 && $2 == "device"' | grep -q .; then
  if ! emulator -list-avds | grep -qx "$AVD"; then
    image=$(cd "$SDK" && ls -d system-images/android-3*/google_apis*/arm64-v8a system-images/android-3*/google_apis*/x86_64 2>/dev/null | sort -r | head -1)
    [ -n "$image" ] || fail "No Android 30+ Google APIs system image in $SDK/system-images. Install one with sdkmanager."
    AVDMANAGER="${AVDMANAGER:-$(command -v avdmanager || echo "$SDK/cmdline-tools/latest/bin/avdmanager")}"
    [ -x "$AVDMANAGER" ] || fail "avdmanager not found. Install the Android cmdline-tools."
    echo "Creating AVD '$AVD' from $image"
    echo no | "$AVDMANAGER" create avd -n "$AVD" -k "${image//\//;}" -d pixel_7 --sdk_root "$SDK" >/dev/null
  fi
  echo "Booting emulator '$AVD'"
  window=()
  [ "${HEADLESS:-0}" = 1 ] && window=(-no-window -no-audio)
  nohup emulator -avd "$AVD" "${window[@]}" -no-snapshot-save >/tmp/ether-emulator.log 2>&1 &
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
fi

./gradlew -q :app:installDebug
adb shell am start -n dev.ether.app/.MainActivity >/dev/null

mailpit=$("$SUPABASE" status -o env 2>/dev/null | sed -n 's/^MAILPIT_URL="\(.*\)"$/\1/p')
cat <<INFO

Ether is running on the emulator.

Demo accounts (email OTP):
  demo1@student.example.ac.id ... demo8@student.example.ac.id
  Any new @student.example.ac.id address also works and starts at profile setup.
Sign-in codes arrive in Mailpit: ${mailpit:-http://127.0.0.1:54324}

Seed: Campus Hackathon 2026 has solos demo1-6 and team Byte Club (demo7 + demo8).
      Data Challenge 2026 has ended; its team Night Owls (demo1-3) can endorse and report a result.
INFO
