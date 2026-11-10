# Sarim — Project and Feature Documentation

**Documented version:** 1.8.0 (versionCode 18)
**Verified against the source tree:** 10 November 2026
**Package:** `com.sarim.app` · **License:** Apache 2.0 · **Platform:** Android 11+ (API 30)

This document explains what Sarim is, how it is put together, and what every feature does.
Companion documents:

| Document | Purpose |
|---|---|
| [FEATURES.md](FEATURES.md) | Feature reference with defaults and gating rules |
| [BUILD_AND_RUN.md](BUILD_AND_RUN.md) | Build, test, install and Device Owner automation |
| [EVALUATION.md](EVALUATION.md) | Assessment of quality and what is missing before publishing |
| [../README.md](../README.md) | Project introduction, screenshots, install and recovery warnings |

---

## 1. What Sarim is

Sarim (Arabic **صارم**) is an Android app with one job: keep pornography and adult content off
the phone it is installed on, and make turning that protection off deliberately difficult.

It is not a general screen-time manager, a parental-control dashboard, a VPN, or a productivity
timer. Those features existed in the upstream project and were removed on purpose (see §17.1).
What remains is a small set of protection layers that are on from the first launch, plus a
commitment period that freezes them for a duration the user chooses but cannot shorten.

The app is a fork of [Déchaîner](https://github.com/warleysr/dechainer) (Apache 2.0), renamed and
narrowed to adult-content blocking. The fork adds the commitment, the built-in adult-site and
forbidden-word lists, the filtering DNS, Tor/torrent blocking, Arabic as the default language,
an onboarding carousel, and a Play-policy build flag.

### At a glance

| | |
|---|---|
| Application ID / namespace | `com.sarim.app` |
| Version | 1.8.0 (versionCode 18) |
| Minimum Android | 11 (API 30) |
| Target / compile SDK | 36 |
| Language / UI | Kotlin 2.4.10, Jetpack Compose (Material 3) |
| Build system | Gradle 8.13, Android Gradle Plugin 8.13.2, JDK 17–21 |
| On-device ML | LiteRT 2.2.0, bundled MobileNetV2 (`nsfw_model.tflite`) |
| Privileged bridge | Shizuku 13.1.5 (ADB / wireless debugging; no root) |
| Languages | Arabic (default), English, Portuguese; full RTL |
| Source size | 64 Kotlin files in `app/src/main/java`, 922-line accessibility service |
| Tests | 9 JVM test classes, 81 tests; no instrumented tests |
| Distribution | Sideloaded APK (no store build configured by default); optional `-PplayStoreBuild=true` variant |

---

## 2. The problem the design solves

A normal Android app lives in a sandbox. It cannot see what other apps display, cannot prevent
itself from being uninstalled, cannot stop the user from changing the system resolver, and cannot
keep another browser from being installed. That is why most blockers are one settings screen away
from being switched off in a weak moment.

Sarim asks for two Android powers that ordinary apps do not get:

| Power | What it is | What Sarim uses it for |
|---|---|---|
| **Device Owner** | The device-management level companies use for work phones, granted once through `dpm set-device-owner` | System restrictions, private DNS, suspending apps, hiding apps, blocking uninstall, pushing browser policies |
| **Accessibility Service** | The API screen readers use, granted in Android accessibility settings (or applied through Shizuku) | Reading on-screen and typed text, capturing media regions, detecting the foreground app |

Both are granted during setup. The accessibility service is then protected against casual
disablement by the enforcement loop, and the Device Owner status cannot be removed without the
recovery code — or the deliberately slow 48-hour forced removal.

### Design principles

1. **Protection is on by default.** There is no "enable protection" master switch. Visual
   blocking, forbidden words, browser lists, SafeSearch, filtering DNS, Tor/torrent blocking and
   the eight hardening restrictions are re-asserted on every launch.
2. **Settings can only be tuned, never switched off.** The recovery code unlocks a 10-minute
   session for the adjustable parts of a layer; it cannot disable the layer itself.
3. **The commitment outranks the recovery code.** While a commitment runs, even a valid session
   cannot weaken protection. Only actions that make the phone stricter are allowed.
4. **The key to the app lives on paper.** The 16-character recovery code is generated once, shown
   once, and confirmed by typing it back from a handwritten copy.
5. **Nothing leaves the device.** There is no account, no analytics, no ad SDK, no telemetry and
   no cloud classifier. The single outbound request is the optional Play Store age-rating lookup.

---

## 3. How it works

### 3.1 The enforcement loop

`security/MandatoryProtection.enforce()` is the single place that re-asserts the protection stack.
It is **idempotent**: it writes only what is out of line and returns whether anything changed. It
runs:

- on every app launch (`SarimApplication.onCreate`);
- on every accessibility-service start (`SarimAccessibilityService.onServiceConnected`);
- on `CommitmentLock.enforce`, which is called on launch and from the service;
- from the accessibility service's `SharedPreferences` change listener;
- after a commitment is started (`CommitmentLock.start`).

Because it is idempotent, clearing a preference in the UI or by hand is undone at the next check.
The screens also refuse to save a built-in list with the built-in entries missing.

### 3.2 The two setup powers in detail

**Device Owner unlocks:**

- the `UserManager` restrictions in §6.9;
- `setGlobalPrivateDnsModeSpecifiedHost` for the filtering resolver;
- `setPackagesSuspended` for Tor, torrents, unsupported browsers and 18+ apps;
- `setApplicationHidden` and `setUninstallBlocked` on the Apps screen;
- `setApplicationRestrictions` for `URLBlocklist` and `ForceGoogleSafeSearch` in browsers.

**The Accessibility Service unlocks:**

- active word blocking in editable fields;
- passive word blocking against on-screen text and content descriptions;
- visual blocking (media-region discovery, screenshot, classification);
- foreground-package detection, used by the visual, word and browser layers;
- install-time inspection of new packages (Tor, torrents, browsers, age rating);
- the fallback that suspends watched apps and blocks new installs if the service goes down.

Without Device Owner, the accessibility layer can still read text and scan images, but it cannot
suspend apps, apply DNS or hold restrictions. In the current build, however, the setup flow
requires Device Owner before the commitment can be started and before the tabs open (see §5.2).

### 3.3 Shizuku

Shizuku is a local ADB bridge: it runs a shell with elevated privileges on the phone itself,
reached over wireless debugging, so no PC is needed after the first pairing. Sarim uses it to run:

```text
dpm set-device-owner com.sarim.app/.SarimDeviceAdminReceiver
settings put secure enabled_accessibility_services com.sarim.app/.SarimAccessibilityService
settings put secure accessibility_enabled 1
```

`DeviceOwnerRepository` does not trust the shell exit code. It waits for the authoritative
`DEVICE_POLICY_CHANGED` / `DEVICE_POLICY_SET_RESULT` broadcast, delivered to
`SarimPolicyUpdateReceiver` and persisted by `DeviceAdmin`, with a bounded polling window.

On a rooted or ADB-connected device the same commands can be issued directly; on Waydroid the host
shell already has the needed privileges, which is why the Makefile path does not use Shizuku.

---

## 4. First run and setup

### 4.1 Prerequisites

- Android 11 or newer.
- Shizuku installed and running (wireless debugging paired).
- **One user, no accounts, no other device owner.** Android enforces these conditions for
  `dpm set-device-owner`; in practice this means a freshly factory-reset phone before signing into
  Google, or removing all accounts first. This is the largest adoption constraint in the app.
- For the APK build: JDK 17–21, Android SDK platform 36, and `local.properties` with `sdk.dir`.

### 4.2 The order the code enforces

`MainActivity` renders exactly one of these, in order:

1. **Recovery code wizard** — if no code is stored.
2. **Device Owner setup** — if the app is not Device Owner.
3. **Commitment selection** — if no commitment has ever been started.
4. **Lock screen** — biometric/device-credential check, then the access challenge(s).
5. **Onboarding carousel** — once, on the first authenticated entry.
6. **Main tabs** — Protection, Apps, Config.

The Device Owner step comes **before** the commitment because the commitment's system restrictions
close ADB, and ADB is how Shizuku grants Device Owner. Granting it afterwards would be impossible
until the period ended. The commitment cannot be started until the app is Device Owner.

### 4.3 Device Owner flow

`SetupDeviceOwnerPrivileges` shows a different card depending on state:

| State | Screen |
|---|---|
| Shizuku not installed | Install Shizuku (Play Store link) |
| Shizuku not running | Open the official Shizuku setup guide |
| Shizuku permission not granted | Request permission |
| Accounts present | Warning dialog listing them |
| Extra users present | Warning dialog |
| Another owner already registered | Dialog naming the existing owner |
| Eligible | "Get owner privileges" button |
| Already owner | "Remove privileges" card |

### 4.4 Advanced blocking

"Advanced blocking" is the accessibility service switch, applied through Shizuku rather than
Android's accessibility settings. It is required for every content layer. Once granted, the Config
tab shows it as on. If the service stops without the app asking it to (`onUnbind` with
`disablingService == false`), the service:

1. suspends the apps it was watching for forbidden words,
2. applies `DISALLOW_INSTALL_APPS` so a new unrestricted browser cannot be installed in the gap,
3. opens `AccessibilityRequestActivity` to ask for the service again.

Re-enabling it is allowed during a commitment because it only adds protection.

---

## 5. Feature reference

### 5.1 The commitment

**Purpose:** a chosen period during which adult-content protection cannot be turned off, weakened
or removed — not by the settings, not by the recovery code, not by clearing preferences.

| | |
|---|---|
| Presets | **Regular** (no lock), 90, 180, 365, 730 days, or **permanent** |
| Accepted range | 90 – 3650 days (values below the floor are clamped to 90) |
| Chosen | Once, during setup, before the app opens |
| Stored | `security_prefs`: `commitment_start_rtc`, `commitment_start_elapsed`, `commitment_duration_ms` (`-1` = permanent, `-2` = regular), `commitment_last_seen_rtc` |
| Release | The period running out; or the 48-hour forced removal (not offered for a permanent or regular setup) |

`CommitmentLock.start()` is the only entry point. `hasStarted()` guards it, so a running commitment
is never restarted, shortened or replaced. There is no `stop()` anywhere in the code.

**What it freezes.** While `isLocked()`:

- every action that goes through `RecoveryGate` is refused, including an action a valid recovery
  session would normally authorise;
- the only bypass is `strengtheningAction = true`, used by the controls that can only add
  protection: enabling advanced blocking, blocking an app, suspending an app.

**How the deadline is measured.** The remaining time is recomputed, never stored as a countdown:

- `elapsedRealtime` is authoritative while the device stays up, so changing the date does nothing;
- after a reboot (`elapsedRealtime` moving backwards) the wall clock takes over;
- a persisted watermark (`commitment_last_seen_rtc`) records the furthest date ever seen, so
  setting the date backwards after a reboot cannot buy back time;
- the wall-clock reading is capped at the deadline, so a jump far into the future cannot make the
  remaining time negative.

A permanent commitment reports `Long.MAX_VALUE` remaining, `0` as an end time, and disables the
forced removal. A **regular** setup records `-2`, reports `0` remaining, and leaves the settings
changeable with the recovery code; protection itself stays on.

### 5.2 Recovery code and sessions

- **16 characters** from `A–Z`, generated with `SecureRandom`.
- Stored in `recovery_prefs` under `recovery_code`.
- The wizard: generate → write it down on paper → type it back letter by letter → choose where to
  keep it, with a one-tap option to send it to an accountability partner through the share sheet.
- A later change is possible through Config → *Change recovery code* (`RecoveryGenerateDialog`),
  gated by the code and refused during a commitment.
- Entering the code opens a **10-minute session** (`SecurityManager.sessionEndTime`). The session is
  renewable while active; an expired session requires the code again. A countdown pill and
  renew/end buttons appear in the top bar.
- Every gated action calls `RecoveryGate.run`, which asks for the code only when no session is
  active. The dialog's `commitmentBlocked` state explains a refusal.

The session is in process memory, not persisted. `MainActivity.onDestroy` ends it.

### 5.3 Visual blocking (on-device ML)

| | |
|---|---|
| Model | `nsfw_model.tflite`, MobileNetV2, 224×224 input, bundled |
| Runtime | LiteRT, CPU; the GPU accelerator and JNI bridge are excluded from the APK |
| Labels | `drawings`, `hentai`, `neutral`, `porn`, `sexy` |
| Categories classified | `drawings`, `hentai`, `porn`, `sexy` — always; `neutral` is never in the block list |
| Monitored apps | `com.reddit.frontpage` by default and **not removable**; more apps can be added |
| Sensitivity | 0.5 default; adjustable 0.3 – 0.7 |
| Escalation | On by default: 5 blocks in 15 minutes → suspend the app for 30 minutes |
| Scan throttle | At most one scan every 2 seconds, never overlapping |

**Scan pipeline:**

1. On a content-changed event in a monitored app, the accessibility tree is walked for likely
   media views. Signals, in order:
   - view class names containing `Image`, `Photo`, `Video`, `Media`, `Player`, `Gif`,
     `TextureView`, `SurfaceView`, `WebView`;
   - accessibility vocabulary (`foto`, `photo`, `imagem`, `image`, `video`, `gif`, `media`, …);
   - Reddit post-card description patterns (`18+, …`, `posted in …`);
   - a geometric fallback: unlabelled leaf nodes with a plausible photo/video aspect ratio
     (0.4–3.0), largest first, capped at 5 candidates.
2. Only those regions are screenshotted and cropped — not the whole screen — so the classifier sees
   media rather than a page of text and buttons.
3. Each crop is classified; the highest score among the four adult categories is compared with
   the threshold. Above it, the app is force-closed via a suspend/un-suspend cycle.
4. Detections are counted per package in a persisted sliding window
   (`VisualBlockingSuspensionTracker`). When the threshold is reached the app is suspended for the
   configured duration; the release timer is restored after a service restart.
5. `NsfwContentBlockedActivity` shows the block screen and, if applicable, how long the suspension
   lasts.

`MandatoryProtection` re-adds the default monitored app on every enforce pass. The categories,
enablement and suspension escalation are always on and are no longer exposed as settings or stored
switches, so there is nothing to reassert.

### 5.4 Forbidden words

**Built-in list:** roughly 80 unique entries across English, Arabic and Portuguese
(`ProtectionDefaults.FORBIDDEN_WORDS`). Matching is case-insensitive and boundary-aware:
`(?<![\p{L}\p{N}_])word(?![\p{L}\p{N}_])`. The list is unioned back on every launch and on every
save (`BlockedWordsViewModel.updateWords`), so it cannot be removed through the UI; the user can
add more.

**Active blocking** (`blocked_words_prefs` → `target_packages`):

- applies to selected apps, and **every installed browser is always selected** (re-added by
  `MandatoryProtection.enforce`);
- the service records the focused editable field while the user types;
- when focus leaves the field (typing stops), it checks the text; on a match it performs
  `GLOBAL_ACTION_BACK` and opens `BlockedWordActivity` with the matched word.

**Passive blocking** (per app, `passive_words_<package>`):

- an optional extra list checked against the text and content descriptions currently on screen;
- a match closes the app and shows the block screen.

Browser address bars and search fields are covered by the always-on active list. The built-in list
deliberately avoids ambiguous words such as "sex" alone, which would block health and news content.

### 5.5 Adult sites in browsers

- Every installed app resolving `CATEGORY_APP_BROWSER`, an `http://` view or an `https://` view is
  treated as a browser.
- Browsers that declare `URLBlocklist` and `ForceGoogleSafeSearch` receive a managed configuration
  with the built-in list (about 80 bare hostnames, matched with subdomains) plus every user list,
  and `ForceGoogleSafeSearch = true`.
- The built-in list has the reserved id `sarim-mandatory-adult-sites`. It is rewritten on every
  launch, cannot be edited or deleted in the UI, and `applyRestrictions()` unions the domains in
  regardless of what the preference file says.
- A browser that does **not** support managed restrictions is suspended rather than left
  unrestricted — both when it is first seen and when it is installed while the service runs.
- Newly installed browsers receive the policy before they can be opened.

### 5.6 Filtering private DNS

Only three resolvers are accepted:

| Host | Label |
|---|---|
| `family.cloudflare-dns.com` | Cloudflare Family |
| `family.adguard-dns.com` | AdGuard Family |
| `adult-filter-dns.cleanbrowsing.org` | CleanBrowsing Adult |

There is no "automatic", no "off" and no free-text host. On every enforce pass, if the mode is not
"hostname" or the host is not one of the three, it is replaced with
`family.cloudflare-dns.com`. The picker offers only the three. The OS setting itself is closed by
`DISALLOW_CONFIG_PRIVATE_DNS`.

### 5.7 Tor, Orbot and torrent clients

**Tor detection** (`BrowserRestrictionsManager`), in order:

1. official and legacy package ids (`org.torproject.torbrowser`, `…android`, `…torservices`,
   `info.guardianproject.orbot`, …);
2. a normalized identity match on the package name **and** the visible app label
   (`torbrowser`, `torproject`, `orbot`, `orfox`, `onionbrowser`), which catches rebuilds and
   rebrands;
3. a cached sweep of all installed apps.

**Torrent detection:** every app that handles a `magnet:` link or an
`application/x-bittorrent` payload, probed through five intent shapes.

**Enforcement:** detected packages are suspended with `setPackagesSuspended`, recorded in
`security_prefs` (`tor_suspended_packages`, `torrent_suspended_packages`), and swept again on every
service start, so a client installed before setup is caught. A new package triggers the
package-added receiver and is suspended on the spot. Both blocks are always on and are listed only
as status rows on the Protection tab; Config has no control for them and `enforce()` keeps the
backing preferences true.

### 5.8 Apps the store rates 18+

`PlayStoreRatingFetcher` fetches the public Play Store listing with Jsoup and reads:

- the `application/ld+json` block for the title and content rating;
- the content-rating descriptor box for lines such as "Nudity", "Explicit Sex", "Sexual Content".

The check runs when a package is installed and the first time an app is opened if no cached rating
exists. If the store reports explicit content **and** "Rated 18+", the app is suspended. The result
is cached in `app_ratings`; a 404 is cached as "not explicit". Offline means no action — the other
layers still apply.

Requests use `hl=en`, `gl=BR`, a desktop user agent, and a 15-second timeout. Only the package name
is sent; nothing about the user, screen or usage.

### 5.9 System restrictions

Eight restrictions are mandatory and cannot be unticked:

| Restriction | What it closes |
|---|---|
| `DISALLOW_CONFIG_VPN` | Pointing the device at a VPN that skips the filtering DNS |
| `DISALLOW_CONFIG_PRIVATE_DNS` | Switching the resolver back |
| `DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY` | Sideloading a browser or an app the store would not hand out |
| `DISALLOW_UNINSTALL_APPS` | Uninstalling the blocker |
| `DISALLOW_APPS_CONTROL` | Force-stopping or clearing its data |
| `DISALLOW_SAFE_BOOT` | Booting into safe mode, where third-party apps are off |
| `DISALLOW_DEBUGGING_FEATURES` | Attaching ADB or a debugger |
| `DISALLOW_CONFIG_DATE_TIME` | Moving the clock a commitment is measured against |

Everything else Android exposes as a `UserManager` policy is listed on the **System restrictions**
screen, searchable, and can be added on top: factory reset, accounts, Bluetooth, and so on. A
per-app restriction editor also lets the user inspect and apply restrictions an individual app
declares through `RestrictionsManager`.

The eight mandatory entries are no longer listed as disabled checkboxes. The screen opens with a
short status line naming what is always restricted, then lists only the optional policies.

### 5.10 App management

The **Apps** tab lists installed apps with search and filter chips for *system apps*, *blocked* and
*explicit content*. Each row has three controls:

| Control | API | Effect |
|---|---|---|
| Block | `AppRepository.setAppHidden` → `setApplicationHidden` | Hides the app from the launcher; re-applied after reinstall |
| Suspend | `AppRepository.setAppSuspended` → `setPackagesSuspended` | Suspends it outright; the icon remains but tapping does nothing |
| Uninstall lock | `AppRepository.setUninstallBlocked` → `setUninstallBlocked` | The app cannot be removed |

Apps are flagged `18+` when the store-rating check found explicit content. Blocking and suspending
are allowed during a commitment; unblocking and un-suspending are not.

### 5.11 Access challenges

After biometric/device-credential authentication, the user must pass the configured challenges in
sequence. The default is **Words**:

| Challenge | Behaviour |
|---|---|
| **Words** (default) | Type 32 random common words correctly, one at a time; a wrong character cannot be entered because input is compared as it is typed |
| **Math** | Five two-digit additions; a wrong answer resets to the first problem |
| **Tetris** | Play for 1–15 minutes (default 3); only active play counts; gravity increases with level, with a lock delay, line-clear animation and optional sound |
| **Reflective reading** | Read a configurable number of passages (default 5) word by word at a slow pace; sources: Bible, Quran, quotes, or custom passages |

Every challenge screen has a **give up** button that returns to the lock screen. The current build
uses the default **Words** challenge; the list and the Tetris/reading values are read from
`security_prefs` (`access_challenges`, `tetris_minutes`, `tetris_sound`, `reading_source`,
`reading_count`, `reading_custom_passages`), but there is no settings screen that writes them.

### 5.12 Lock screen

- Authentication is deliberately **not** triggered on open: the lock screen is the first thing the
  user sees, and the prompt runs only when they tap *Access app*.
- Order: open → *Access app* → biometric (`BIOMETRIC_STRONG`) → device credential → challenge(s).
- If neither biometrics nor a device credential is configured, the challenge runs without an
  authentication step (this is the case in Waydroid).
- The commitment (days left, "Permanent", or "Regular") is shown for orientation and cannot be
  unlocked here.
- APK updates of already-installed apps are offered without authentication when unknown sources are
  restricted; the Play Store build hides this path.

### 5.13 Forced removal

Config → *Forced removal* is the deliberate last resort when the recovery code is lost:

- starts a 48-hour timer, accumulated with `elapsedRealtime`;
- can be cancelled before it completes;
- when it reaches zero, Device Owner can be removed without the recovery code and the app can be
  uninstalled;
- is **hidden** for a permanent commitment and for a regular setup (`canStartForcedRemoval`
  returns false and the row is not rendered).

This is the only difference between "permanent" and a very long timed commitment.

Implementation note: the current accumulation adds the device's uptime after a reboot rather than
the wall-clock gap, so time spent powered off is not counted. See
[EVALUATION.md](EVALUATION.md#p0-6-forced-removal-does-not-count-time-spent-powered-off).

### 5.14 Onboarding, language, keyboard

- **Onboarding:** a 5-slide "How it works" carousel appears once after the first authenticated
  entry. Its completion flag lives in `onboarding_prefs`; the same carousel can be reopened from
  Config → *How it works* at any time.
- **Language:** Arabic is the default on a fresh install. Config → *Language* switches to English
  or Portuguese. `localeFilters` declares `en`, `pt`, `ar`; the manifest has
  `android:supportsRtl="true"` and a `locales_config`.
- **Keyboard:** normal or shuffled keypad for the recovery code and word challenge, so the code
  cannot be memorised by watching finger positions.
- **Tetris sound** can be muted.

### 5.15 APK updates (non-Play build)

The lock screen can open an APK picker in the non-Play build. The updater is restricted to APKs
whose package is already installed, so it can update an app but not introduce a new one. The
`-PplayStoreBuild=true` build sets `BuildConfig.PLAY_STORE_BUILD` and removes the entry point, per
Google Play policy.

### 5.16 Private-DNS picker errors

The DNS dialog reports two failure modes from `DevicePolicyManager`: host not serving, and a
generic failure. It applies the change with a 15-second timeout and keeps the dialog open on
failure.

---

## 6. What is gated, and by what

Gating is implemented by `screens/common/RecoveryGate.kt`.

| Action | Recovery code / session | During a commitment |
|---|---|---|
| Read any screen, commitment, owner state | No | Allowed |
| Language, keyboard layout | No | Allowed |
| Reopen onboarding | No | Allowed |
| Start/cancel forced removal (timed commitment) | No | Allowed |
| Add/remove monitored apps, change sensitivity | Yes, 10-min session | Refused |
| Add/remove forbidden words, per-app passive lists | Yes | Refused |
| Add URL lists, attach them to browsers, suspend a browser | Yes | Refused |
| Switch among the three DNS hosts | Yes | Refused |
| Add/remove optional `UserManager` restrictions | Yes | Refused |
| Block/hide, suspend, uninstall-lock an app | Yes | Allowed (block/suspend only add protection) |
| Enable advanced blocking | Yes | Allowed |
| Disable advanced blocking | Yes | Refused |
| Change the recovery code | Yes | Refused |
| Built-in words, adult-site list, SafeSearch, Tor/torrents, the eight restrictions, visual categories and enablement | No control exists | No control exists |

---

## 7. UI map

```text
First run
├── SetupRecovery           16-character code wizard (paper copy, confirm, store)
├── SetupDeviceOwner        Shizuku / owner / account / user checks
├── SetupCommitment         Regular, 90/180/365/730 days or permanent, acknowledgement
├── LockScreen              commitment banner, biometrics, challenge(s)
└── OnboardingCarousel      5 slides, once

Main tabs
├── Protection              commitment card, incomplete-setup warning, layer list
├── Apps                    search, filters, block / suspend / uninstall lock
└── Config
    ├── Commitment (read-only)
    ├── Device owner state / SetupDeviceOwnerPrivileges
    ├── Advanced blocking (accessibility switch)
    ├── VisualBlockingScreen
    ├── BlockedWordsScreen
    ├── BrowserRestrictionsScreen
    ├── Private DNS dialog
    ├── RestrictionsTab (optional system restrictions + per-app restrictions)
    ├── How it works
    ├── Language
    ├── Keyboard
    ├── Change recovery code
    └── Forced removal
```

Block screens (`BlockedWordActivity`, `NsfwContentBlockedActivity`) and
`AccessibilityRequestActivity` are separate, no-history activities that can appear over other apps.

---

## 8. Architecture and code map

```text
app/src/main/java/com/sarim/app/
├── AppPrefs.kt                     onboarding flag
├── SarimApplication.kt             locale, package receiver, launch-time enforce
├── SarimAccessibilityService.kt    word/visual/Tor/torrent/rating enforcement (922 lines)
├── SarimDeviceAdminReceiver.kt     DeviceAdmin receiver
├── SarimPolicyUpdateReceiver.kt    authoritative device-policy broadcast
├── activities/
│   ├── MainActivity.kt             setup gate and tab host
│   ├── LockScreen.kt               biometrics, challenges, APK update entry
│   ├── BlockedWordActivity.kt      word-block screen
│   ├── NsfwContentBlockedActivity.kt
│   ├── AccessibilityRequestActivity.kt
│   └── LocalizedActivity.kt        per-app locale wrapper
├── data/
│   ├── DeviceAdmin.kt              DevicePolicyManager + component
│   ├── DeviceOwnerRepository.kt    Shizuku commands, DNS, accounts, accessibility settings
│   ├── SystemRestrictions.kt       UserManager policy catalogue
│   ├── BrowserRestrictionsManager.kt  browser/Tor/torrent detection, managed config
│   ├── AppRepository.kt            installed apps, hide/suspend/uninstall lock
│   ├── PlayStoreRatingFetcher.kt   Jsoup scrape of the store listing
│   ├── ProtectionDefaults.kt       built-in words, adult sites, DNS hosts
│   ├── VisualBlockingSettings.kt   keys and defaults
│   ├── VisualBlockingSuspensionTracker.kt
│   ├── ApkUpdateInstaller.kt       local-APK update path
│   └── ReadingPassages.kt          Bible/Quran/quotes corpus
├── models/                         AppItem, BlockedList
├── notifications/                  APK update result receiver
├── screens/
│   ├── setup/                      recovery, owner, commitment
│   ├── challenges/                 words, math, Tetris, reading, sounds
│   ├── common/                     RecoveryGate, wizards, onboarding, APK dialogs
│   └── tabs/                       Protection, Apps, Config, Visual, Words, Browser, Restrictions
├── security/
│   ├── SecurityManager.kt          code, session, challenges, forced removal
│   ├── CommitmentLock.kt           commitment start and time arithmetic
│   └── MandatoryProtection.kt      idempotent enforcement of the whole stack
├── ui/theme/                       Compose theme
├── utils/
│   ├── NsfwContentDetector.kt      LiteRT interpreter
│   ├── ShizukuRunner.kt            shell command bridge
│   └── LocaleUtils.kt              Arabic default, per-app locale
└── viewmodels/                     one per screen area
```

### 8.1 Event flows

| Event | Path |
|---|---|
| App launch | `SarimApplication` → `LocaleUtils.applyDefaultLocale` → register package receiver → preload app list → `CommitmentLock.enforce` + `MandatoryProtection.enforce` → `MainActivity` gate |
| Accessibility event | `onAccessibilityEvent` → active word check for focused editable field; if content changed in a monitored app, throttled visual scan; passive word check per package |
| Visual scan | walk tree → candidate regions → `takeScreenshot` → crop → `NsfwContentDetector` → threshold → close or suspend |
| Package added | package receiver → invalidate app cache; service receiver → Tor/torrent/browser policy and age-rating check |
| Preference change | service listener → `MandatoryProtection.enforce` → re-sync Tor/torrent suspensions |
| Settings action | Compose control → `RecoveryGate.run` → commitment check → code/session → action |
| Owner setup | `DeviceOwnerRepository` → Shizuku `dpm` → policy broadcast → `DeviceAdmin` persists result → UI reloads |

---

## 9. Data and persistence

All persistence is `SharedPreferences`; there is no database.

| Preference file | Contents |
|---|---|
| `recovery_prefs` | `recovery_code`, `forced_removal_active`, `forced_removal_accumulated`, `forced_removal_last_elapsed` |
| `security_prefs` | commitment keys, `access_challenges`, `tetris_minutes`, `tetris_sound`, `reading_source`, `reading_count`, `reading_custom_passages`, `shuffle_keyboard`, Tor/torrent suspension sets, admin result keys, debug keys |
| `visual_blocking_prefs` | `target_packages`, `threshold`, suspension settings |
| `blocked_words_prefs` | `blocked_words` (built-in ∪ user), `target_packages`, `passive_words_<package>` |
| `browser_prefs` | `blocked_lists_json` (built-in list + user lists) |
| `hidden_apps_prefs` | packages to re-hide when reinstalled |
| `app_ratings` | package → has explicit content |
| `visual_blocking_suspensions` | suspension deadlines per package |
| `onboarding_prefs` | whether the one-time carousel has been shown |

The recovery code and commitment are stored in plain preferences, not encrypted. The manifest has
`android:allowBackup="true"` with empty backup rules, so the default Android backup set includes
these preferences. That is discussed as a release risk in
[EVALUATION.md](EVALUATION.md#p0-5-backup-can-copy-the-recovery-code-off-the-device).

---

## 10. Permissions and manifest components

| Permission | Why | Notes |
|---|---|---|
| `INTERNET` | Fetch the Play Store age-rating page | The only network permission |
| `QUERY_ALL_PACKAGES` | Enumerate installed apps for blocking, Tor/torrent detection, browser policies, 18+ checks | Restricted permission on Google Play |
| `USE_BIOMETRIC` | Lock-screen authentication | Falls back to device credential |
| `REQUEST_DELETE_PACKAGES` | Undo an app installed through the APK updater | Scoped to the updater |
| `WRITE_SECURE_SETTINGS` | Declared for secure-settings access | Not referenced in the current Kotlin code; granted through Shizuku by the setup path |

Manifest components:

- **Activities:** `MainActivity` (launcher), `BlockedWordActivity`, `NsfwContentBlockedActivity`,
  `AccessibilityRequestActivity`.
- **Receivers:** `SarimDeviceAdminReceiver` (DeviceAdmin), `SarimPolicyUpdateReceiver` (policy
  broadcasts), `ApkUpdateResultReceiver` (not exported).
- **Provider:** `rikka.shizuku.ShizukuProvider` (exported, protected by
  `INTERACT_ACROSS_USERS_FULL`).
- **Service:** `SarimAccessibilityService` with `canRetrieveWindowContent` and
  `canTakeScreenshot`.

The accessibility configuration listens for `typeViewTextChanged`,
`typeWindowStateChanged` and `typeWindowContentChanged`, includes not-important views, and
retrieves interactive windows.

---

## 11. Security model and threat model

### 11.1 What it resists

- Casual disabling through app settings, because protection is re-asserted on launch and on
  service start.
- Uninstalling or force-stopping the app, via `DISALLOW_UNINSTALL_APPS` and
  `DISALLOW_APPS_CONTROL`.
- Switching to a VPN or a different private DNS, via the corresponding restrictions and the
  DNS enforcer.
- Sideloading a new browser, via `DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY` and the
  install-blocking fallback when accessibility is off.
- Safe mode and ADB, via `DISALLOW_SAFE_BOOT` and `DISALLOW_DEBUGGING_FEATURES`.
- Moving the clock to shorten the commitment, via `DISALLOW_CONFIG_DATE_TIME` and the
  monotonic-clock/watermark arithmetic.
- Tor, torrent clients, unsupported browsers and store-rated 18+ apps, via suspension.
- Adult sites in supporting browsers, via `URLBlocklist`, SafeSearch and the filtering DNS.
- Adult media on screen and adult words typed, via the accessibility layers.

### 11.2 What it cannot resist

- **Root, a custom ROM, an unlocked bootloader or a factory reset/reflash.** No on-device app can
  stop someone who controls the firmware. A factory reset also removes Device Owner.
- **A user who reveals or hands over the recovery code.** The app cannot distinguish a coerced
  code from a voluntary one.
- **A classifier miss.** If an app exposes no identifiable media region, or the score stays below
  the threshold, visual blocking will not catch it. The DNS, URL, word and 18+ layers are
  independent.
- **Unsupported browsers.** They are suspended, but an app that can render web content without
  declaring a browser intent or a managed-config capability may not be recognised.
- **The accessibility service being kept off.** The app suspends watched apps and blocks new
  installs, but existing apps and the DNS/URL layers are the remaining protection.
- **The 18+ rating layer without a Play Store.** Non-GMS devices have no store page to scrape.
- **A determined user with physical access and time** to use the 48-hour forced removal, or to
  restore a backup (see the evaluation).

### 11.3 Self-lockout

The design accepts self-lockout as a feature. If full restrictions are enabled and the recovery code
is lost, the device may be unable to change system settings or uninstall the app until the 48-hour
forced removal completes. A permanent commitment has no forced removal. This is stated in the
README and in the setup screen; it is the main reason the commitment choice must be deliberate.

---

## 12. Privacy and network behaviour

- No account, no ads, no ad SDK, no analytics, no crash-reporting SDK, no telemetry.
- The classifier runs locally. Screens are captured in memory for classification and are not
  written to disk or uploaded.
- The only outbound request is the age-rating lookup:
  `https://play.google.com/store/apps/details?id=<package>&hl=en&gl=BR`. Only the package name is
  sent. The result is cached locally.
- Shizuku runs on the device, not on a server. The app opens `shizuku.rikka.app` and the Play Store
  only through user-initiated buttons.
- Word lists, browser lists, the commitment and the recovery code stay in local preferences.

The README points to `https://sarim.app/privacy`. That URL returned **404** when checked on
10 November 2026, so the policy must be hosted before any public release.

---

## 13. Build, test and release

### 13.1 Toolchain

| | |
|---|---|
| JDK | 17–21 (AGP 8.13 rejects 22+; a default JDK 27 fails with `What went wrong: 27-ea`) |
| Android SDK | platform 36 + build-tools |
| Gradle | 8.13 via the wrapper |
| `local.properties` | `sdk.dir=...`, optional `dechainer.store*` signing keys |

`gradle.properties` currently pins `org.gradle.java.home=/home/sessarhi/.local/share/jdks/jdk-21.0.12.1+1`.
That path is machine-specific and must be removed or overridden before the project is built
elsewhere.

### 13.2 Commands

```bash
make doctor          # JDK, SDK, Gradle, device reachability
make build           # debug APK → app/build/outputs/apk/debug/app-debug.apk
make test            # JVM unit tests
make check           # test + build
make release         # minified release APK (signed only if local keys exist)
make playstore       # debug APK with PLAY_STORE_BUILD=true
make setup           # build + install + Device Owner + accessibility + launch
make deploy-adb      # same, on a USB phone
make logcat          # follow the Sarim log tag
```

`make setup` defaults to Waydroid. `make owner` checks eligibility (no other owner, one user, no
accounts) before running `dpm set-device-owner`; `make privileges` enables the accessibility
service and grants `WRITE_SECURE_SETTINGS`; `make status` prints the state.

The full device walkthrough, Waydroid caveats and troubleshooting are in
[BUILD_AND_RUN.md](BUILD_AND_RUN.md).

### 13.3 Tests

| Suite | What it covers |
|---|---|
| `SecurityManagerTest` | Recovery code, sessions, challenges, forced removal |
| `CommitmentLockTest` | Duration clamping, permanent mode, clock/reboot arithmetic |
| `MandatoryProtectionTest` | Idempotent enforcement of the built-in layers |
| `TorBlockTest`, `TorrentBlockTest` | Detection signals and negative cases |
| `StringsParityTest` | en/ar/pt key parity and the exact 324-entry count |
| `LocaleUtilsTest` | Arabic default, RTL, per-app locale |
| `NavigationViewModelTest` | Tab/back-stack behaviour |
| `OnboardingStateTest` | One-time flag |

Verified on 10 November 2026: **81 tests, 0 failures, 0 errors, 0 skipped**. The tests are JVM
tests (Robolectric where Android APIs are needed); there are no meaningful instrumentation or UI
tests, and the accessibility service is not covered.

### 13.4 Release build

- `assembleRelease` enables R8 minification and resource shrinking with the default ProGuard file
  and an almost-empty `proguard-rules.pro`.
- Signing is configured only when `dechainer.storeFile`, `dechainer.storePassword`,
  `dechainer.keyAlias` and `dechainer.keyPassword` exist in `local.properties`. When they do, the
  **debug** build is also signed with that key.
- `-PplayStoreBuild=true` writes `BuildConfig.PLAY_STORE_BUILD`; it removes the APK-update entry
  point but does not remove Device Owner, accessibility, `QUERY_ALL_PACKAGES` or app suspension.
- There is no product flavor, no Android App Bundle task configuration and no CI workflow.

---

## 14. Localization and accessibility

- **Locales:** Arabic (default), English and Portuguese. Each `strings.xml` has 324 string/plural
  entries, enforced by `StringsParityTest`.
- **RTL:** the manifest enables RTL; Compose layouts use start/end padding and arrangements.
- **Arabic word matching:** the forbidden-word list includes Arabic terms, and matching is
  boundary-aware; the app also includes an Arabic Quran source for reflective reading.
- **Accessibility service:** it is the enforcement mechanism, not an accessibility feature for
  disabled users. The app's own Compose UI uses standard Material 3 semantics, but the service's
  broad permissions (`canRetrieveWindowContent`, `canTakeScreenshot`) are a review focus on any
  store.

---

## 15. Licensing and attribution

- **Project license:** Apache License 2.0, retained verbatim. Fork of Déchaîner, with the original
  notices in [NOTICE](../NOTICE).
- **Bundled model:** `app/src/main/assets/models/nsfw_model.tflite`, described in NOTICE as coming from
  the upstream project, with no runtime network access. The model's own license and provenance
  should be confirmed before redistribution.
- **Reading passages:** KJV Bible and João Ferreira de Almeida (public domain); Quran translations
  attributed to Saheeh International and Samir El-Hayek, with Arabic text sourced from
  Quran.com (QuranEnc/QUL licensing). The per-source attribution is in `ReadingPassages.kt`.
  The licensing of the English and Portuguese Quran translations needs legal confirmation.
- **Philosophical quotations:** public-domain English translations, listed individually in
  `ReadingPassages.kt`.

---

## 16. Known limitations

1. **Device Owner is mandatory in practice.** The setup flow does not let the user reach the tabs
   without it, even though older documentation says the app can run with only accessibility.
2. **Setup requires removing all accounts** (or a factory reset) before `dpm set-device-owner`.
   This is Android's rule, not the app's, but it makes the install flow unsuitable for most
   consumers.
3. **The 48-hour forced removal does not count powered-off time** (see the evaluation).
4. **The recovery code is stored in plain preferences and is eligible for Android backup.**
5. **The age-rating layer depends on scraping a Play Store page.** It does nothing without a store
   or network, and it is brittle when Google changes the page.
6. **Visual blocking depends on the accessibility tree.** Compose apps that expose no media
   semantics may be missed; the geometric fallback is a heuristic.
7. **A browser that does not support managed restrictions is suspended**, which may be surprising
   for users of niche browsers.
8. **The 18+ layer only acts on "explicit content" plus "Rated 18+"**; an app rated 18+ for
   violence or gambling is not suspended.
9. **No crash reporting or diagnostics by design**, which makes field support harder.
10. **Store metadata and release engineering are incomplete.** See
    [EVALUATION.md](EVALUATION.md) for the publication checklist.

---

## 17. Appendix

### 17.1 Features removed from upstream

Focus mode (Pomodoro), focus schedules/reminders/notifications, per-app daily/weekday limits, time
windows, reopening cooldowns, usage tracking, usage warnings, app groups, colour filters, impulse
lock/panic button, activity blocker, and the old opt-in strictness lock. Their screens, models,
services, receivers, strings, permissions and notification layouts were removed with them.

### 17.2 Key invariants to preserve when changing the code

- `MandatoryProtection.enforce()` must stay idempotent and must remain the single source of truth
  for the built-in layers.
- `CommitmentLock` must never gain a `stop()` or a settable duration.
- The recovery code must remain the only ordinary way to weaken an adjustable layer.
- The built-in browser list, forbidden words and the default monitored app must be unioned back,
  never replaced; the visual categories and escalation are hard-wired on.
- New gated actions must go through `RecoveryGate`, with `strengtheningAction = true` only when the
  action cannot weaken protection.
- Any new preference that affects a protection layer must be re-asserted by `enforce()`.
- New user-visible strings must be added to all three locales; update the count in
  `StringsParityTest`.
