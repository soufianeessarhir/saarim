# Onboarding carousel — illustration prompts

Source of the frame: the 5-slide copy in `OnboardingCarousel.kt` / `strings.xml`
(`onboarding_slide_0..4`). Generate one illustration per slide with the prompts below
and drop the assets in as:

```
app/src/main/res/drawable-nodpi/onboarding_slide_0.png   # 0 … 5 (light background)
app/src/main/res/drawable-nodpi/onboarding_slide_0_dark.png   # …_dark variants
```

`-nodpi` so Android never scales them; squares look best in the carousel's centered
column (the composable reserves ~96–500 dp, so **1024×1024** is the right working size).

## Non-negotiable constraints (bake these into every prompt)

1. **No text of any kind** — no letters, digits, UI captions or icons-with-labels. The app
   ships the same slides in Arabic (default), English and Portuguese, and Arabic flips to
   RTL; any baked-in text is wrong in two of the three languages and can't be re-translated.
   Where copy says "code" or "10 minutes", draw an abstraction (dashed key tag, filled
   progress ring) instead.
2. **Flat vector style, no photorealism** — the app is Material 3 with 2-tone icons; anything
   rendered looks broken next to it.
3. Match this palette exactly (from `ui/theme/Color.kt`):
   - Deep green `#2C6A45` · bright mint `#95D5A8` · pale mint `#B0F1C3` · forest `#0F512F`
   - Terracotta `#8E4D2F` (light bg) / `#FFB596` (dark bg) — accents, warnings
   - Soft gold `#ECC06C` / `#7A590C` — the "session timer" color
   - Backgrounds: cream `#F6FBF3` (light set) or near-black green `#0F1511` (dark set)
4. **Two variants per slide** — the carousel renders on top of both light and dark themes. One
   flat drawing on a transparent background also works if your model supports alpha; then
   skip `_dark`.

## Shared style block (paste before each slide prompt)

> Flat minimalist vector illustration, Material Design 3 style, clean geometric shapes with
> soft rounded corners, limited palette of deep green #2C6A45, mint green #95D5A8, pale mint
> #B0F1C3 and a small terracotta #8E4D2F / soft gold #ECC06C accent, on a plain cream
> #F6FBF3 background, centered single subject, generous empty margins on all sides, no
> gradients heavier than a subtle two-tone, no text, no letters, no numbers, no watermark.

Negative prompt (SD/SDXL-class tools): `text, letters, typography, alphabet, numbers, labels,
UI mockup, photo, photorealistic, 3d render, shadows, gradient mesh, busy background,
watermark, signature, realistic people, hands with fingernails`

---

## Slide 0 — What Sarim is

> Flat minimalist vector illustration, Material Design 3 style, limited palette of deep
> green #2C6A45, mint green #95D5A8, pale mint #B0F1C3, terracotta #8E4D2F and gold
> #ECC06C accents on a plain cream #F6FBF3 background, no text, no letters, no numbers.
> Scene: a modern smartphone seen from the front, wearing a large translucent green shield
> emblem whose silhouette doubles as a padlock. Behind/around the phone, three small
> floating cards in muted grey-beige #414942 tones sit blocked outside the shield: one card
> is an app grid, one card shows a speech bubble with crossed-out scribbles, one card shows
> an image with an eye icon overlaid by a cancel slash. To the side of the phone, a small
> folded paper tag with a dashed keyhole hangs on a short string, suggesting a physical
> written key. Calm, protective, self-control mood. Centered single subject, generous
> margins, flat colors with subtle two-tone shading.

## Slide 1 — Your session

> Flat minimalist vector illustration, Material Design 3 style, deep green #2C6A45, mint
> #95D5A8, pale mint #B0F1C3, gold #ECC06C on cream #F6FBF3 background, no text, no
> digits. Scene: an abstract circular hourglass / countdown ring in soft gold #ECC06C,
> about two-thirds filled with a mint green arc, floating above a rounded doorway shape in
> deep green with a padlock that is just unlocking — the shackle is open and a small beam
> of mint light spills through the opening. A dashed border marks the "active" area inside
> the doorway, and the ring's own entry point and end point are tabbed with small arrows
> pointing from full to nearly empty, communicating a ticking limited window. Nothing
> suggests a clock face with numbers. Centered subject, generous margins, two-tone shading.

## Slide 2 — Restrictions & apps

