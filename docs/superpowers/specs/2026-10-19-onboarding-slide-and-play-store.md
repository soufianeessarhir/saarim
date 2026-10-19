# Sarim — In-App "How It Works" Slide + Play Store Publication Study

Implementation brief for freebuff. No code changes in this document; it describes what to build and what to decide.

## 1. Goal

1. Add a clean, easy-to-grasp **"How it works / how to use it"** slide to the app.
2. Study whether publishing Sarim on Google Play is feasible and what it blocks on.

Placement decision (already made): **Option C — both**:
- a **one-time onboarding carousel** shown on first run, and
- a **permanent "How it works" entry** in the Config tab that re-opens the same carousel.

## 2. Current first-run flow (do not break this order)

`MainActivity.onCreate` (`app/src/main/java/com/sarim/app/activities/MainActivity.kt`, lines 190-221) renders in this priority order:

1. If `!SecurityManager.isRecoveryCodeSet()` → `SetupRecovery` (the 16-char recovery code wizard). This is the gate; nothing else shows until it is done.
2. Else if `!authenticated` → `LockScreen` (biometric/device credential, then challenges).
3. Else → tabs (`restrictions` / `apps` / `config`), with `SetupDeviceOwnerPrivileges` reachable from the Config tab.

**Where the onboarding goes.** The recovery wizard is the anti-bypass gate and must stay first. The onboarding carousel is *instructional*, not a gate, so it should sit at step 2.5 — i.e. after the recovery code is set but before the user reaches the tabs:

- Do **not** show it before `SetupRecovery`. The user has not yet chosen their recovery code; throwing a carousel at them first delays the one irreversible decision the app makes.
- Do **not** show it after the tabs are visible. The Config tab's "How it works" row is the permanent entry point, and a first-run carousel popping over live tabs is noisy.

Concrete approach: gate the carousel on the same `isRecoveryCodeSet` check but a *separate* `onboarding_completed` flag, and render it in the `else` branch of the `SetupRecovery` check — before the `if (!authenticated.value) LockScreen(...)` branch, or as a `Dialog` shown on first authenticated entry. The second option is simpler and keeps `MainActivity`'s `when (currentScreen)` block untouched:

```kotlin
val showOnboarding = SecurityManager.isRecoveryCodeSet(this) && !onboardingCompleted
if (showOnboarding) {
    OnboardingCarousel(onFinish = { markOnboardingComplete(); authenticated.value = true })
}
```

Flag storage: a new key in the existing `security_prefs` `SharedPreferences` (the same one `DeviceOwnerRepository` uses for `admin_result_received` / `admin_enabled`), or a dedicated `onboarding_prefs` file — either is fine, but **do not** put it in `SecurityManager`'s session state, which is cleared on logout.

## 3. In-app "How it works" slide

### 3.1 Shape

- A **horizontal pager carousel** (Compose `HorizontalPager`) of **5 slides**, each one short: one headline, one icon, 2–3 lines of body, no scrolling inside a slide.
- Slides:
  1. **What Sarim is** — "A self-control tool. It blocks apps, words, and content on your own phone. The recovery code is the only key."
  2. **Session** — "Enter the recovery code → 10-minute session → access the tabs. No session, no changes."
  3. **Restrictions** (restrictions + apps tabs) — per-app daily limits, time windows, blocked words, visual (NSFW) blocking.
  4. **Focus & impulse** — Pomodoro focus mode with alarms; panic button that suspends chosen apps for a duration that survives reboots.
  5. **Device owner setup** — "Grant owner privileges via Shizuku to unlock app suspension, install blocking, and color modes. Optional but recommended."
- Bottom: page indicator dots + **Skip** (top-right) + **Next** (bottom), last slide becomes **Got it**.
- One-time flag stored in `SharedPreferences` (`onboarding_completed = true`). Skip and Got-it both set it.

### 3.2 Re-open entry

- Add a `ListItem` row in `ConfigTab` (`app/src/main/java/com/sarim/app/screens/tabs/ConfigTab.kt`), matching the existing pattern exactly:
  ```kotlin
  ListItem(
      headlineContent = { Text(stringResource(R.string.how_it_works)) },
      supportingContent = { Text(stringResource(R.string.how_it_works_desc)) },
      leadingContent = { Icon(Icons.Outlined.Info, "") },
      modifier = Modifier.clickable { showHowItWorks = true }
  )
  HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
  ```
- It opens the same carousel as a dialog/sheet, **not** gated by the one-time flag. This is the permanent entry point (Option C).
- Place the row near the **Language settings** row (line ~233) so it sits with the other app-level settings, not with the protection toggles.
- The dialog state follows the file's existing pattern: `var showHowItWorks by remember { mutableStateOf(false) }` declared with the other dialog flags (line ~56-66), and the dialog rendered in the `if (...)` block near the bottom (line ~464) alongside `showLanguageDialog` / `showKeyboardDialog` / `showRecoveryDialog`.

