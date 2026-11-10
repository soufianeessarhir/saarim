# Sarim — Evaluation and Publication Readiness

**Evaluated version:** 1.8.0 (versionCode 18)
**Date:** 10 November 2026
**Method:** source inspection of the full app module, build/test run, asset and metadata
inventory, and a privacy-policy URL check. No on-device session, no Play review and no legal
review were performed.

See [PROJECT.md](PROJECT.md) for the feature and architecture reference.

---

## 1. Verdict

**As an engineering project:** this is a thoughtful, coherent implementation of a hard idea. The
enforcement loop is idempotent, the commitment clock handles clock tampering unusually well for a
consumer app, the built-in layers are genuinely non-removable, and the localisation work is real
(three languages at exact key parity, Arabic as the default). It is well above the usual quality of
a self-control app.

**As a publishable product:** it is not ready. The code is closer to done than the release is.
There is no signed release pipeline, no store copy or feature graphic, no CI, the privacy policy URL
is dead, the recovery code can be copied off the device by Android backup, and the 48-hour escape
hatch does not count time spent powered off. Those are fixable.

**As a Google Play app:** the current product is a poor fit. It requires Device Owner, an
Accessibility Service with screenshot access, `QUERY_ALL_PACKAGES`, the ability to suspend other
apps, and an uninstall lock — and its setup requires removing all accounts from the phone first.
Even with the `PLAY_STORE_BUILD` flag, Google Play review is a high-risk path. A Play release would
need a separate, weaker product variant, not a build flag.

**Recommended path:** ship a signed APK through GitHub Releases (with Obtainium support) to the
audience that will actually complete the Shizuku/Device Owner setup, fix the P0 items below, and
treat Google Play as a later, separate product decision.

---

## 2. What was actually verified

| Check | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | **81 tests, 0 failures, 0 errors, 0 skipped** (9 test classes) |
| Version and SDK | 1.8.0 / versionCode 18; minSdk 30; target/compile 36 |
| Source size | 64 Kotlin files in `app/src/main`; 922-line accessibility service |
| Localisation | 324 string/plural entries per locale; en/ar/pt parity test exists and passes |
| Privacy policy | `https://sarim.app/privacy` → **HTTP 404** |
| Store metadata | `fastlane/` contains only images: 512×512 icon and 7 screenshots (540×1052) for en-US, an icon for ar-SA. No titles, descriptions, changelogs, feature graphic or pt-BR assets |
| CI | No `.github/` workflows or other CI configuration |
| Release signing | Configured only from `dechainer.store*` in `local.properties`; no release key in the repo (correct), no AAB configuration |
| Instrumented tests | One template test only; no UI/service tests |
| On-device behaviour | **Not verified** — no phone or Waydroid session was run |

The unit tests cover the security manager, commitment arithmetic, mandatory protection, Tor and
torrent detection, locale handling, navigation and onboarding state. They do **not** cover the
accessibility service, browser policy application, visual scanning, Compose screens or the
Shizuku/DPM integration.

---

## 3. Readiness scorecard

Scores are for release readiness, not for ambition. 5 = ready, 1 = blocking work remains.

| Area | Score | Comment |
|---|---:|---|
| Core functionality | 4.5 | The feature set is complete and coherent; layers are wired together |
| Security design | 4.0 | Strong intent and good enforcement; backup, plaintext code and timer bug weaken it |
| Robustness / edge cases | 3.0 | Several untested integrations; scraping and OEM behaviour are fragile |
| User experience | 3.5 | Clear first-run flow and lock screen; setup is technical and account removal is harsh |
| Accessibility of the app UI | 3.5 | Material 3 and RTL; no dedicated screen-reader review |
| Privacy | 4.5 | No telemetry, on-device ML; backup and Play-page fetch must be disclosed/handled |
| Code quality | 3.5 | Clear packages and comments; the 922-line service and blocking sleeps are weak spots |
| Test coverage | 3.0 | Good unit coverage of pure logic; no instrumentation/service coverage |
| Documentation | 3.5 | Extensive, but drifted from the code in several places |
| Release engineering | 1.5 | No signing, CI, changelog, AAB or reproducible builds |
| Store compliance | 1.0 | Missing assets and policy conflicts; Play is unlikely without a product change |
| Legal / licensing | 2.5 | Apache 2.0 is clean; model and Quran translation licenses need confirmation |