> Flat minimalist vector illustration, Material Design 3 style, deep green #2C6A45, mint
> #95D5A8, pale mint #B0F1C3, terracotta #8E4D2F on cream #F6FBF3, no text, numbers or
> app logos. Scene: a tidy 2×3 grid of rounded app tiles in pale mint, each with a neutral
> abstract icon (camera eye, speech bubble, globe, music note, chat, book). Two tiles are
> dimmed to grey outline with a soft terracotta "paused" overlap bar across them, one tile
> is capped with a small gold hourglass badge representing a daily budget, and one tile is
> inside a dashed pale-mint rectangle suggesting an allowed time window with a tiny sun and
> moon pair at its corners. One suspended speech-bubble tile has its scribble-content
> erased with trailing star-markers. Balanced, orderly composition, centered, generous
> margins, two-tone flat shading.

## Slide 3 — Focus & impulse

> Flat minimalist vector illustration, Material Design 3 style, deep green #2C6A45, mint
> #95D5A8, pale mint #B0F1C3, gold #ECC06C, terracotta #8E4D2F on cream #F6FBF3, no text
> or digits. Scene: left half, a Pomodoro-style rounded tomato shape stylized as a
> geometric timer with a pie-slice progress ring on its face and small sound-wave arcs by
> its stem, hinting at alarms. Right half, a large soft terracotta SOS-style panic button
> with concentric ring pulses radiating outward; beneath the pulses sit three greyed-out
> rounded app silhouettes visibly "frozen", each with a small snowflake pause glyph. The
> two halves are joined by a thin mint dashed tether, showing that starting a focus session
> pauses the same apps. Centered balanced composition, generous margins, flat two-tone.

## Slide 4 — Device owner setup (the Shizuku bridge)

> Flat minimalist vector illustration, Material Design 3 style, deep green #2C6A45, mint
> #95D5A8, pale mint #B0F1C3, gold #ECC06C, terracotta #8E4D2F on cream #F6FBF3, strictly
> no readable characters — any settings-like lines should be generic grey dashes. Scene
> (the Shizuku communication, simplified): two smartphones side by side. Left phone, cooler
> grey-green tones, wears a small origami-crane / windmill paper emblem as its identity
> mark and emits three expanding wireless-pairing arcs from its top edge. Right phone, the
> target, wears a deep-green crown/chief badge on its screen. Between the two phones a
> clean dashed mint bridge or double-headed arrow carries three descending geometric tokens:
> a shield (privilege), a suspension icon (an app tile fading), and a color-wheel quarter
> (screen color filters). On the right phone's corner, a bold mint circular checkmark badge
> signals the grant succeeded. Optional small terracotta warning triangle floating at the
> bridge's midpoint, acknowledging the caution around the grant. Centered, calm, generous
> margins, flat two-tone shading.

### Alternative: split slide 4 into two slides (6 total)

If you'd rather have dedicated depth on the grant flow, slide 4 stays "what device owner
unlocks" and a new slide 5 gets this prompt (same style block):

> … Scene: a close-up of a single phone in deep green lying flat, viewed isometric, with a
> glowing mint command ribbon entering from its top-left corner where a small paper-crane
> emblem floats, and exiting bottom-right as a solid green key tab that clicks into a
> shield socket on the phone's side. Three faint "before" silhouettes behind the phone
> stay grey, while three "after" silhouettes in front pop into full mint color. A small
> gold hourglass near the ribbon's start suggests confirmation takes a moment (the app
> waits for the system's own confirmation, not the command's exit code). NO readable
> characters anywhere on the screen; terminal-like rows must be generic grey dashes only.

If you go this route I will bump `ONBOARDING_SLIDE_COUNT` to 6 and swap slide-4's copy to
the capabilities list — say the word and I'll wire the code and strings alongside.

---

## Placement tweaks needed once assets are in

- `OnboardingSlide` currently renders a 96 dp `Icon`. With bitmaps it should render the
  drawable in a 240–280 dp box instead (`painterResource(R.drawable.onboarding_slide_X)` +
  `onboarding_slide_X_dark` chosen by `isSystemInDarkTheme()`), falling back to the vector
  icon while an asset is missing. I'll do that wiring when the files land.
- Keep files ≤ 100 KB each (PNG-8 / WebP lossy); 6 full-res PNGs would double the APK.