### 3.3 Localization & RTL

- All slide strings go in `values/strings.xml`, `values-ar/strings.xml`, `values-pt/strings.xml`. All three files currently hold **514 keys each** — keep them at parity. Note: **no test enforces this today** (`LocaleUtilsTest` covers the Arabic default, the RTL resolution, and the per-app language, but not key parity). Parity was verified manually for the last release. Either keep it equal by hand, or add a small parity test while you are in these files — that is a genuinely useful addition, but it is optional and should not block the carousel.
- Icons must be direction-neutral or mirrored; layout uses `Arrangement`/`padding` that flips under RTL (Compose handles this if we avoid hardcoded `start`/`end` mistakes — follow existing screens).
- The carousel must be swipable in both directions; RTL should reverse swipe direction automatically via `LocalLayoutDirection`.
- The app's default language is **Arabic** on a fresh install (`SarimApplication.onCreate` + `LocaleUtils`), so the ar strings are the ones a new user sees first — they are not the optional translation.

### 3.4 Reuse, don't reinvent

- Reuse `SarimTheme`, `MaterialTheme.typography`, existing button styles (`BigActionButton`/`SmallActionButton` patterns in `LockScreen.kt`).
- Reuse the existing `SetupDeviceOwnerPrivileges` flow for slide 5's CTA ("Set up now" → navigate to `setup_device_owner`), not a new owner flow.
- Reuse `NavigationViewModel` for navigation into setup; do not add a new nav graph.

### 3.5 Tests

- Add a unit test in a new `OnboardingStateTest` (or extend an existing suite) covering: flag defaults to false, set true persists across reads, carousel has exactly 5 slides, slide 5's CTA routes to setup. Follow the existing test style (JUnit + Kotest assertions, `isIncludeAndroidResources = true`).

## 4. Play Store publication study

### 4.1 Feasibility: YES, but with conditions

The app is technically publishable. The hard parts are policy, not code.

### 4.2 Blockers and how to resolve each

| # | Blocker | Why it blocks | Resolution |
|---|---|---|---|
| 1 | **Device Owner via Shizuku** | Play's Device Admin / Device Owner policy requires the app to be the legitimate owner of a managed device and to declare it. Shizuku-driven `dpm set-device-owner` on a personal device is allowed for the device's own user, but Play reviewers flag apps that *require* device owner to function. | Make device owner **optional and clearly labeled** (it already is — "Advanced blocking" is a separate toggle). The core features (session, per-app limits, blocked words, focus, impulse lock) must work without owner. Document in the store listing that owner unlocks: app suspension (`dpm.setPackagesSuspended`), install blocking (`DISALLOW_INSTALL_APPS`), and `WRITE_SECURE_SETTINGS` for color modes. |
| 2 | **Accessibility service** | Play requires a prominent disclosure + a video showing the service in use, and the service must be essential to core functionality. | Sarim's a11y service is essential (word/activity blocking). Write the disclosure text, record a short demo video, submit both in the Play Console. |
| 3 | **`QUERY_ALL_PACKAGES`** | Restricted permission; Play requires a declaration form justifying it. | Justify: per-app usage limits and app grouping require enumerating installed apps. No alternative (the app literally cannot do its job without it). |
| 4 | **`PACKAGE_USAGE_STATS`** | Special permission, granted by user in Settings. | Already handled via Settings deep-link. Add a store-listing note. |
| 5 | **`WRITE_SECURE_SETTINGS`** | Privileged; only grantable via adb/Shizuku. | Already gated behind owner setup. Fine as long as the app degrades gracefully without it. |
| 6 | **Unknown sources restriction lifting** (`ApkUpdateInstaller`) | Play prohibits apps that install other APKs or bypass install restrictions. | **This is the biggest risk.** Verified in source: `ApkUpdateInstaller` (a) checks the picked APK's package against `PackageManager.getPackageInfo` and refuses anything not already installed (`Inspection.NewApp`, line 76), and (b) clears `DISALLOW_INSTALL_UNKNOWN_SOURCES` / `..._GLOBALLY` via `dpm.clearUserRestriction` (line 183) and restores them via `dpm.addUserRestriction` (line 140) once the install finishes. So it is scoped to updating already-installed apps — but it still *is* an app invoking `PackageInstaller`. Options: (a) remove the self-update path for the Play build and rely on Play's own update mechanism, or (b) keep it and document. **Recommended: (a)** — a `BuildConfig` flag `PLAY_STORE_BUILD = true` that disables `ApkUpdateInstaller` and hides the APK-update button in `LockScreen` (the button is at `LockScreen.kt` line 208-215, gated on `apkUpdatesAvailable`). |
| 7 | **Signing** | Debug key is not acceptable for Play. | Generate a release keystore, enable Play App Signing (upload key + app signing key managed by Google). Store the keystore outside the repo; add `dechainer.store*` to `local.properties` (already wired in `build.gradle.kts`). |
| 8 | **Privacy policy URL** | Mandatory for any app that handles data; Play rejects without it. | Sarim collects nothing off-device (on-device ML, local SharedPreferences). Still needs a hosted privacy policy stating that. Host on GitHub Pages from the repo. |
| 9 | **Data safety form** | Must declare collection/sharing. | Declare: no data collected, no data shared, no third-party SDKs that collect. LiteRT runs on-device. |
| 10 | **Content rating** | IARC questionnaire. | Sarim blocks adult content; the app itself is a productivity/utility. Answer honestly; expect a "Teen" or "Everyone" rating. |
| 11 | **Target SDK 36 / minSdk 30** | Play requires targetSdk ≥ 34 (as of 2025). | Already at 36. Fine. minSdk 30 excludes Android 11 and below — acceptable, document it. |
| 12 | **Package name** | `com.sarim.app` is fine but generic. | Keep it; renaming after first publish is painful. |
| 13 | **Store listing assets** | Title, short/full description, screenshots, feature graphic, icon. | Already have 9 phone screenshots in `fastlane/metadata/android/en-US/images/phoneScreenshots/`. Need: feature graphic (1024×500), localized descriptions for ar-SA and pt (currently only en-US has screenshots). |
| 14 | **Arabic RTL in store listing** | Play supports RTL listings. | Translate title/description to Arabic; ensure screenshots show RTL UI. |