---

## 4. Strengths worth preserving

1. **Idempotent enforcement.** `MandatoryProtection.enforce()` is the one place that re-asserts the
   stack, writes only what is out of line, and runs on launch, service start and preference
   changes. This is the right architecture for an anti-bypass app.
2. **The commitment clock.** Mixing `elapsedRealtime` with a wall-clock watermark and capping the
   wall clock at the deadline handles both clock directions and reboots better than most apps of
   this kind.
3. **No off switches.** Tor, torrents, the built-in word list, the adult-site list, SafeSearch, the
   DNS hosts, the four visual categories and the eight restrictions really are non-cancellable in
   code; the UI shows them as fixed rather than pretending.
4. **Thoughtful identity detection.** Tor is matched by package id, normalised package name *and*
   visible label, with a sweep of already-installed apps. That is more robust than a hardcoded list.
5. **Localisation discipline.** 324 entries in three languages, an Arabic default, RTL, and a
   parity test that fails on drift. A rare thing in a small project.
6. **Recovery-by-design.** The paper-code wizard, the share-to-accountability-partner option and
   the 48-hour forced removal are a coherent answer to the "what if I lose it?" problem.
7. **Privacy stance.** No analytics, no ads, no crash SDK, no account, on-device classifier, and a
   short outbound-request list that is documented.

---

## 5. Blocking issues (P0) — fix before any public release

### P0-1. The privacy policy URL is dead

**Evidence:** `https://sarim.app/privacy` returns HTTP 404.
**Impact:** Google Play requires a live privacy policy; even for direct distribution, the README
promises one.
**Fix:** Host a real policy at a stable URL and update the README and store listing. It must cover
the Play Store age-rating request (package name sent to Google), Shizuku's local role, the on-device
model, the absence of analytics, and Android backup (see P0-5).

### P0-2. There is no release pipeline

**Evidence:**

- signing is configured only from `dechainer.store*` keys in `local.properties`, with no documented
  key-generation or CI path;
- `gradle.properties` pins `org.gradle.java.home=/home/sessarhi/.local/share/jdks/jdk-21.0.12.1+1`,
  a machine-specific path that breaks builds on every other machine;
- there is no CI workflow, no changelog, no versioning policy, no documented AAB/Play publishing
  path and no reproducible-build instructions;
- `make release` produces a minified APK, but the ProGuard file is the default template, so the
  Shizuku/reflection and LiteRT paths are not explicitly kept;
- when release keys are present, the **debug** build is also signed with the release key, which is a
  confusing separation.

**Impact:** no verifiable artifact can be handed to a user or a store.
**Fix:** remove the hardcoded JDK path, document a keystore/Play App Signing process, add a
`bundleRelease` path, run a release build in CI on a clean machine, add a changelog, tag releases,
and publish SHA-256 checksums.

### P0-3. Store and release assets are incomplete

**Evidence:** `fastlane/metadata/android/` has only images:

- en-US: icon (512×512) and 7 screenshots (540×1052);
- ar-SA: icon only;
- no pt-BR assets;
- no `title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/`, feature graphic
  (1024×500), or promo graphics.

**Impact:** neither Google Play nor F-Droid can render a listing, and there is no Arabic or
Portuguese store presence despite Arabic being the default in-app language.
**Fix:** write store copy in all three languages, produce a feature graphic, verify screenshots
against the current UI, and add `changelogs/<versionCode>.txt`. Keep the fastlane layout if F-Droid
is a target; otherwise fill the Play Console manually.

### P0-4. Google Play policy fit is unresolved — and likely negative

This is the central publication decision. The current build combines:

| Feature | Play policy area | Risk |
|---|---|---|
| Device Owner via Shizuku on a personal phone | Device Admin / enterprise policy; reviewers expect a managed-device use case | High |
| Accessibility Service that reads all window content and takes screenshots | Accessibility API policy: prominent disclosure, consent, core functionality, demo video | High |
| `QUERY_ALL_PACKAGES` | Restricted permission declaration | Medium–high |
| Suspending/hiding other apps and blocking uninstall | Interference with other apps / user control | High |
| Scraping `play.google.com` listings | Google network/ToS expectations; brittle in review | Medium |
| Setup requiring removal of all accounts / factory reset | Consumer onboarding; review friction | High |
| Preventing the user from uninstalling its own app | User-control and "deceptive behaviour" review | High |

