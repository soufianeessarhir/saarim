# Sarim — feature reference

Sarim does one job: keep pornography and adult content off an Android phone, and keep the way back
in closed. Every other kind of self-control feature — focus timers, app time limits, usage
warnings, app groups, colour filters, an impulse button — was removed on purpose. What is left is
a small number of layers that are all on from the first launch, plus a commitment that freezes
them for a period the user can end early only through a code-confirmed order that waits seven days.

This document describes the app as it is built: what each layer does, its defaults, what it
needs, and exactly which actions are gated by the recovery code and which by the commitment.
Only settings that touch adult-content protection are gated; the rest are ordinary settings.

### The hardening-first design

Everything that keeps adult content out is on by default; the recovery code decides only whether
an *existing* protection may be tuned or weakened. Concretely:

- **No secret code needed** for settings that do not touch a protection layer: language, keyboard
  layout, the onboarding carousel, and the forced-removal timer. They never open the code dialog.
- **A 10-minute session unlocks the adjustable parts of the protection screens** — extra monitored
  apps and the visual sensitivity (the built-in app and categories stay fixed), the per-app passive
  word lists, extra URL lists and per-browser suspension, the DNS choice among the three filtering
  resolvers, the factory-reset block, enabling advanced blocking, and changing the recovery code
  itself. The session is
  opened by the dialog in `RecoveryGate` and is renewable; while it is active the dialog does not
  reappear.
- **The commitment overrides the session.** While a commitment is running, `RecoveryGate` refuses
  every gated change, even with the code, except actions that only add protection
  (`strengtheningAction = true`): enabling advanced blocking and suspending a browser.
  The one deliberate exception is the release order: it sits outside `RecoveryGate`, is confirmed
  with the code, and cannot end anything until its seven-day wait has passed.
- **Tor/Orbot and torrents are on by default and cannot be cancelled.** They are shown as status
  rows on the Protection tab; Config has no toggle for either, and `enforce()` keeps the backing
  preferences true.
- **Any source that could fetch pornography is blocked by default and not cancellable.** That covers
  the built-in adult-site browser list, forced SafeSearch, the three filtering DNS hosts, Tor and
  torrents, the developer/unsafe-source routes, and — on the adult side only — the commit lock
  itself once it is running.

The recovery code is the only ordinary way to *weaken* an adult-protection layer later, which is
exactly what the code was added to guard. It cannot shorten the commitment, and it can end one only
through the seven-day release order.

### What “default and not cancellable” means in the code

`security/MandatoryProtection.enforce()` is the single place that re-asserts all of it. It is
idempotent in both directions — it writes only what is out of line and returns whether it had to
write anything — so it runs on every app launch (`SarimApplication`), on every accessibility
service start, on `CommitmentLock.enforce`, and from the preference listener in the service.

There are no "off" switches for the layers below. Tor and torrents still have preference keys so
the service reads a truthful value; `enforce()` puts them back and there is no UI setter.

---

## 1. Setup, in order

First run is a fixed sequence:

1. **The "How it works" tour** — five slides, before anything is generated, so the app is
   understood before it starts making decisions for the user.