### 4.3 Recommended build flavors

Add a `play` build flavor (or `BuildConfig.PLAY_STORE_BUILD`) that:
- disables `ApkUpdateInstaller` and hides the APK-update button in `LockScreen`,
- keeps Shizuku/owner setup (allowed, but documented),
- keeps the accessibility service (with disclosure).

Keep the current `debug`/`release` builds as the sideload (non-Play) variant so power users keep the self-update path.

### 4.4 Effort estimate

- Onboarding slide: ~1–2 days (UI + strings in 3 languages + tests).
- Play Store readiness: ~3–5 days (signing setup, privacy policy, disclosure video, listing copy in 3 languages, declaration forms, review iteration). Play review itself takes 1–7 days after submission.

### 4.5 What to NOT do

- Do not ship the self-update path to Play (blocker 6).
- Do not rename the package after first publish.
- Do not skip the accessibility disclosure video — it is the #1 rejection reason for a11y-based apps.

## 5. Out of scope for this brief

- Actual implementation (freebuff handles).
- Generating the privacy policy text (separate doc).
- Recording the disclosure video.
- Play Console account creation / payment.

## 6. Acceptance criteria (for freebuff to verify)

- Onboarding carousel shows exactly once on first run, after recovery code is set, before first authenticated tab entry.
- Skip and Got-it both persist `onboarding_completed = true`.
- Config tab has a "How it works" row that re-opens the carousel regardless of the flag.
- All 5 slide strings present in en/ar/pt with matching keys; RTL layout verified in ar.
- New unit test suite passes; existing 53 tests still green.
- `PLAY_STORE_BUILD` flag exists and disables the APK-update path when true.
- No changes to the device-owner / Shizuku flow (reuse as-is).

## 7. Device-owner / Shizuku flow — must be preserved verbatim

This is the part the user explicitly asked to keep. The implementation must **not** change it; the onboarding slide only *describes* it.

### 7.1 Existing flow (reuse as-is)

Source of truth: `app/src/main/java/com/sarim/app/data/DeviceOwnerRepository.kt` and `app/src/main/java/com/sarim/app/viewmodels/DeviceOwnerViewModel.kt`. The flow is:

1. **Shizuku must be installed and running first.** `DeviceOwnerRepository.isShizukuInstalled()` checks for `moe.shizuku.privileged.api`. If missing, `installShizuku()` opens the Play Store listing; `openShizukuSetupGuide()` opens `https://shizuku.rikka.app/guide/setup/`. Permission is checked via `Shizuku.checkSelfPermission() == PERMISSION_GRANTED` (and `Shizuku.isPreV11()` is rejected).

2. **Device owner is granted through Shizuku, not directly.** `processDeviceOwnerPrivileges(remove = false)` runs:
   ```
   ShizukuRunner.command("dpm set-device-owner com.sarim.app/.SarimDeviceAdminReceiver", listener)
   ```
   then waits 250 ms and re-checks `dpm.isDeviceOwnerApp(packageName)`.

