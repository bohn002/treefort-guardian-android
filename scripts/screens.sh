#!/usr/bin/env bash
# Capture docs/screens/android/<id>.png from an Android EMULATOR (never a physical device).
# Usage: android/scripts/screens.sh [id ...]   (default: every id below)
# Needs a running emulator (e.g. `emulator -avd medium_phone`); set EMU=emulator-5554 to pick one.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="../docs/screens/android"
PKG=com.treefort.guardian

EMU="${EMU:-$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')}"
[[ "$EMU" == emulator-* ]] || { echo "no running emulator (start one: emulator -avd medium_phone)" >&2; exit 1; }
A=(adb -s "$EMU")

./gradlew -q assembleDebug
"${A[@]}" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
"${A[@]}" shell pm grant $PKG android.permission.RECORD_AUDIO
"${A[@]}" shell pm grant $PKG android.permission.POST_NOTIFICATIONS
"${A[@]}" shell cmd uimode night no
"${A[@]}" shell settings put global sysui_demo_allowed 1
for c in "enter" "clock -e hhmm 0941" "battery -e level 100 -e plugged false" "network -e wifi show -e level 4" "notifications -e visible false"; do
  "${A[@]}" shell am broadcast -a com.android.systemui.demo -e command ${c%% *} ${c#* } >/dev/null 2>&1 || true
done

# id[:scroll in px]. Scroll lands the card the web shot is scrolled to near the top.
IDS=(
auth-verifying auth-sign-in auth-sign-in-error auth-sign-in-provider-not-set-up auth-create-account
auth-create-account-error auth-check-email auth-email-code auth-email-code-verify auth-forgot-password
auth-forgot-password-sent auth-reset-password auth-reset-password-error auth-provider-error auth-certify-18
auth-guardian-lock station-home station-quiet-hours-expanded station-device-menu station-phones:900 station-no-phones
station-trip-active station-caller-home modal-pair-phone modal-add-rule modal-remove-phone friends-list
friends-empty modal-add-friend-invite modal-add-friend-phrase modal-add-friend-number modal-edit-friend
logbook-calls logbook-calls-empty logbook-safety logbook-safety-empty settings-top settings-household
settings-household-invite settings-household-invite-form settings-household-accept-invite
settings-locations:800 settings-travel settings-travel-plan-trip settings-guardian-lock:1900
settings-subscription:2500 settings-plain-text:3100 settings-health:3700 settings-about-delete:9000
settings-modal-add-address settings-modal-release-phone settings-modal-guardian-lock-pin
settings-modal-delete-account support-home support-faq-open
)
if [[ $# -gt 0 ]]; then
  want=(); for w in "$@"; do for e in "${IDS[@]}"; do [[ "${e%%:*}" == "$w" ]] && want+=("$e"); done; done
  IDS=("${want[@]}")
fi

mkdir -p "$OUT"
for e in "${IDS[@]}"; do
  id="${e%%:*}"; scroll=0; [[ "$e" == *:* ]] && scroll="${e#*:}"
  "${A[@]}" shell am force-stop $PKG
  "${A[@]}" shell am start -W -n $PKG/.MainActivity --es screen "$id" --ei scroll "$scroll" >/dev/null
  sleep 2.5
  "${A[@]}" exec-out screencap -p > "$OUT/$id.png"
  echo "captured $id"
done