2. **A recovery code** — generated in-app, written on paper, handed to someone else if possible.
3. **Device owner and advanced blocking** — the two Android powers the enforcement relies on,
   granted through [Shizuku](https://shizuku.rikka.app/) and re-checked on every launch.
4. **The protection review** — `SetupProtection` lists the always-on layers, lets the user pick the
   filtering DNS host and add the factory-reset block, and requires one confirmation.
5. **The commitment** — Regular (no lock), a timed period, or permanent.

The protection review comes before the commitment because the commitment is what freezes those
choices. Confirming the setup first is what makes the lock meaningful, and it keeps the duration
picker from doubling as a settings screen.

### 1.1 Device owner

`DeviceOwnerRepository` runs `dpm set-device-owner com.sarim.app/.SarimDeviceAdminReceiver`
through Shizuku and then waits for the authoritative `DEVICE_POLICY_CHANGED` broadcast
(`SarimPolicyUpdateReceiver` → `DeviceAdmin.persistResult`) instead of trusting the shell exit code.

Device owner is what unlocks, at the OS level:

- the `UserManager` restrictions in §2.7,
- a private DNS host (§2.4),
- `setPackagesSuspended` — suspending Tor, torrents, unsupported browsers and 18+ apps (§2.5, §2.6),
- `setApplicationRestrictions` — the `URLBlocklist` and `ForceGoogleSafeSearch` policies (§2.3).

Without it the first-run gate stops at the owner screen: the commitment and the tabs are not
reachable, and the Protection tab would show an incomplete-protection card if it could be opened.

### 1.2 The commitment

`security/CommitmentLock.kt`.

| | |
|---|---|
| Presets | **Regular** (no lock), 90, 180, 365, 730 days, or **permanent** |
| Accepted range | 90 – 3650 days for a timed commitment (values below the floor are raised to 90); `0` selects Regular |
| Stored as | `security_prefs` → `commitment_start_rtc`, `commitment_start_elapsed`, `commitment_duration_ms` (`-1` = permanent, `-2` = regular, `-3` = ended early), `commitment_last_seen_rtc`, plus the release-order keys |
| Chosen at | first after the protection review, before the app opens; later changes only extend it |
| Release | the period running out; a code-confirmed release order after a 7-day wait (timed commitments only); or the 48-hour forced removal — **not offered for a permanent or regular setup** |

`start()` creates the commitment and `hasStarted()` guards it: it is never restarted, shortened or
replaced at setup. `changeDuration()` is the only later entry point, and it is deliberately
one-way: it extends a running period (counted from today), turns a Regular or ended commitment
into a real one, or makes a timed commitment permanent. It refuses a shorter period, refuses to
change a permanent commitment, and drops any pending release order because that order was placed
against the old deadline. Config exposes it on the commitment row.

**The release order.** A timed commitment can be ended early in three stages:

1. **Order** — Config → *End commitment early* explains the wait, then asks for the recovery code.
   `requestDisable()` records the order and starts a 7-day timer.
2. **Wait or cancel** — the timer is recomputed from `elapsedRealtime` plus a wall-clock watermark,
   so a reboot or a date change cannot skip it. Cancelling (`cancelDisableRequest()`) needs no code,
   drops the order, resets the timer, and leaves the commitment exactly as it was.
3. **Finish** — once the wait is over, a second confirmation with the recovery code calls `stop()`,
   which records `-3` and lifts the settings freeze. Protection stays on. `canStartForcedRemoval()`
   then returns false: there is nothing left to escape.

A permanent commitment cannot place an order; a regular setup has nothing to end.

**What it freezes.** While `isLocked()`:

- every action that goes through `RecoveryGate` is refused, *including* one a recovery session
  would normally authorise — the recovery code does not override the commitment. This covers
  browser lists, DNS, visual-blocking changes, browser suspension, the
  accessibility switch being turned *off*, and changes to the recovery code itself;
- the only bypass is `strengtheningAction = true`, used by the few controls that can only add
  protection: enabling advanced blocking, blocking an app, suspending an app;
- the release order is the other exception, and it sits outside `RecoveryGate` on purpose: it takes
  the recovery code directly, records a 7-day wait, and cannot stop anything before that wait ends.

**Telling time.** The deadline is recomputed, never stored as a countdown:

- `elapsedRealtime` is authoritative while the device stays up — changing the date does nothing;
- after a reboot (`elapsedRealtime` going backwards) the wall clock takes over;
- a watermark (`commitment_last_seen_rtc`) records the furthest date ever seen, so setting the date
  *backwards* after a reboot cannot buy back time;
- the wall-clock reading is capped at the deadline, so an absurd jump forward cannot make the
  remaining time negative either.

A permanent commitment reports `Long.MAX_VALUE` remaining, `0` as an end time,
`SecurityManager.canStartForcedRemoval()` returns false, and cannot place a release order.

### 1.3 Recovery code and sessions

`security/SecurityManager.kt`, `screens/common/RecoveryCodeWizard.kt`.

- 16 random characters from `A–Z`, generated with `SecureRandom`.
- The wizard: generate → write it down → type it back from the paper letter by letter → choose
  where to keep it, with a one-tap option to send it to an accountability partner. Two dialogs
  confirm and regenerate it later (`RecoveryGenerateDialog`, `RecoveryConfirmDialog`).
- Entering it opens a **10-minute session** (`SecurityManager.sessionEndTime`, renewable, shown as
  a countdown pill in the top bar). Every gated action calls `RecoveryGate.run`, which asks for the
  code only when no session is active.
- The session never weakens the protection layers themselves: it only allows editing settings that
  the layers leave adjustable, and only when the commitment is not running.

---

## 2. The protection stack

`security/MandatoryProtection.enforce()` is the single place that re-asserts all of it. It is
idempotent in both directions — it writes only what is out of line and returns whether it had to
write anything — so it runs on every app launch (`SarimApplication`), on every accessibility
service start, on `CommitmentLock.enforce`, and from the preference listener in the service.

There are no "off" switches for the layers below. Two of them still have preferences, kept so the
service reads a truthful value; `enforce()` puts them back:

```kotlin
fun isBlockTorEnabled(): Boolean = true        // no setter
fun isBlockTorrentsEnabled(): Boolean = true   // no setter
```

### 2.1 Visual blocking

`SarimAccessibilityService` + `utils/NsfwContentDetector.kt` (LiteRT / MobileNetV2).

| | |
|---|---|
| Model | `nsfw_model.tflite`, bundled, 224×224 input, on-device only |
| Labels | `drawings`, `hentai`, `neutral`, `porn`, `sexy` |
| Categories classified | **all four adult ones**, always (`VisualBlockingSettings.DEFAULT_CATEGORIES`); `neutral` is never in the list |
| Monitored apps | `com.reddit.frontpage` by default and **not removable**; any other app can be added |
| Sensitivity | 0.5 default; adjustable 0.3 – 0.7 while a session is active |
| Scan throttle | at most one scan per 2 s, never overlapping |

How a scan happens:

1. On a content-changed event in a monitored app, the accessibility tree is walked for likely media
   views — view class names (`Image`, `Photo`, `Video`, `Media`, `Player`, `Gif`, `TextureView`,
   `SurfaceView`, `WebView`), accessibility vocabulary (`foto`, `photo`, `imagem`, `video`, …),
   Reddit's post-card description pattern, and finally a geometric fallback (unlabelled leaf nodes
   with a plausible photo/video aspect ratio, capped at 5 candidates).
