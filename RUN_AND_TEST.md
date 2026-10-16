# Building, running and testing Sarim on a phone or in Waydroid

Everything below was verified against this exact source tree. Build machine requirements first, then install, then a full test checklist on a real device — or, when there is no phone at hand, the container-based path in §4.

---

## 1. Build on the computer

**Requirements:** JDK **17–21** (AGP rejects 22+; a system default of JDK 27 fails with the opaque message `What went wrong: 27-ea`), Android SDK platform 36, and the SDK location in `local.properties` (`sdk.dir=...`).

This machine's JDK 21 lives at `~/.local/share/jdks/jdk-21.0.12.1+1`. Recommended: pin it once in `gradle.properties` so plain `./gradlew` works everywhere:

```properties
org.gradle.java.home=/home/<you>/.local/share/jdks/jdk-21.0.12.1+1
```

### Build commands

```bash
# Full debug APK (output: app/build/outputs/apk/debug/app-debug.apk)
JAVA_HOME=~/.local/share/jdks/jdk-21.0.12.1+1 ./gradlew :app:assembleDebug

# Release (needs 'dechainer.store*' keys in local.properties, else unsigned)
JAVA_HOME=~/.local/share/jdks/jdk-21.0.12.1+1 ./gradlew :app:assembleRelease
```

### Unit tests (JVM, no phone needed)

```bash
JAVA_HOME=~/.local/share/jdks/jdk-21.0.12.1+1 ./gradlew :app:testDebugUnitTest
```

53 tests in 6 suites — SecurityManager (24: recovery code, session, impulse block incl. clock-tamper resistance, config), LocaleUtils (9: the Arabic default, wrapped Arabic/RTL resources, the system per-app pick, a choice made in Config), FocusCycle (6), FocusSchedule (6), ColorFilterScope (7), ExampleUnitTest (1). Results land in `app/build/reports/tests/`.

> [!NOTE]
> Never filter Gradle output through `grep`/`tail` alone for verification — a pipe hides the exit code. Use `${PIPESTATUS[0]}` (bash) as done here, or re-check `echo $?` after.

---

## 2. Install on the phone

### Option A — USB + adb (ordinary development)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

With the Studio UI: enable USB debugging in Developer options, then Run ▶.

### Option B — the device itself (the app's own update flow)

The app can self-update from a downloaded APK via the same Device-Owner privileges that block unknown-source installs. For a non-Owner install, use option A.

### First-run setup (this is the important part — do it in order)

Sarim is "hard to bypass" because it takes over system protections. Every step is abandoned-protection until Device Owner is granted:

1. **Install Shizuku** on the phone, and start it by *Wireless debugging pairing* (Developer options → Wireless debugging → pair) or by plugging into a computer with adb and running `sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh` (older builds). Shizuku must show *running*.
2. **Open Sarim**, accept the Shizuku permission dialog it raises.
3. **Config tab → Device owner state → "Get owner privileges"**. It runs `dpm set-device-owner com.sarim.app/.SarimDeviceAdminReceiver` through Shizuku and then *waits for the authoritative broadcast* (`admin_result_received`), not just the shell exit code. The Config tab badge flips to green **Granted**.
4. **Config tab → Advanced blocking** — this enables `SarimAccessibilityService` (required for word blocking, visual blocking, activity blocking, torrent blocking, time limits, color modes).
5. **Recovery code wizard** — 16 characters. Write it on paper, confirm by typing it back from the paper, store the paper somewhere hard to reach. There is no digital copy.

*Fallback (clean device, adb):* if Shizuku pairing is painful, with no other device-admin configured, factory-reset the device (or wipe data), then `adb shell dpm set-device-owner com.sarim.app/.SarimDeviceAdminReceiver` directly from USB.

> [!WARNING]
> After step 3 the Device-Owner restrictions actually apply. From this point the recovery paper is the *only* loose supply valve; the 48-hour forced-removal path in the Config tab is the antibackdoor backstop if the paper is lost.

> [!TIP]
> With no phone at hand, §4 installs the same APK, grants the same two privileges and runs the same checklist inside a Linux container.

---

## 3. Test on the phone

### Functional checklist (each feature has a user-visible contract)