3. **Authoritative confirmation is async, not the shell exit code.** `waitForAdminResult(timeoutMs = 5_000)` polls `SharedPreferences("security_prefs")` for `admin_result_received == true` (written by `SarimPolicyUpdateReceiver`), then reads `admin_enabled`. The UI must wait for this broadcast before flipping the badge to green — the existing `DeviceOwnerViewModel.waitForAdminResult()` already does this. **Do not replace this with a fire-and-forget check.**

4. **Pre-conditions are enforced before attempting.** The setup screen (`SetupDeviceOwnerPrivileges`) must keep checking `dpm list-owners`, `pm list users`, and `dumpsys account` via Shizuku and block when: another device admin is already set, more than one user exists, or any account is signed in. The existing `AccountWarningDialog` / `UserWarningDialog` / `ExistingOwnerDialog` handle these. A GAPPS image with a signed-in Google account fails — remove the account or re-init with `-s VANILLA`.

5. **On success**, `ColorFilterController.grantPermissionViaShizuku(context)` is called to grant `WRITE_SECURE_SETTINGS` for color modes.

6. **Removal** goes through `processDeviceOwnerPrivileges(remove = true)`, which calls `dpm.clearDeviceOwnerApp(packageName)` and writes the same prefs flags with `admin_enabled = false`.

### 7.2 What device owner actually unlocks (write the slide from this list)

Verified in source — this is the accurate set of owner-only capabilities, for slide 5's copy:

| Capability | Source | Notes |
|---|---|---|
| **App suspension** | `AppRepository.kt:147`, `FocusMode.kt:366/442`, `SarimAccessibilityService.kt:1154` — all use `dpm.setPackagesSuspended(...)` | This is what makes the impulse lock and per-app limits actually block an app. Without owner, the app is only a countdown timer with no teeth. |
| **Install blocking** | `SarimAccessibilityService.kt:597` adds `DISALLOW_INSTALL_APPS`; line 514 clears it | Blocks installing new apps while a block is active. |
| **Private DNS control** | `DeviceOwnerRepository.setPrivateDNS()` → `dpm.setGlobalPrivateDnsModeSpecifiedHost` | Used by the DNS settings row in Config. |
| **`WRITE_SECURE_SETTINGS`** | granted via `ColorFilterController.grantPermissionViaShizuku(context)` on owner success | Required for the color-filter modes (grayscale / night light / dim / invert). |
| **User restrictions** | `SystemRestrictions.kt` lists `DISALLOW_CONFIG_VPN`, `DISALLOW_CONFIG_PRIVATE_DNS`, `DISALLOW_FACTORY_RESET`, `DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY` | The app can read and toggle the full `DISALLOW_*` / `ALLOW_*` set. |

**Do not claim "safe mode blocking"** — there is no `DISALLOW_SAFE_MODE` usage in the codebase. The uninstall-blocking that device owner provides comes from the device-owner role itself (Android blocks `PackageInstaller.uninstall` for the owner), not from an explicit restriction the app sets.

### 7.3 What the onboarding slide may and may not do

- **May**: describe the owner capabilities in the table above in plain language on slide 5, and offer a CTA "Set up now" that navigates to the existing `setup_device_owner` screen (via `NavigationViewModel.navigateTo("setup_device_owner")`).
- **May not**: reimplement any of the above, add a new owner flow, change the Shizuku permission listener, change the polling timeout, or skip the account/user checks.
- **May not**: call `processDeviceOwnerPrivileges` from the onboarding itself. The CTA only navigates; the actual grant happens on the existing setup screen.

### 7.4 Other verified facts for the slide copy

- The session is **10 minutes** (`SecurityManager.kt:64` — `sessionEndTime = now + (10 * 60 * 1000)`), renewed from the app bar's refresh button.
- Focus mode re-arms itself after a reboot, an app update, or a clock change: `FocusSystemReceiver` (in `notifications/FocusReceiver.kt`) handles `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `TIME_SET`, calling `FocusMode.sync(context)` and `FocusScheduleManager.reschedule(context)`.
- The app blocks its own uninstall once it is device owner — this is a *feature* (the user asked for it) and worth stating plainly in the slide so it is never a surprise.

### 7.5 Acceptance (device-owner specific)

- Shizuku install/permission flow unchanged.
- `dpm set-device-owner` still runs via `ShizukuRunner.command` with the same listener shape.
- `waitForAdminResult` still polls `security_prefs` for `admin_result_received`.
- Badge still flips only after the broadcast, not the shell exit code.
- Account/user/owner pre-checks still gate the attempt.