2. Only those regions are screenshotted and cropped — not the whole screen — so the classifier sees
   the media rather than a page of text and buttons.
3. Each crop is classified; the highest score across the adult categories is compared with the
   threshold. Above it, the app is closed.

**Escalation is always on.** Detections are counted per package in a sliding window
(`VisualBlockingSuspensionTracker`, persisted). When an app is blocked 5 times within 15 minutes it
is **suspended for 30 minutes** instead of just closed; the release timer is re-armed after a
service restart from the persisted deadline. Below the threshold the app is suspended and
immediately un-suspended, which force-closes it without leaving it blocked.

Windows that are closed by the flanking screens: the blocked-content screen
(`NsfwContentBlockedActivity`) shows how long a suspension lasts.

### 2.2 Forbidden words

`security/MandatoryProtection` seeds `data/ProtectionDefaults.FORBIDDEN_WORDS` — English, Arabic and
Portuguese adult vocabulary, matched case-insensitively on word boundaries (`(?<![\p{L}\p{N}_])`).
The list is unioned back in on every launch. It is extended for all three languages, hidden from
the UI, and cannot be edited or removed; only the per-app passive lists are user-editable.

Two modes, per app:

- **Active** — after the user stops typing in an editable field of a selected app, the text is
  checked and the app is sent `GLOBAL_ACTION_BACK` with the word shown on
  `BlockedWordActivity`. Every installed browser is always in this list (re-added by
  `enforce()`), so an address bar or a search query is covered.
