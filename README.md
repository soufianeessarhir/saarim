<div align="center">
<img src="docs/logo.png" width="128" alt="Sarim logo">

# Sarim · صارم

**A hard-to-bypass Android barrier against pornography and digital addiction.**

Device Owner restrictions, an Accessibility Service and an on-device ML model working together,<br>
built so that your future self can't simply turn it off.

[![Android](https://img.shields.io/badge/Android-11%2B-3ddc84?style=flat-square&logo=android&logoColor=white)](#installation)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff?style=flat-square&logo=kotlin&logoColor=white)
[![License](https://img.shields.io/badge/License-Apache%202.0-b8860b?style=flat-square)](LICENSE)
![Languages](https://img.shields.io/badge/Languages-English%20%C2%B7%20%D8%A7%D9%84%D8%B9%D8%B1%D8%A8%D9%8A%D8%A9%20%C2%B7%20Portugu%C3%AAs-2e6b3f?style=flat-square)

[How it works](#how-it-works) · [Features](#features) · [Installation](#installation) · [Recovery](#recovery-and-safety) · [Privacy](#privacy)

</div>

>[!TIP]  
>"In your struggle against sin, you have not yet resisted to the point of shedding your blood." - Hebrews 12:4

<br>

<table>
  <tr>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1_lock_screen.png" width="230"><br><sub><b>Panic button</b>, reachable before unlocking</sub></td>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2_tetris_challenge.png" width="230"><br><sub><b>Tetris challenge</b> before entering</sub></td>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3_reading_challenge.png" width="230"><br><sub><b>Reflective reading</b>, word by word</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4_restrictions.png" width="230"><br><sub><b>System restrictions</b> via Device Owner</sub></td>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5_apps.png" width="230"><br><sub><b>Per-app</b> limits, groups and time windows</sub></td>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6_config.png" width="230"><br><sub><b>All protections</b> in one place</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7_visual_blocking.png" width="230"><br><sub><b>Visual blocking</b> with on-device ML</sub></td>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8_impulse_lock.png" width="230"><br><sub><b>Impulse lock</b> and access challenges</sub></td>
    <td align="center"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/9_color_modes.png" width="230"><br><sub><b>Color modes</b> on a schedule</sub></td>
  </tr>
</table>

> [!CAUTION]
> This software implements deep system-level modifications and is designed to be difficult to bypass. Proceed only if you fully understand the implications of Device Owner privileges. Read [Recovery and Safety](#recovery-and-safety) before installing.

## Why Sarim?

**Sarim** (Arabic: **صارم**, pronounced [/sˤaː.rim/](https://en.wiktionary.org/wiki/%D8%B5%D8%A7%D8%B1%D9%85)) names *the one who abstains* — and also, in the same root, *the one who is hard to bypass*. Both readings are the point. Most blockers are one settings screen away from being disabled in a weak moment. Sarim is built the other way around:

| | Typical blocker | Sarim |
|---|---|---|
| Can be uninstalled on impulse | Yes | No, Device Owner prevents it |
| VPN / private DNS can be changed to get around it | Usually | Blocked at the OS level |
| Detects explicit images and video | Rarely, often in the cloud | Yes, 100% on-device |
| Blocks words typed or shown inside apps | Rarely | Yes, in the apps you choose |
| Turning it off | A toggle | A 16-character recovery code stored on paper |
| Your data | Often sent to a server | Never leaves the device |

## How it works

A regular Android app lives in a sandbox. It can't see what other apps show, can't stop you from changing system settings, and can be uninstalled with two taps. That's why most blockers are so easy to get around.

Sarim is granted two special powers that ordinary apps never get:

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>1. Device Owner</h3>
      <p>The same level of control companies use to manage work phones. It lets Sarim:</p>
      <ul>
        <li>lock system settings such as VPN, private DNS, factory reset and app installs</li>
        <li>force SafeSearch and site blocklists on browsers</li>
        <li>suspend apps, and stop any app from being uninstalled, including itself</li>
      </ul>
    </td>
    <td width="50%" valign="top">
      <h3>2. Accessibility Service</h3>
      <p>The same access screen readers use to help blind users. It lets Sarim:</p>
      <ul>
        <li>read text on screen and as it's being typed</li>
        <li>look at images and video on screen, using an on-device ML model</li>
        <li>know which app and screen is open, to enforce time limits and block specific screens</li>
      </ul>
    </td>
  </tr>
</table>

Both are granted once, during setup, through [Shizuku](https://shizuku.rikka.app/). From then on, the Accessibility Service is protected against being turned off, and only your **recovery code** can take either power away. That code lives on a piece of paper, not in your head.

## Highlights

| | |
|---|---|
| 🛡️ **Device Owner restrictions** | Block VPNs, private DNS changes, factory reset, app installs, USB debugging and dozens more `UserManager` policies. |
| 🧠 **On-device visual blocking** | A local TensorFlow Lite model detects explicit images and video on screen and closes the app. Nothing is uploaded. |
| 🔒 **Strictness lock** | Locks adult-content blocking on for 90 days or longer. Nothing in the app can release it before it expires. |
| ⌨️ **Word blocking** | Erases forbidden words as they're typed, or closes the app when they appear anywhere on screen. |
| 🚨 **Impulse lock** | A panic button on the lock screen that locks Sarim (and chosen apps) for 15 minutes to 6 hours, resistant to clock changes. |
| 🍅 **Focus mode** | Pomodoro sessions that suspend distracting apps during each focus period and give them back on breaks. |
| ⏱️ **Time limits and windows** | Daily limits per app or group, per weekday, allowed time windows, reopening cooldowns and usage warnings. |
| 🌙 **Color modes** | Grayscale, night light, extra dim or inversion during scheduled windows, which can't be switched off while active. |

## Features

<details>
<summary><b>System restrictions (Device Owner)</b></summary>

- Every Android system restriction (`UserManager` policy) can be toggled on. Three are pre-selected as recommended: block VPN configuration, block private DNS configuration, and block factory reset. Dozens of others are available in a searchable list (blocking app installs/uninstalls, safe boot, USB/ADB debugging, adding accounts, and more, depending on Android version).
- Per-app restriction editor: any restriction an individual app declares (via `RestrictionsManager`) can be inspected and applied to that app specifically.

</details>

<details>
<summary><b>Browser and network</b></summary>

- Forces a family-safe private DNS provider (Cloudflare Family, AdGuard DNS Family, CleanBrowsing, or a custom host).
- Applies `URLBlocklist` and `ForceGoogleSafeSearch` policies to browsers that support them.
- Any newly installed browser that doesn't support these policies is suspended outright instead of being left unrestricted.
- Blocks Tor and Orbot: every installed Tor component is suspended, and so is anything installed later. Detection isn't a fixed list of package ids — the package name *and* the visible app name are checked, so a Tor client published under a different id (a rebuild, a rebrand, a future Tor Project app) is caught too. Turning the block on sweeps the apps already on the phone too, so a client installed beforehand can't slip past it.

</details>

<details>
<summary><b>App management and time limits</b></summary>

- Hide/block or instantly suspend individual apps.
- Block uninstallation on a per-app basis.
- Daily usage time limits per app or per group of apps, configurable per weekday, with a lock screen once the limit is reached.
- Allowed time windows, outside of which an app can't be opened.
- Optional usage warnings as notifications, in as many stages as you want, before a limit runs out.
- A minimum cooldown before an app can be reopened.
- Automatically suspends apps rated 18+ or flagged with explicit content, checked against the Play Store's content rating page when an app is installed or first opened.
- Automatically blocks torrent apps (detected by their ability to handle magnet links or `.torrent` files), including the ones already installed when the block is turned on.

</details>

<details>
<summary><b>Word blocking</b></summary>

- **Active blocking:** erases a forbidden word as it's typed into a selected app and shows a block screen.
- **Passive blocking:** scans on-screen text (not just typed input) per app, useful for search results and feeds, and closes the app when a configured word appears anywhere on screen.

</details>

<details>
<summary><b>Visual blocking (on-device machine learning)</b></summary>

- A local TensorFlow Lite model (MobileNetV2) classifies on-screen images and video into drawings, hentai, neutral, porn, and sexy. No data leaves the device.
- The Accessibility Service scans the screen of selected apps for likely media regions, captures them, and runs the classifier; the app is closed when the combined score for the selected categories crosses a configurable threshold.
- Optional escalation: after a set number of blocks within a time window, the app is suspended for a configurable duration instead of just being closed.
- **Strictness lock:** pins visual blocking on, with the adult categories enforced, for a period you choose — 90 days minimum, up to 10 years. While it runs, the switches that would narrow or disable it are frozen, and any attempt to clear the stored settings from outside the app is undone. It survives reboots and resists system clock changes in both directions.
- The only ways out before it expires are the 48-hour forced removal, or removing the app. The recovery code does **not** release it — that's the point of a lock you can't talk yourself out of at 1 a.m.

</details>

<details>
<summary><b>Activity blocker</b></summary>

- Blocks specific Android screens (activities) by class name, with a log of recently accessed activities to help identify which ones to block.

</details>

<details>
<summary><b>Impulse lock and access challenges</b></summary>

- A panic button on the lock screen, reachable even before authenticating: starts a 15-minute to 6-hour lock on Sarim itself, optionally also suspending a user-chosen list of apps for the same duration.
- The timer resists system clock changes and survives the app or its service being restarted.
- Opening Sarim requires biometric or device authentication, plus optional extra challenges. Any combination can be selected, and they run one after the other:
  - **Math:** a 5-problem arithmetic quiz.
  - **Words:** typing 32 words correctly in a row.
  - **Tetris:** playing for a configurable number of minutes (time only counts while actually playing). Gravity speeds up as levels rise, with a lock delay, line clear animations and sound effects that can be muted. When the time is up the game keeps going until you tap "Continue", so the current piece or line can be finished.
  - **Reflective reading:** reading a configurable number of texts (5 by default) about lust and self-control, picked at random from one of four sources: Bible verses (King James Version / João Ferreira de Almeida), Quran verses (Saheeh International / Samir El-Hayek), quotes from philosophers and great writers, or your own phrases. Each text is timed for a slow reading pace based on its length, with words highlighted one by one, and the next one only unlocks when the time runs out.
- Every challenge screen has a "give up" button that goes back to the lock screen.

</details>

<details>
<summary><b>Focus mode (Pomodoro)</b></summary>

- Alternates focus periods with short breaks, and gives a long break after a configurable number of focus periods.
- A user-chosen list of apps is suspended during each focus period and restored as soon as the break starts.
- Sessions can be started from the lock screen without authenticating. Starting, resuming and skipping a break are always free; pausing or ending a session requires the recovery code.
- An ongoing notification shows the current phase, a live countdown and a progress bar, with quick actions for what doesn't need the recovery code. 
- All times are configurable.

</details>

<details>
<summary><b>Color modes</b></summary>

- Applies screen color filters during user-defined time windows (including windows that span midnight): grayscale, night light, extra dim (Android 12+) and color inversion, alone or combined, with adjustable intensity for night light and extra dim.
- While a window is active the filters can't be turned off: switching them off (e.g. from Quick Settings) is reverted right away, and the active window, the selected modes and their intensity can't be changed until it ends, unless a recovery session is active. Adding windows or modes never asks for the recovery code; removing them, changing intensity or disabling the feature does.
- On devices without a platform night light (e.g. some Samsung models), night light is drawn as an amber overlay by the Accessibility Service instead, since forcing the system setting there can black out the screen.
- Your own display settings are restored once the window ends, and also if the Accessibility Service is turned off. Requires the Accessibility Service and the `WRITE_SECURE_SETTINGS` permission, granted automatically through Shizuku when Device Owner is set up, or when enabling the feature if it's still missing.

</details>

<details>
<summary><b>Other safeguards</b></summary>

- Optional shuffled keypad, so the recovery code can't be memorized by watching finger position.
- Any configuration change requires the recovery code, which unlocks a 10-minute session (renewable) so it isn't asked for on every single action.

</details>

## Installation

**Requirements:** Android 11 or newer.

Becoming Device Owner requires a bridge between user space and system space. Follow these steps precisely:

1. **Install [Shizuku](https://shizuku.rikka.app/).** It's used to run the required ADB commands on the device itself.
2. **Enable Wireless Debugging** in Developer Options and pair it with Shizuku.
3. **Install and open Sarim**, then grant it access to Shizuku.
4. **Become Device Owner.** In the Config tab, follow the prompts to register the app as Device Owner. This runs `dpm set-device-owner` through Shizuku.
5. **Enable the Accessibility Service.** Still in the Config tab, turn on *Advanced blocking* (also applied through Shizuku). It's required for word blocking, visual blocking, activity blocking, torrent blocking, time limits and color modes. App installation is blocked while this service is off, so an unrestricted browser can't be installed in the meantime.

## Recovery and Safety

During setup, Sarim generates a unique **16-character recovery code**. It's the only ordinary way to disable restrictions or uninstall the app without a full device wipe (if a wipe is even permitted by your active settings).

A step-by-step wizard sets it up, both on first setup and whenever you generate a new code: write it down on paper, type it back from the paper to confirm every letter, then choose a safe place to store it.

The last step points out the option that works best: handing the code to an **accountability partner** — a friend, a parent, a spouse — rather than keeping it in reach. The wizard can hand it straight to the share sheet with a ready-written message, so it leaves your phone in one step and you're not able to look at it when temptation arrives.

> [!IMPORTANT]
> - **Write it on paper.** The key must be recorded by hand, on a physical piece of paper.
> - **Store it somewhere hard to reach**, such as a high shelf or a different building.
> - **Never store it digitally.** Not in notes, emails or cloud storage: you may end up blocking the very tools needed to retrieve it.

### If the code is lost

The Config tab offers a **forced removal** option as a deliberate last resort. It starts a 48-hour timer, and once it expires, Device Owner privileges can be removed without the recovery code. The delay exists so it can't be used as a quick bypass, and it can be cancelled at any time before it completes.

> [!WARNING]
> **Sarim is designed to be uncompromising.** Activating full system restrictions and then losing your recovery code may leave you permanently unable to change system settings or restore the device until the 48-hour forced removal completes. This self-lockout is intentional: it's what stops your future self from relapsing. Besides the recovery code and forced removal, there is no other backdoor.

> [!NOTE]
> If you start a **strictness lock**, the recovery code deliberately stops working for adult-content settings until the lock expires: the 48-hour forced removal is the only way out early. Choose a period you can live with before starting it.

## Privacy

**Nothing leaves the device.** Sarim has no ads, no ad SDKs, no analytics, no crash-reporting SDK, and no telemetry. It makes no network calls of its own.

- The visual-blocking model (`nsfw_model.tflite`) runs entirely on-device. Screens are never captured to disk and never uploaded.
- Word blocking, time limits, focus mode, color modes and the recovery code all run and stay on the phone.
- The only outside interaction is with [Shizuku](https://shizuku.rikka.app/), which is used locally to run the Device Owner setup commands. Shizuku runs on your device; it is not a server.
- No account is required. No data is collected, sold or shared.

The full text is in the [privacy policy](https://sarim.app/privacy).

## Fork changes

Sarim is a fork of [Déchaîner](https://github.com/warleysr/dechainer) (Apache 2.0). Changes made in this fork:

- Renamed the app to Sarim / صارم, with the application ID and namespace moved to `com.sarim.app`.
- Added Arabic as the app's default language — full `values-ar` translations, RTL layout support, and an Arabic Quran (Mus'haf) source for the reading challenge. Config → Language switches to English or Portuguese.
- Added Arabic root/stem matching to the word blocker, since single-word matching does not work well for a morphological language.
- Added unit test coverage for the security manager, and made Device Owner setup wait for the authoritative policy broadcast instead of guessing.
- Added a first-run "How it works" carousel (5 slides, en/ar/pt) that appears once after the recovery code is set and can be re-opened any time from the Config tab.
- Added a `-PplayStoreBuild=true` build flag (`BuildConfig.PLAY_STORE_BUILD`) that removes the self-update-by-APK path for the Play-distributed APK, per Google Play policy.
- Added the strictness lock (90-day minimum adult-content lock, enforced from the Accessibility Service), identity-based Tor/Orbot blocking with an install-time sweep, and accountability-partner guidance in the recovery-code wizard.

## License

Released under the [Apache License 2.0](LICENSE). The full license text is kept verbatim; no additional terms are imposed.

This project is a fork. It is derived from [Déchaîner](https://github.com/warleysr/dechainer), Copyright the Déchaîner authors, licensed under the Apache License 2.0. The original copyright and attribution notices are retained — see [NOTICE](NOTICE).

**Disclaimer:** this software is provided "as is", without warranty of any kind. The developers are not liable for any data loss, system instability, or permanent device lockouts resulting from the use of Device Owner privileges.
