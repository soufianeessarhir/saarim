# Commands run inside Android (Waydroid container or USB phone) by the Makefile.
# The Makefile pipes this file into `$(DSH) sh -s -- <group> [args]`, so the shell
# reads it from stdin -- nothing here goes through `waydroid`'s own argument parser,
# which would eat a `-c`. Args after `--` reach us as $1.. and $@.
set -u

case "$1" in
owner)
	owner=$(dumpsys device_policy 2>/dev/null | grep -m1 "^  Device Owner:" || true)
	if [ -n "$owner" ]; then
		echo "!! a device owner is already registered:"; echo "$owner"
		echo "   wipe the container with: make waydroid-reset"; exit 1
	fi
	users=$(pm list users | grep -c UserInfo || true)
	if [ "${users:-0}" -ne 1 ]; then echo "!! more than one user:"; pm list users; exit 1; fi
	accounts=$(dumpsys account 2>/dev/null | grep -c "Account {" || true)
	if [ "${accounts:-0}" -ne 0 ]; then
		echo "!! an account is signed in; remove it first (a GAPPS image with a"
		echo "   Google account can never accept a device owner)"; exit 1
	fi
	dpm set-device-owner "$2"
	dumpsys device_policy 2>/dev/null | grep -A2 "^  Device Owner:"
	;;
privileges)
	settings put secure enabled_accessibility_services "$2"
	settings put secure accessibility_enabled 1
	pm grant "$3" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null || true
	echo "== accessibility service =="; settings get secure enabled_accessibility_services
	echo "== permissions granted =="
	dumpsys package "$3" | grep -E "android.permission.WRITE_SECURE_SETTINGS: granted=true"
	;;
status)
	echo "== device owner =="; dumpsys device_policy 2>/dev/null | grep -A2 "^  Device Owner:"
	echo "== accessibility service =="; settings get secure enabled_accessibility_services
	echo "== app =="
	dumpsys package "$2" 2>/dev/null \
		| grep -E "versionName|WRITE_SECURE_SETTINGS: granted"
	;;
*)
	echo "usage: device-cmds.sh {owner|privileges|status} [args]" >&2; exit 2
	;;
esac