The existing `-PplayStoreBuild=true` flag only removes the APK-update path. It does **not** address
the Device Owner requirement, the accessibility screenshot use, `QUERY_ALL_PACKAGES`, app
suspension or the uninstall lock. The prior internal publication study recommended making Device
Owner optional and keeping a useful core without it; the current `MainActivity` does the opposite —
it blocks on Device Owner before the commitment and tabs. So the Play path has not actually been
implemented.

**Impact:** a Play submission is likely to be rejected or to require a fundamentally weaker
product variant.
**Fix (decision, not code):**

- **Option A — direct distribution (recommended).** Publish the full product as a signed APK on
  GitHub Releases, with Obtainium instructions. No Play policy applies.
- **Option B — F-Droid.** Viable for a free/open-source app, but the prebuilt model, the Play-page
  scrape (likely NonFreeNet anti-feature) and reproducible builds need work. See §7.
- **Option C — a Play-safe variant.** Remove Device Owner as a hard requirement, drop the
  uninstall lock and self-update, keep accessibility with a prominent disclosure, and accept that
  the variant is materially weaker. This is a separate product, not a build flag.

If Play is still desired, confirm the current Device Admin and Accessibility policies with the Play
Console before investing; policy text changes, and a rejection costs a review cycle.

### P0-5. Backup can copy the recovery code off the device

**Evidence:** `AndroidManifest.xml` sets `android:allowBackup="true"`;
`res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` are empty templates; the recovery
code lives in plain `SharedPreferences` (`recovery_prefs`), as does the commitment
(`security_prefs`). Android's default backup set includes app `SharedPreferences`.

**Impact:** Google Drive backup or device-to-device transfer can carry the code and commitment off
the phone, contradicting the "paper only, never digital" promise. A restored backup on another
device also restores the commitment without the Device Owner state, which is an inconsistent and
potentially confusing state.
**Fix:** set `android:allowBackup="false"` (simplest and consistent with the product), or exclude
`recovery_prefs` and `security_prefs` from both cloud backup and device transfer. Consider
`DevicePolicyManager.setBackupServiceEnabled(false)` while Device Owner is held. Store the recovery
code with `EncryptedSharedPreferences` if it must persist.

### P0-6. Forced removal does not count time spent powered off

**Evidence:** `SecurityManager.getForcedRemovalRemainingTime()` stores
`forced_removal_last_elapsed` from `SystemClock.elapsedRealtime()`. After a reboot,
`elapsedRealtime` restarts, so `now >= lastElapsed` is false and the code adds `now` (uptime) rather
than the wall-clock gap. Time while the device is off is not added.

**Impact:** the documented "48-hour timer" can be paused indefinitely by powering the phone off, and
`docs/FEATURES.md` claims that a reboot does not pause it, which is false. This is the only in-app
escape hatch for a lost code (when the commitment is timed), so the discrepancy matters.
**Fix:** mirror the commitment's approach: persist a wall-clock timestamp and a watermark, and add
the wall-clock delta on reboot, clamped to the target. Add a unit test for "reboot after N hours"
and "powered off for N hours". Update the docs either way.

### P0-7. Asset licensing is not fully resolved

**Evidence:** `NOTICE` says the TFLite model is "bundled from the upstream project" without a model
license, and attributes Quran translations to Saheeh International (English) and Samir El-Hayek
(Portuguese) with a general QuranEnc/QUL reference. The model and those translations are not
obviously covered by the project's Apache 2.0 license.

**Impact:** redistribution (especially in a store) could raise a copyright claim; F-Droid is
particularly strict about asset provenance.
**Fix:** record the model's source, version and license; confirm permission for each reading
translation, or replace uncertain translations with public-domain/self-authored passages. Keep the
attribution file in sync.

---

## 6. Important, non-blocking issues (P1)

### P1-1. Setup is hostile to ordinary users

