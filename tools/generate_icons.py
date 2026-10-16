#!/usr/bin/env python3
"""Generates every launcher, adaptive-icon, logo and store asset from `sarim.svg`.

Source of truth: the master SVG at the repo root (1024x1024, full-bleed field with
the shield mark). Everything else is derived:

  - legacy launcher `ic_launcher.webp` (rounded-square, full-bleed field)
  - legacy round `ic_launcher_round.webp` (circle mask)
  - adaptive `<foreground>` `ic_launcher_foreground.webp` (transparent, mark in the
    66/108 safe zone; the field itself comes from ic_launcher_background.xml)
  - adaptive `<monochrome>` `ic_launcher_monochrome.webp` (white mark)
  - Play Store 512px full-bleed PNG (app + fastlane metadata)
  - README `docs/logo.png` (transparent, mark only)

Usage:  python3 tools/generate_icons.py
"""

import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SVG = ROOT / "sarim.svg"

RESDIR = ROOT / "app/src/main/res"
MASTER = 1024

ADAPTIVE_CANVAS = 108  # dp; the foreground artwork lives in the middle 66dp
SAFE_ZONE_RATIO = 58 / 108


def render_svg(width: int, transparent: bool = False) -> Image.Image:
    """Rasterizes the SVG at `width` px. `transparent` drops the background rect."""
    if transparent:
        src = SVG.read_text(encoding="utf-8")
        # The first path is the full-canvas field; removing it leaves mark-on-clear.
        svg = src.replace(
            '<path fill="#F6FBF3" d="M0 0L1024 0L1024 1024L0 1024L0 0Z"/>', ""
        )
        if svg == src:
            sys.exit("could not strip the background rect from sarim.svg")
        svg_file = tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False)
        svg_file.write(svg)
        svg_file.close()
        path = svg_file.name
    else:
        path = str(SVG)

    out = tempfile.NamedTemporaryFile(suffix=".png", delete=False)
    out.close()
    subprocess.run(
        ["rsvg-convert", "-w", str(width), "-h", str(width), "-o", out.name, path],
        check=True,
    )
    img = Image.open(out.name).convert("RGBA")
    Path(out.name).unlink()
    return img


def cropped_mark(rendered: Image.Image) -> Image.Image:
    """The mark alone, tightly bbox-cropped, on transparency."""
    bbox = rendered.getbbox()
    if bbox is None:
        sys.exit("sarim.svg rendered empty")
    return rendered.crop(bbox)


def rounded_mask(size: int, radius_ratio: float) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size - 1, size - 1], radius=round(radius_ratio * size), fill=255
    )
    return mask


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def tinted(mark: Image.Image, tint: tuple[int, int, int]) -> Image.Image:
    """Solid-color copy of the mark, preserving its alpha."""
    solid = Image.new("RGBA", mark.size, tint + (255,))
    out = Image.new("RGBA", mark.size)
    out.paste(solid, (0, 0), mask=mark.split()[3])
    return out


def compose_adaptive(mark: Image.Image, size: int) -> Image.Image:
    """Transparent 108dp canvas with the mark centered inside the safe zone."""
    target = round(size * SAFE_ZONE_RATIO)
    ratio = target / max(mark.width, mark.height)
    fitted = mark.resize((round(mark.width * ratio), round(mark.height * ratio)), Image.LANCZOS)

    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(fitted, ((size - fitted.width) // 2, (size - fitted.height) // 2))
    return canvas


def main() -> None:
    field = render_svg(MASTER)
    mark = cropped_mark(render_svg(MASTER, transparent=True))

    ladder = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for bucket, scale in ladder.items():
        dpi_dir = RESDIR / f"mipmap-{bucket}"
        legacy_px = int(48 * scale)

        legacy = field.resize((MASTER, MASTER), Image.LANCZOS).convert("RGBA")
        legacy.putalpha(rounded_mask(MASTER, 0.18))
        legacy.resize((legacy_px, legacy_px), Image.LANCZOS).save(
            dpi_dir / "ic_launcher.webp", lossless=True
        )

        round_icon = field.resize((MASTER, MASTER), Image.LANCZOS).convert("RGBA")
        round_icon.putalpha(circle_mask(MASTER))
        round_icon.resize((legacy_px, legacy_px), Image.LANCZOS).save(
            dpi_dir / "ic_launcher_round.webp", lossless=True
        )

        fg_px = int(ADAPTIVE_CANVAS * scale)
        compose_adaptive(mark, fg_px).save(dpi_dir / "ic_launcher_foreground.webp", lossless=True)
        compose_adaptive(tinted(mark, (255, 255, 255)), fg_px).save(
            dpi_dir / "ic_launcher_monochrome.webp", lossless=True
        )

    # Play Store listing icon: 512x512, full-bleed (no alpha), 32-bit PNG.
    store = field.convert("RGB").resize((512, 512), Image.LANCZOS)
    store.save(ROOT / "app/src/main/ic_launcher-playstore.png")
    store.save(ROOT / "fastlane/metadata/android/en-US/images/icon.png")
    store.save(ROOT / "fastlane/metadata/android/ar-SA/images/icon.png")

    # README logo: transparent, mark only, 500px wide.
    mark.resize((500, round(500 * mark.height / mark.width)), Image.LANCZOS).save(
        ROOT / "docs/logo.png"
    )

    print("Icons generated from sarim.svg.")


if __name__ == "__main__":
    main()
