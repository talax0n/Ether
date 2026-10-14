#!/usr/bin/env bash
# Resets the local database, installs the debug app, and runs every Maestro flow in .maestro/.
set -euo pipefail
cd "$(dirname "$0")/.."

SUPABASE="${SUPABASE:-$(command -v supabase || echo "$HOME/.local/bin/supabase")}"
MAESTRO="${MAESTRO:-$(command -v maestro || echo "$HOME/.maestro/bin/maestro")}"
export PATH="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools:$PATH"

. scripts/_local.sh
require_local_backend
use_emulator || { echo "No emulator running. Run scripts/demo.sh or start the emulator." >&2; exit 1; }

"$SUPABASE" db reset
scripts/e2e-setup.sh
./gradlew -q :app:installDebug
"$MAESTRO" --device "$ANDROID_SERIAL" test .maestro/