`dpm set-device-owner` requires one user, no accounts and no other owner. In practice the user must
remove their Google account (which may wipe app data) or factory-reset the phone. That is Android's
rule, but the README's "Install Shizuku, pair wireless debugging, become Device Owner" steps
understate it. For a consumer release, this is the main adoption barrier after the Play policy.

**Fix:** make the account-removal step explicit and test the exact flow on a stock Pixel and a
Samsung device; consider a setup checklist with a "what will happen to your data" warning.

### P1-2. The accessibility service is a single 922-line class

It handles text events, active/passive word blocking, media-region discovery, screenshots, LiteRT
inference, suspension tracking, Tor/torrent sweeps, package-added receivers, rating checks and
service lifecycle. It has no unit or instrumentation coverage.

**Fix:** extract focus/word handling, media-region discovery and suspension policy into testable
classes. Add at least a Robolectric test for the word matcher and the region heuristics.

### P1-3. No CI and no instrumentation tests

The 81 unit tests are a good start, but nothing runs them automatically. A clean-machine build is
also unverified because of the hardcoded JDK path.

**Fix:** a GitHub Actions workflow that runs `make test`, `assembleDebug` and `bundleRelease` on
JDK 21 and SDK 36; optionally run a managed-device or emulator smoke test for the app launch and
settings gate.

### P1-4. The age-rating layer is a brittle scrape

`PlayStoreRatingFetcher` depends on the current HTML shape (`application/ld+json`, the
`img[itemprop=image][alt=Content rating]` sibling box), a desktop user agent, `hl=en` and `gl=BR`.
It fails silently offline and does nothing on non-GMS devices. It also fetches a Google page from
an app that may be distributed by Google.

**Fix:** make the fetch injectable and unit-test the parser against saved fixtures; surface "rating
unknown" in the UI; add retry/backoff; make `hl/gl` follow the app locale; document the fallback.
Consider using an official API or a maintained rating source if one exists.

### P1-5. Blocking work in `DeviceOwnerRepository`

`processDeviceOwnerPrivileges` calls `Thread.sleep(250)` and `waitForAdminResult` polls with
`Thread.sleep` in a loop. If either is reached from the main thread, the UI freezes; if it is not,
the code is still harder to test than a coroutine.

**Fix:** move the wait into a `suspend` function with `delay` on `Dispatchers.IO`, or use the
policy broadcast exclusively, and assert the thread in tests.

### P1-6. No rate limiting or audit trail for code attempts

The 16-character alphabet makes brute force impractical, so this is not a security hole. But the
app keeps no record of failed attempts, and the code dialog is a modal that can be retried at will.
For an accountability product, a small attempt counter with a delay would be a cheap deterrent.

**Fix (optional):** store a timestamp/count in `recovery_prefs` and add a short delay after repeated
failures.

### P1-7. Accessibility-off degradation is under-explained

If the user keeps the accessibility service off, visual/word blocking is inactive. The app suspends
the watched apps and applies `DISALLOW_INSTALL_APPS`, but the UI does not give a durable, prominent
explanation beyond the setup card.

**Fix:** add a persistent warning on the Protection tab while the service is off, and state in the
README exactly which layers remain (DNS, URL lists, system restrictions, Tor/torrent suspension).

### P1-8. Data Safety and content rating are not prepared

The app sends installed package names to `play.google.com` for the age-rating check. That is a
disclosure item for the Data Safety form even though no analytics SDK is present. The content
rating questionnaire should account for adult vocabulary in settings, reading passages about lust,
and the app's purpose.

**Fix:** draft the Data Safety answers and the IARC questionnaire before submission; keep them in
the release checklist. Note that "no data collected" is not quite accurate if package names leave
the device — "app info" is shared for app functionality.

### P1-9. No support or diagnostics story

No crash reporting is a defensible privacy choice, but there is no in-app diagnostics screen, no
"copy debug info" action and no issue template. Field reports about OEM-specific behaviour will be
hard to act on.

**Fix:** add an optional, user-initiated diagnostics export (version, owner state, service state,
enabled layers, recent block counts — no content), and a GitHub issue template.

### P1-10. Versioning and changelog discipline

`versionCode` and `versionName` are edited by hand; there is no changelog file and no
release-notes template. Fastlane has no `changelogs/` directory.