- **Passive** — per app, an optional extra list checked against the text *and* content descriptions
  currently on screen; a match closes the app the same way.

Matching is conservative on purpose: the screen text is only checked for words that are actually
configured, and the built-in list avoids words that appear in ordinary health or news content.

### 2.3 Adult sites in browsers

`data/BrowserRestrictionsManager.kt` + `data/ProtectionDefaults.ADULT_SITES`.

- Every installed app that resolves `CATEGORY_APP_BROWSER`, `http://` or `https://` is treated as a
  browser.
- For browsers that declare `URLBlocklist` and `ForceGoogleSafeSearch`
  (`RestrictionsManager.getManifestRestrictions`), a managed configuration is pushed with the
  built-in list (~80 adult domains, matched with their subdomains) plus every user list, and
  `ForceGoogleSafeSearch = true`.
- The built-in list lives under the reserved id `sarim-mandatory-adult-sites`: it is re-written on
  every launch, it cannot be edited or deleted in the UI, and `applyRestrictions()` unions the
  domains in regardless of what the preference file says.
- A browser that does not support managed restrictions is **suspended** instead of being left
  unrestricted — both when it is first seen and when it is installed while the service is running
  (the package-added receiver).
- Every newly installed browser gets the policy immediately, before it can be opened.

### 2.4 Filtering private DNS

`MandatoryProtection.ensureFilteringDns`, `DeviceOwnerRepository.setPrivateDNS`.

Only three hosts are accepted:

| Host | Label |
|---|---|
| `family.cloudflare-dns.com` | Cloudflare Family |
| `family.adguard-dns.com` | AdGuard Family |
| `adult-filter-dns.cleanbrowsing.org` | CleanBrowsing Adult |

There is no "automatic", no "off" and no free-text host, and the OS setting itself is closed by
`DISALLOW_CONFIG_PRIVATE_DNS`. On every launch, if the current mode is not "hostname" or the host
is not one of the three, it is replaced with `family.cloudflare-dns.com`; the picker
(`DnsSelectionDialog`) only offers the three.

### 2.5 Tor, Orbot and torrents

Detection (`BrowserRestrictionsManager`):

- **Tor** — three signals, in order: the official/legacy package ids
  (`org.torproject.torbrowser`, `…android`, `…torservices`, `info.guardianproject.orbot`, …); a
  normalized identity match on package name *and* visible label (`torbrowser`, `torproject`,
  `orbot`, `orfox`, `onionbrowser`), which catches rebuilds and rebrands; and a cached sweep of all
  installed apps.
- **Torrents** — every app that handles a `magnet:` link or an `application/x-bittorrent` payload,
  probed through five intent shapes so clients that only register one of them are still found.

Enforcement: anything detected is suspended via `setPackagesSuspended`, recorded in
`security_prefs` (`tor_suspended_packages`, `torrent_suspended_packages`), and swept again on every
service start — so a client that was already installed before setup is caught too. Installation of
a new Tor/torrent client produces the package-added broadcast, which suspends it on the spot.

### 2.6 Apps the store rates 18+

`data/PlayStoreRatingFetcher.kt`, checked from the accessibility service:

- when a package is installed (package-added receiver), and
- the first time an app is opened, if it has no cached rating.

If the store reports explicit content **and** `Rated 18+`, the app is suspended. The rating is
cached in `app_ratings` so it is fetched once; a 404 is cached as "not explicit". Offline (or a
store page that cannot be fetched) means no action — the visual, word and DNS layers still apply.

