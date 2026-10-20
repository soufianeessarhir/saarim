#!/usr/bin/env bash
# Watches for an Android device (USB, or adb connect over Wi-Fi) and installs Sarim.
# Usage: tools/install-on-device.sh
set -u

APK="$(cd "$(dirname "$0")/.." && pwd)/app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }

echo "Watching for a device. Plug in the phone, or run 'adb connect <ip>:5555' in another terminal."
echo "APK: $APK"
echo

install_to() {
  local serial="$1"
  echo ">>> Device found: $serial — installing..."
  adb -s "$serial" wait-for-device
  adb -s "$serial" install -r "$APK" && {
    echo ">>> Installed OK on $serial"
    adb -s "$serial" shell monkey -p com.sarim.app -c android.intent.category.LAUNCHER 1 2>/dev/null
    echo ">>> Launched com.sarim.app"
    return 0
  }
  echo ">>> Install FAILED on $serial"
  return 1
}

while true; do
  # Any device in 'device' state that is not the Waydroid container or an emulator.
  while read -r serial state; do
    [ "$state" = "device" ] || continue
    case "$serial" in 192.168.240.*|*waydroid*|*emulator*) continue ;; esac
    install_to "$serial" && exit 0
  done < <(adb devices | tail -n +2)

  sleep 2
done
