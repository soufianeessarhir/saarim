#!/usr/bin/env python3
"""Generates the Sarim / صارم wordmark and every launcher, logo and store icon asset.

Design: a clean dark-on-light wordmark. The Arabic صارم sits above the Latin SARIM,
both in the app's primary green on a warm off-white field, so the mark stays legible
at 48dp and still reads at 512px.

Usage:  python3 tools/generate_icons.py
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent

# --- Palette (dark on light) -------------------------------------------------
FIELD = (247, 243, 234, 255)  # warm off-white field
INK = (31, 77, 51, 255)       # primary dark green, matches Theme primaryLight
SUB_INK = (43, 43, 43, 255)   # near-black for the Latin wordmark
RULE = (31, 77, 51, 110)      # hairline rule between the two lines

ARABIC_FONT = "/usr/share/fonts/vazirmatn-vf-fonts/Vazirmatn[wght].ttf"
LATIN_FONT = "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Bold.ttf"

# Master artwork is rendered once at high resolution, then downsampled.
MASTER = 2048


def arabic_font(size: int) -> ImageFont.FreeTypeFont:
    font = ImageFont.truetype(ARABIC_FONT, size)
    try:  # Vazirmatn is a variable font; pin the weight axis to Bold.
        font.set_variation_by_axes([700])
    except Exception:
        pass
    return font


def latin_font(size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(LATIN_FONT, size)


def tracked_text(draw: ImageDraw.ImageDraw, text: str, font, tracking: int) -> Image.Image:
    """Renders `text` with letter-spacing, since PIL has no tracking support."""
    widths = [draw.textlength(ch, font=font) for ch in text]
    total = int(sum(widths) + tracking * (len(text) - 1))
    height = int(font.size * 1.6)
    layer = Image.new("RGBA", (max(total, 1), height), (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    x = 0.0
    for ch, w in zip(text, widths):
        ld.text((x, 0), ch, font=font, fill=SUB_INK)
        x += w + tracking
    return layer


def build_mark(canvas: int, content_ratio: float) -> Image.Image:
    """The bare wordmark on transparency, sized so its width is `content_ratio` of `canvas`."""
    ar_text = "صارم"
    lat_text = "SARIM"

    probe = ImageDraw.Draw(Image.new("RGBA", (8, 8)))
    ar_size = 1000
    ar_font = arabic_font(ar_size)
    ar_w = probe.textlength(ar_text, font=ar_font)
    # Scale the Arabic line so it occupies the requested width.
    ar_font = arabic_font(max(10, int(ar_size * (canvas * content_ratio) / ar_w)))

    ar_box = ar_font.getbbox(ar_text)
    ar_w = ar_box[2] - ar_box[0]
    ar_h = ar_box[3] - ar_box[1]

    lat_font = latin_font(int(canvas * content_ratio * 0.20))
    lat_layer = tracked_text(probe, lat_text, lat_font, tracking=int(canvas * content_ratio * 0.055))
    lat_layer = lat_layer.crop(lat_layer.getbbox())

    rule_h = max(2, int(canvas * 0.006))
    gap = int(canvas * content_ratio * 0.10)

    total_h = ar_h + gap + rule_h + gap + lat_layer.height
    mark = Image.new("RGBA", (canvas, total_h), (0, 0, 0, 0))
    md = ImageDraw.Draw(mark)

    cx = canvas // 2
    y = 0
    md.text((cx, y - ar_box[1]), ar_text, font=ar_font, fill=INK, anchor="ma")

    y += ar_h + gap
    rule_w = int(min(canvas * content_ratio, lat_layer.width * 1.9))
    md.rectangle([cx - rule_w // 2, y, cx + rule_w // 2, y + rule_h], fill=RULE)

    y += rule_h + gap
    mark.alpha_composite(lat_layer, (cx - lat_layer.width // 2, y))
    return mark


def paste_centered(base: Image.Image, overlay: Image.Image, cy_ratio: float = 0.5) -> None:
    x = (base.width - overlay.width) // 2
    y = int(base.height * cy_ratio - overlay.height / 2)
    base.alpha_composite(overlay, (x, y))


def rounded_mask(size: int, radius_ratio: float) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size - 1, size - 1], radius=radius_ratio * size, fill=255
    )
    return mask


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def square_icon(size: int, mask: Image.Image, content_ratio: float) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), FIELD)
    paste_centered(canvas, build_mark(size, content_ratio).resize(
        (size, size), Image.LANCZOS
    ))
    canvas.putalpha(mask)
    return canvas


def main() -> None:
    # Adaptive-icon foreground: artwork must survive the 66/108 safe zone, so it is
    # drawn small inside a transparent canvas.
    fg_master = build_mark(MASTER, content_ratio=0.42)

    ladder = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for bucket, scale in ladder.items():
        dpi_dir = ROOT / f"app/src/main/res/mipmap-{bucket}"

        legacy_px = int(48 * scale)
        legacy = Image.new("RGBA", (MASTER, MASTER), FIELD)
        paste_centered(legacy, build_mark(MASTER, content_ratio=0.60))
        legacy.putalpha(rounded_mask(MASTER, 0.22))
        legacy.resize((legacy_px, legacy_px), Image.LANCZOS).save(
            dpi_dir / "ic_launcher.webp", lossless=True
        )

        round_icon = Image.new("RGBA", (MASTER, MASTER), FIELD)
        paste_centered(round_icon, build_mark(MASTER, content_ratio=0.58))
        round_icon.putalpha(circle_mask(MASTER))
        round_icon.resize((legacy_px, legacy_px), Image.LANCZOS).save(
            dpi_dir / "ic_launcher_round.webp", lossless=True
        )

        fg_px = int(108 * scale)
        fg_master.resize((fg_px, fg_px), Image.LANCZOS).save(
            dpi_dir / "ic_launcher_foreground.webp", lossless=True
        )

    # Play Store listing icon: 512x512, full-bleed (no alpha), 32-bit PNG.
    store = Image.new("RGBA", (MASTER, MASTER), FIELD)
    paste_centered(store, build_mark(MASTER, content_ratio=0.68))
    store.convert("RGB").resize((512, 512), Image.LANCZOS).save(
        ROOT / "app/src/main/ic_launcher-playstore.png"
    )
    store.convert("RGB").resize((512, 512), Image.LANCZOS).save(
        ROOT / "fastlane/metadata/android/en-US/images/icon.png"
    )
    store.convert("RGB").resize((512, 512), Image.LANCZOS).save(
        ROOT / "fastlane/metadata/android/ar-SA/images/icon.png"
    )

    # README logo.
    logo = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))
    paste_centered(logo, build_mark(MASTER, content_ratio=0.86))
    logo.resize((500, 500), Image.LANCZOS).save(ROOT / "docs/logo.png")

    print("Icons generated.")


if __name__ == "__main__":
    main()