### 2.7 System restrictions

`MandatoryProtection.RESTRICTIONS`, applied whenever the admin is active and never removable:

| Restriction | What it closes |
|---|---|
| `DISALLOW_CONFIG_VPN` | pointing the device at a VPN that skips the filtering DNS |
| `DISALLOW_CONFIG_PRIVATE_DNS` | switching the resolver back |
| `DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY` | sideloading a browser or an app the store would not hand out |
| `DISALLOW_UNINSTALL_APPS` | uninstalling the blocker |
| `DISALLOW_APPS_CONTROL` | force-stopping or clearing it |
| `DISALLOW_SAFE_BOOT` | booting into safe mode, where third-party apps are off |
| `DISALLOW_DEBUGGING_FEATURES` | attaching ADB or a debugger |
| `DISALLOW_CONFIG_DATE_TIME` | moving the clock a commitment is measured against |

The only optional restriction is `DISALLOW_FACTORY_RESET`, offered on the **System restrictions**
screen. Everything else Android exposes as a `UserManager` policy — volume, Bluetooth, calls, SMS,
accounts and so on — is deliberately not offered, because it does not protect the content block.
The eight above are always on and are not listed as editable checkboxes.

### 2.8 Advanced blocking (accessibility)

The switch lives in Settings and is applied through Shizuku
(`DeviceOwnerRepository.changeAccessibilityPermission`), i.e. it can be granted without touching
Android's accessibility settings. It is what runs visual blocking, word blocking, blocked-app
enforcement against typed text, and the media scan.

When the service goes down without being asked to (`onUnbind` with `disablingService == false`),
it suspends the apps that were being watched for words and applies `DISALLOW_INSTALL_APPS` until it
is back, then opens `AccessibilityRequestActivity` to ask for it again. Turning it back on is
allowed even during a commitment, since it only adds protection.

---

## 3. Getting in

`activities/LockScreen.kt`.

1. The lock screen opens showing the commitment (days left, "Permanent", or "Regular"). Nothing
   else is behind it yet.
2. **Access app** → biometric prompt, falling back to the device credential, falling back to no
   check at all if the device has neither configured.
3. Then any access challenges (`SecurityManager.getAccessChallenges`), run in sequence. The default
   is now none: the biometric or device-credential prompt is the gate. The old 32-word typing
   challenge is removed; the Math, Tetris and reflective-reading screens remain in the code but no
   settings entry selects them.
4. Every challenge screen has *give up*, which returns to the lock screen without opening the app.

APK updates of already-installed apps are offered here without authentication (only when unknown
sources are restricted, so the APK path exists at all). The Play Store build disables that path
(`BuildConfig.PLAY_STORE_BUILD`).

---

## 4. Automatic app suspension

There is no app-management screen. The old Apps tab was a holdover from the time-management
features and has been removed together with its hide/uninstall-lock controls. What remains runs
without user curation:

- apps the Play Store rates 18+ **and** flags with explicit content are suspended when installed,
  or the first time they are opened when no rating is cached;
- Tor/Orbot and torrent clients are suspended by the accessibility service (§2.5);
- a browser that cannot take managed restrictions is suspended, and its switch stays on the
  Browser restrictions screen.

Suspensions use `setPackagesSuspended`; there is no per-app block or uninstall lock.

---

## 5. Tabs and settings

The app has three root tabs: **Home** (protection status, commitment, prerequisites and the layer
summary), **Protection** (the incomplete-setup warning and the layer list) and **Config** (the
settings below).