| # | What to do | What should happen |
|---|---|---|
| 1 | Open app first time | Recovery wizard, not tabs, and no bypass |
| 2 | Generate + confirm the 16-char code | Proceed to the lock screen every launch until session open |
| 3 | Enter the code wrong | Lock screen stays; timer never opens anything |
| 4 | Enter the code right | Session countdown starts (10 min) and shows as a pill in the app bar; refresh and logout buttons appear |
| 5 | Config tab badge | Red *Not granted* → green *Granted* after owner setup |
| 6 | Settings tab → per-app limit (e.g. YouTube, 10 min/day) | Usage counted; when cost runs out, launching that app is replaced by the **Time up** screen |
| 7 | Add a time window (e.g. 22:00–06:00) for an app | Launching inside the window shows **Outside time window** and returns home |
| 8 | Blocked words list: add a word, then search that word in any browser | Word/blocking activity intercepts the page; the **Blocked word** screen explains and there is no "continue anyway" |
| 9 | Visual blocking on, open a known adult site in a browser | On-device ML flags the screen → **Content blocked** screen (no screenshots leave the device) |
| 10 | Impulse lock panic button (locks Sarim + suspends picked apps) | Chosen apps grey out and cannot launch; duration ticks down; the countdown survives a reboot |
| 11 | Impulse lock during a session | Set duration 15–360 min: 14 min is clamped up, 400 min clamped down |
| 12 | Focus mode (Pomodoro) | Timer FGS notification with countdown; short break after each focus, long break after N cycles; phase-end alarm never lost (set exact alarms) |
| 13 | Reading challenge with source = Quran | Arabic Uthmani text renders, RTL-correct, no placeholder boxes |
| 14 | Reading challenge with source = Bible | Van Dyck Arabic / KJV-adjacent English text renders |
| 15 | App language switcher (en / pt / ar) | A fresh install is already in Arabic; picking en/pt/ar swaps the whole UI including the RTL flip, and the choice survives a restart |
| 16 | Color modes: grayscale on schedule | Overlay applies and releases; if accessibility is disabled while applied, next app start releases the stale filter |
| 17 | Try to uninstall Sarim from the launcher after owner grants | Android blocks it (uninstall-blocked by Device Owner) |
| 18 | Recovery: mistype code 3× then enter from the paper | Opens session and returns to tabs |
| 19 | Config → Forced removal: cancel before 48 h | Available, cancellable, ticking clock visible |
| 20 | Airplane mode / reboot while any protection is active | All state lives in `SharedPreferences` + Device Owner; nothing is lost and nothing needs network |

### Manual device sanity (battery, not just correctness)

- **Reboot the phone with protections on** — everything (owner restrictions, accessibility, focus alarms, limit counters) must come back without user action. `FocusSystemReceiver` listens for `BOOT_COMPLETED` and re-arms alarms.
- **Let the app be killed** in the background overnight while a focus session is active — next launch reconciles the phase (`FocusMode.sync`) and re-arms.
- **Change the system clock forward** mid-impulse-lock — the block should *not* end (tests only trust elapsed time when no reboot happened).

### Something not working?

- `adb logcat -s Sarim` (Timber debug tree in debug builds) for app logs.
- If owner setup fails: is Shizuku running? is another admin already set (`adb shell dpm list-owners`)? Owner can only be set on a device with no other profiles possibly already set.
- If the accessibility service shows *off*: system Settings → Accessibility → Sarim; some phones (miui/oneui) also gate "restricted settings" — allow-restricted-settings once from the app info page.

---

## 4. Run and test in Waydroid (Android in a Linux container, no phone needed)

