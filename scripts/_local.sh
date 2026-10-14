# Sourced by demo.sh and e2e.sh. Both reset data and clear app state, so they must only ever touch
# the local Supabase stack and an emulator, never a remote project or a plugged-in phone.

require_local_backend() {
  local url
  url=$(sed -n 's/^SUPABASE_URL=//p' local.properties 2>/dev/null | tr -d '[:space:]')
  case "${url:-http://10.0.2.2:54321}" in
    http://10.0.2.2:*|http://127.0.0.1:*|http://localhost:*) ;;
    *) echo "error: local.properties points SUPABASE_URL at $url. These scripts only run against local Supabase." >&2; exit 1 ;;
  esac
}

# Exports ANDROID_SERIAL so adb, Gradle installs, and Maestro all target the emulator only.
use_emulator() {
  ANDROID_SERIAL=$(adb devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ {print $1; exit}')
  export ANDROID_SERIAL
  [ -n "$ANDROID_SERIAL" ]
}