| Row | What it does |
|---|---|
| Commitment | days left, *Permanent*, *Regular (no commitment)* or *Ended early*; tap to extend it or make it permanent — never to shorten it |
| End commitment early | the 7-day release order for a timed commitment: code to order, cancel at any time, code again when the wait ends |
| Device owner | state badge; opens the Shizuku setup screen |
| Advanced blocking | accessibility switch (gated; enabling is allowed during a commitment) |
| Visual blocking | monitored apps; sensitivity and suspension thresholds, unlocked with the recovery code and saved explicitly; all four categories are fixed |
| Blocked words | which apps are monitored and the per-app passive lists; the built-in list is hidden |
| Browser restrictions | installed browsers, per-browser suspension, user URL lists; the built-in list is hidden and always applied |
| Private DNS | the three filtering resolvers |
| System restrictions | a status line for the mandatory eight, plus the optional factory-reset block |
| How it works | the 5-slide onboarding carousel again |
| Language | English, Portuguese, Arabic (Arabic is the default) |
| Keyboard | normal or shuffled keypad for the recovery code |
| Change recovery code | regenerate, gated |
| Forced removal | 48-hour timer; hidden for a permanent or regular setup |

---

## 6. Forced removal

`SecurityManager.startForcedRemoval` / `getForcedRemovalRemainingTime` — accumulated with
`elapsedRealtime`, so a reboot does not pause it, capped at 48 hours. When it reaches zero, device
owner can be removed without the recovery code, and the app can be uninstalled.

It is cancellable before it completes, and it is **not offered at all for a permanent or regular
setup, nor once a commitment has ended early** (`canStartForcedRemoval` returns false and the
Settings row is hidden). It is also no longer the only early exit for a timed commitment: the 7-day
release order ends one while the recovery code is still available, without removing Device Owner.
Forced removal stays the path for a lost code, which is why it needs no code at all. Between
"permanent" and a very long period there are now two differences: no forced removal and no release
order, and the setup screen says so before the choice is made.

---

## 7. What is gated and what is not

Gating is done by `screens/common/RecoveryGate.kt`. An action either opens the code dialog (when no
session is active), runs through an active session, or never asks at all.

**Never asks for the code:**

- language;
- keyboard layout;
- opening the onboarding carousel again;
- the forced-removal timer (when the commitment is timed);
- cancelling the release order (only the order and its final confirmation need the code);
- reading any screen, including the commitment, device-owner state and every current setting.

**Asks once, then runs for 10 minutes (renewable):**

- visual blocking: adding/removing monitored apps and changing the sensitivity; the built-in app
  and the four adult categories cannot be changed at all;
- forbidden words: which apps are monitored and the per-app passive lists;
- browser restrictions: extra URL lists, attaching them to browsers, suspending a browser;
- private DNS: switching among the three filtering resolvers;
- system restrictions: adding or removing any optional `UserManager` policy;
- advanced blocking: turning accessibility on (turning it off is refused by the commitment);
- the recovery code itself (changing it needs the current one).

**The release order:** while a timed commitment is running, Config → *End commitment early* asks for
the recovery code directly, not through the session gate — it has to work precisely when everything
else is frozen. The code starts the 7-day wait, the same code (or a still-active session) ends the
commitment once the wait is over, and cancelling the order never asks for a code.

**Refused even with an active session:** every one of the gated actions above while a commitment is
running — except enabling advanced blocking and suspending a browser, which only make the phone
stricter, and the release order described above. The dialog explains that the commitment
is locked.

The built-in layers (visual blocking itself, its categories, the forbidden-word seed list, the
built-in adult-site list, SafeSearch, the DNS host being one of the three allowed ones, Tor and
torrent blocking, and the eight mandatory restrictions) have no control at all, so no code can
change them.

---

## 8. What is no longer in the app

Removed, along with their screens, models, services, receivers and strings:

