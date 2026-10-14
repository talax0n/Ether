#!/usr/bin/env bash
# Resets the local database, installs the debug app, and runs every Maestro flow in .maestro/.
set -euo pipefail
cd "$(dirname "$0")/.."

SUPABASE="${SUPABASE:-$(command -v supabase || echo "$HOME/.local/bin/supabase")}"
MAESTRO="${MAESTRO:-$(command -v maestro || echo "$HOME/.maestro/bin/maestro")}"
export PATH="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools:$PATH"

adb get-state >/dev/null 2>&1 || { echo "No Android device connected. Run scripts/demo.sh or start the emulator." >&2; exit 1; }

"$SUPABASE" db reset
scripts/e2e-setup.sh
./gradlew -q :app:installDebug
"$MAESTRO" test .maestro/