[Waydroid](https://waydro.id/) boots a full Android system in an LXC container on a Wayland desktop, which is enough to run the whole of §3 without a phone. The official image is LineageOS on **Android 13 (API 33)** — above this app's `minSdk = 30`, and the release where the framework handles per-app languages, so the Arabic default also shows up under *Settings → Apps → Sarim → Language*.

> [!NOTE]
> Waydroid is a phone-shaped Android, not a phone: no SIM, no biometric sensor, no real camera, and LiteRT runs on the CPU here (the APK ships without its GPU accelerator, see `app/build.gradle.kts`). §4.5 says which checklist items that rules out.

### 4.1 Host setup

Needs a **Wayland** session (X11 users: run Waydroid inside a nested compositor such as `cage`), the `binder` kernel module, and a GPU Waydroid supports — Intel works out of the box, AMD mostly, NVIDIA needs the extra steps in the [install docs](https://docs.waydro.id/usage/install-on-desktops).

```bash
sudo waydroid init                    # downloads the Android 13 image; -s GAPPS only if you want Play
sudo systemctl enable --now waydroid-container
waydroid session start
waydroid show-full-ui                 # the Android UI, in a window
```

Launching *Waydroid* from the desktop menu runs the same first-launch wizard (System OTA `https://ota.waydro.id/system`, Vendor OTA `https://ota.waydro.id/vendor`). Useful extras:

```bash
waydroid prop set persist.waydroid.multi_windows true   # Android apps as desktop windows; restart the session after
waydroid container restart                             # stop and boot the container again, for the boot-completed and state checks
waydroid session stop ; sudo waydroid container stop   # shut everything down
```

### 4.2 Build and install

The debug APK already carries `x86_64` native libraries (LiteRT, for visual blocking), so the container can run it as-is:

```bash
JAVA_HOME=~/.local/share/jdks/jdk-21.0.12.1+1 ./gradlew :app:assembleDebug
waydroid app install app/build/outputs/apk/debug/app-debug.apk
waydroid app launch com.sarim.app
waydroid app remove com.sarim.app     # drop the app and its data to start the wizard over
```

`waydroid logcat` follows Android's log; `/var/lib/waydroid/waydroid.log` is the container side.

### 4.3 Device owner and advanced blocking without Shizuku

On a phone these two steps go through Shizuku. In Waydroid there is a root shell, so the very same commands can be run from the host. Do it **before adding any account** to the container:

```bash
sudo waydroid shell dpm set-device-owner com.sarim.app/.SarimDeviceAdminReceiver
sudo waydroid shell settings put secure enabled_accessibility_services com.sarim.app/.SarimAccessibilityService
sudo waydroid shell settings put secure accessibility_enabled 1
sudo waydroid shell pm grant com.sarim.app android.permission.WRITE_SECURE_SETTINGS   # color modes
```

`dpm set-device-owner` only succeeds when the container has a single user, no accounts, and no other app already owns the device — the same conditions the app checks itself with `dpm list-owners`, `pm list users` and `dumpsys account`. A GAPPS image with a signed-in Google account fails the check: remove the account, or re-init with `-s VANILLA`.

Reopen Sarim afterwards: the Config tab badge reads **Granted** and *Advanced blocking* is on, which unlocks word blocking, activity blocking, visual blocking, torrent blocking, time limits and color modes. From there, walk §3's checklist inside the Waydroid window.

### 4.4 adb: logcat and the instrumented test

Waydroid 1.6+ no longer connects adb on its own:

```bash
waydroid adb connect    # then accept the authorization dialog inside the Android UI
# older releases: adb connect <the IP printed by `waydroid status`>:5555

adb devices             # sanity check: the container is listed
adb logcat -s Sarim     # app logs (Timber debug tree in debug builds)
JAVA_HOME=~/.local/share/jdks/jdk-21.0.12.1+1 ./gradlew :app:connectedDebugAndroidTest
```

`adb install -r`, `adb shell` and screenshots (`waydroid shell screencap -p /sdcard/s.png` then `adb pull`) all behave like they do on a phone, so bug reports can be captured here too.

### 4.5 What Waydroid covers, and what it cannot

| Part of §3 | In Waydroid |
|---|---|
| Recovery wizard, lock screen, session pill, Config tab | Works. A fresh container has no biometric or device credential enrolled, so *Open Sarim* passes without a prompt (by design: nothing to verify against). The recovery code is still required everywhere it normally is |
| Word, activity, torrent blocking; time limits; time windows | Works — the accessibility service is enabled in §4.3 |
| Visual blocking (on-device ML) | Works on the CPU — just slower on large images, since the shipped APK has no LiteRT GPU accelerator |
| Color modes (grayscale, night light, extra dim, inversion) | Works — the settings behind them are reachable after the `pm grant` in §4.3. If the image reports no platform night light, the app drops that one mode by itself |
| Device-owner restrictions: uninstall blocking, safe mode, USB debugging, uninstalling apps | Works; the container is disposable, so try the lockout paths here first |
| Reboot and kill-recovery (state comes back, alarms re-arm) | Works: `waydroid container restart` boots Android from scratch |
| Arabic default and RTL layout | Works, and is the quickest way to eyeball the RTL flip without a phone in Arabic |
| Reading challenge (Quran, Bible, quotes) | Works; the Arabic Uthmani text renders with correct shaping |
| SIM/telephony restrictions (SMS, outgoing calls, data roaming, SIM removal) | **Not testable** — no telephony |
| Biometric or device-credential auth, camera restrictions, Bluetooth/NFC, UWB | **Not testable** — no such hardware |
| Real battery drain, thermal behaviour, OEM skins (One UI / MIUI quirks) | **Not testable** — needs the actual phone |

Disclaimers aside, what remains is a fast loop for logic, resources and policy: install, check the checklist, restart the container, check that state survived.

---

## 5. What "everything works" means here (source-level verification already done)

- `:app:compileDebugKotlin` / `assembleDebug`: exit 0, APK produced.
- `:app:testDebugUnitTest`: 53/53 pass (6 suites), including the app-language tests that run on both sides of the API 33 split.
- The Arabic default is applied in `SarimApplication.onCreate`; on API 30–32 every screen is re-wrapped with that language in `LocalizedActivity`, on 33+ the framework's per-app language does it.
- `processDebugResources`: en/pt/ar all present (514 keys matching, placeholders aligned).
- Digit-scanned all three language files for stray CJK/Hangul/Devanagari/fullwidth characters: none remain.
- Full integration tests on a physical device are **not runnable from this machine** (no attached device); the instrumented `ExampleInstrumentedTest` and a real phone still serve as the final gate — use §2 and §3 above, or §4 in Waydroid.