| Removed | Why |
|---|---|
| Focus mode (Pomodoro), focus schedules, focus reminders, focus notifications + foreground service | productivity, not adult-content blocking |
| Per-app daily/weekday limits, time windows, reopening cooldowns, usage tracking | screen-time management |
| Usage-warning notifications and stages | belongs to the time-limit feature |
| App groups | only existed to share limits and windows |
| Colour filters (grayscale, night light, extra dim, inversion) | comfort/sleep feature |
| Impulse lock and the panic button | replaced by the commitment; its 15–360-minute timer was a time-management tool |
| Activity blocker (blocking individual screens by class name) | short-video/social-media blocking |
| Apps screen with per-app hide, suspend and uninstall lock | dead code from the time-management features; automatic suspension covers the job |
| Strictness lock | superseded by the commitment: timed, permanent, or Regular (no lock) |

Also removed with them: `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_SPECIAL_USE` and `POST_NOTIFICATIONS` from the manifest, and the
`notification_focus` / `notification_usage_warning` layouts with their icons.

---

## 9. Fixed, adjustable, and gated

**Fixed — no UI can change it:**

- visual blocking on, all four adult categories, suspension escalation on;
- the built-in forbidden words and the built-in adult-site browser list;
- SafeSearch forced on every supporting browser;
- Tor/Orbot and torrent blocking;
- the eight system restrictions;
- the commitment itself, once started — a timed one can only be ended through the 7-day release
  order, a permanent one not at all.

**Adjustable — by an authenticated session, and only when no commitment is running:**

- which extra apps are monitored visually (the default one cannot be removed);
- visual sensitivity within 0.3 – 0.7, suspension counts/windows/durations;
- additional blocked words, per-app passive lists;
- extra URL lists, suspending a particular browser;
- the DNS choice among the three filtering resolvers;
- extra `UserManager` restrictions;
- blocking/suspending any app (blocking stays available even during a commitment);
- the recovery code itself;
- language and keyboard layout (not gated);
- the 48-hour forced removal timer (not gated; unavailable when permanent or regular, or after an
  early end);
- the 7-day release order for a timed commitment (code-confirmed, cancellable at any time).

---

---

## 10. Where the state lives

| Preference file | Contents |
|---|---|
| `recovery_prefs` | recovery code, forced-removal state |
| `security_prefs` | commitment and release-order keys, access challenges, Tor/torrent suspension sets, admin result, shuffle keyboard |
| `visual_blocking_prefs` | monitored packages, threshold, suspension settings |
| `blocked_words_prefs` | `blocked_words` (built-in ∪ user), `target_packages`, `passive_words_<package>` |
| `browser_prefs` | `blocked_lists_json` (built-in list + user lists) |
| `app_ratings` | package → has explicit content |
| `visual_blocking_suspensions` | suspension deadlines per package |
| `onboarding_prefs` | whether the one-time carousel has been shown and the protection setup confirmed |

---

## 11. Requirements and limits

- **Android 11+.** The screen capture used by visual blocking needs the accessibility
  `takeScreenshot` API (30+), and `setPackagesSuspended`/private DNS need a modern `DevicePolicyManager`.
- **Device owner is effectively mandatory.** Without it the app can still read words and scan
  images, but cannot suspend apps, filter DNS, or hold the system restrictions — the Protection tab
  says so until it is granted.
- **Advanced blocking is required** for every content layer. With it off, the `onUnbind` path
  suspends the word-watched apps and blocks app installs until it is back on.
- **A rooted device, a custom ROM, or a factory reset are outside any app's reach.** Nothing on the
  phone can stop someone who can reflash it. Within Android's own options, the restrictions above
  close the usual routes: VPN, resolver, sideload, uninstall, force-stop, safe mode, ADB and the
  clock.
- **The classifier can miss.** It sees the regions the accessibility tree exposes; an app that
  renders media in a way the heuristics cannot identify, or content that scores below the
  threshold, will not be caught by this layer. The DNS, URL, word and 18+ layers are independent of
  it.
- **The Play Store build** has no self-update path (`PLAY_STORE_BUILD`).
- **Waydroid** (used for testing in `docs/BUILD_AND_RUN.md`) has no real Play Store, so the 18+
  rating check finds nothing there, and screenshot-based scanning depends on the host GPU.