**Fix:** add `CHANGELOG.md`, a release checklist, and per-version fastlane changelogs.

---

## 7. Distribution channel analysis

| Channel | Fit | What is required | Main blockers |
|---|---|---|---|
| **GitHub Releases + Obtainium** | **Best fit** | Signed APK, SHA-256, release notes, version tags, an update instruction | P0-1/2/3/5/6; no store policy |
| **F-Droid** | Good, with caveats | Fastlane metadata, `fdroidserver` metadata, reproducible build recipe, asset licenses | Prebuilt TFLite model, Play-page scrape (likely NonFreeNet anti-feature), reproducible builds |
| **Google Play** | Poor fit for the full product | Play-safe variant, accessibility disclosure/video, `QUERY_ALL_PACKAGES` declaration, Device Admin justification, Data Safety, content rating | Device Owner requirement, uninstall lock, app suspension, account-removal setup |
| **Samsung Galaxy Store / Amazon Appstore** | Uncertain | Similar review package; check each store's device-admin policy | Likely the same policy concerns as Play, with less reviewer familiarity |
| **Enterprise / MDM** | Niche | Managed-device positioning, EMM integration | The product is self-control, not fleet management; distribution model differs |

**Recommendation:** direct distribution first. It matches the audience (people willing to pair
Shizuku and remove an account), avoids a policy fight, and lets the product keep its defining
features. Add F-Droid if reproducible builds and asset licensing can be sorted. Treat Play as an
optional, separate variant only if there is a concrete business reason.

---

## 8. What is still needed, in order

### Phase 0 — decisions (before code)

1. Choose the channel: direct, F-Droid, or a Play variant. Everything else depends on this.
2. Decide whether the full product or a weakened variant is the public one.
3. Confirm asset licenses (model, Quran translations) with the actual license texts.
4. Decide the support model: GitHub Issues, a privacy-friendly diagnostics export, or none.

### Phase 1 — release engineering (roughly 2–4 days)

- Remove the machine-specific `org.gradle.java.home` from `gradle.properties`.
- Document and script keystore creation; use Play App Signing if Play is pursued; keep keys out of
  the repo.
- Add `bundleRelease`/signed-APK instructions and verify a clean-machine build in CI.
- Add `CHANGELOG.md`, version tags, release notes and SHA-256 checksums.
- Host the privacy policy and update the README link.
- Write store copy in en/ar/pt, add a feature graphic, refresh screenshots, add fastlane changelogs.
- Add a GitHub Actions workflow for test + build + release.

### Phase 2 — security and correctness (roughly 3–5 days)

- Disable or scope Android backup; encrypt or exclude the recovery code.
- Fix the forced-removal timer and add reboot/power-off tests.
- Add the age-rating parser fixtures and a "rating unknown" state.
- Remove blocking sleeps from the owner flow.
- Reconcile README/FEATURES with the code, including the owner-before-commitment order and the
  account-removal requirement.
- Add an instrumentation smoke test for the launch gate and the settings gate.
- Extract the accessibility service's pure logic and test it.

### Phase 3 — distribution (roughly 1 week for direct; longer for a store)

- Produce the signed release APK, tag it, publish release notes and checksums.
- Write install instructions for a stock phone, including the account-removal step and the
  Shizuku pairing.
- Provide a recovery-code safety page and a "what to do if you lose it" page.
- If F-Droid: add `metadata/<appid>.yml`, reproducible-build recipe, and resolve anti-features.
- If Play: build the weaker variant, record the accessibility disclosure video, submit the
  `QUERY_ALL_PACKAGES` declaration, and prepare for a rejection loop.

---

## 9. Pre-release checklist

### Build and artifact

- [ ] Clean-machine build on JDK 21 and SDK 36 with no machine-specific paths
- [ ] Release build passes R8/resource shrinking and installs
- [ ] Signed APK (and AAB if a store is targeted), key backed up outside the repo
- [ ] `versionCode` bumped; `CHANGELOG.md` and store changelog updated
- [ ] SHA-256 published with the artifact
- [ ] `-PplayStoreBuild=true` artifact verified to hide the APK updater (if used)

### Security and privacy

- [ ] Backup disabled or recovery code/commitment excluded and encrypted
- [ ] Forced-removal timer counts wall-clock time across power-off and reboot
- [ ] Commitment clock tested with date forward/back, reboot, and long power-off
- [ ] Recovery code never logged, never displayed after the wizard, never backed up
- [ ] Privacy policy live and accurate (Play page fetch, Shizuku, backup, no analytics)
- [ ] Data Safety form drafted (package names shared with Google for app functionality)
- [ ] Content rating questionnaire completed
- [ ] Model and reading-passage licenses recorded

### Behaviour and QA

- [ ] Android 11, 12, 13, 14, 15 and 16, stock and OEM (Samsung/Xiaomi if possible)
- [ ] GMS and non-GMS (Waydroid or AOSP) devices
- [ ] Arabic RTL, English and Portuguese; all screens
- [ ] Fresh install: recovery code → owner → commitment → lock → onboarding
- [ ] Account present / multiple users: setup explains and blocks correctly
- [ ] Reboot, power off for hours, date forward/back, safe mode, ADB
- [ ] Uninstall attempt, force-stop attempt, clear-data attempt
- [ ] Accessibility on/off; service killed; re-enable path
- [ ] Install supported browser, unsupported browser, Tor, torrent client, 18+ app
- [ ] Visual blocking on Reddit and on a Compose app; battery/thermal impact
- [ ] Word blocking in a browser address bar, a chat app, and a search field
- [ ] Lost code: forced removal completes; permanent commitment has no escape
- [ ] Release build, not just debug
- [ ] TalkBack pass on the main screens (the app's own accessibility, not the service)

### Distribution

- [ ] Store listing text and screenshots in all supported languages
- [ ] Feature graphic and icon meet the target store's dimensions
- [ ] Install and recovery instructions in the README
- [ ] Support channel documented (issue template or contact)
- [ ] Channel decision recorded, with the Play variant explicitly out of scope if not pursued

---

## 10. Honest assessment of the biggest risks

1. **Policy risk is bigger than code risk.** The product's core mechanic — making itself hard to
   remove — is exactly what app-store review is designed to scrutinise. No amount of build flags
   changes that.
2. **The setup funnel will lose most users.** Removing all accounts, pairing Shizuku and granting
   Device Owner is a lot to ask even when the user picks Regular instead of a timed commitment. The
   app should be honest about this on its landing page; the current README is detailed but
   optimistic.
3. **The security claims are slightly ahead of the implementation.** The commitment clock is strong;
   the forced-removal timer and backup handling are not. Fix those before marketing the app as
   tamper-resistant.
4. **The visual classifier is the most fragile layer.** It is heuristic region discovery plus a
   224×224 classifier on the CPU. It will work on Reddit and some feed apps and miss others. The
   product should present DNS, URL lists and word blocking as the primary layers, with visual
   blocking as an additional net — which is how the code is structured, but not how the README sells
   it.
5. **The 18+ layer is a scrape.** It is useful when it works, but it is not a reliable enforcement
   boundary. Do not let the UI imply otherwise.
6. **Licensing can stop a release late.** A prebuilt model with unclear provenance and
   copyrighted Quran translations are exactly the kind of issue that surfaces at the store-review
   stage. Resolve it now.

---

## 11. Limitations of this evaluation

- No app was installed or exercised on a phone or in Waydroid; all behavioural findings come from
  reading the code and running the JVM tests.
- No Google Play, F-Droid or legal review was performed. Policy statements are an assessment of
  publicly known policy areas, not a ruling.
- The test run used the machine's JDK 21 and cached dependencies; it does not prove a clean-clone
  build.
- Store policy and Android behaviour change; re-check the current requirements before submitting.
- The evaluation does not assess visual design quality beyond the structure implied by the Compose
  code and existing screenshots.

---

## 12. Bottom line

The hard part of this project — making protection survive a determined user without a server — is
mostly done and is unusually well considered. The remaining work is ordinary release engineering
plus four real fixes: **the privacy policy, backup handling, the forced-removal timer, and the
policy/channel decision**. With those, a direct-distribution beta is realistic. A Google Play release of the full product is not,
and pretending otherwise would waste the strongest part of the work.
