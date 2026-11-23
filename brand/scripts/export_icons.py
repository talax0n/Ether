# Run: uv run --with numpy --with pillow brand/scripts/export_icons.py
"""Export logo #2 (swipe spark) into launcher icon, in-app mark and Instagram avatar."""
import sys
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "brand/logos/ether-logo-2-swipe-spark.png"
RES = ROOT / "app/src/main/res"
CORAL = np.array([0xFF, 0x5A, 0x36], float)
INK = np.array([0x1E, 0x1B, 0x2E], float)
CREAM = np.array([0xFF, 0xF7, 0xEA], float)

img = np.asarray(Image.open(SRC).convert("RGB"), float)
# Pixel = a*F + (1-a)*CORAL. F is ink or cream; pick the nearer one, solve a by projection.
def unmix(fg):
    d = fg - CORAL
    return np.clip(((img - CORAL) @ d) / (d @ d), 0, 1)
a_ink, a_cream = unmix(INK), unmix(CREAM)
use_cream = a_cream >= a_ink
alpha = np.where(use_cream, a_cream, a_ink)
rgb = np.where(use_cream[..., None], CREAM, INK)
mark = Image.fromarray(np.dstack([rgb, alpha * 255]).astype(np.uint8), "RGBA")
bbox = mark.getbbox()
mark = mark.crop(bbox)
# Monochrome: front card solid, back card half alpha so the two cards stay readable when tinted.
mono_a = np.where(use_cream, a_cream, a_ink * 0.45)
mono = Image.fromarray(np.dstack([np.full(img.shape, 255.0), mono_a * 255]).astype(np.uint8), "RGBA")
mono = mono.crop(bbox)

def place(m, canvas, frac, bg=None):
    """Center m on a square canvas, longest side = frac*canvas."""
    s = frac * canvas / max(m.size)
    m = m.resize((round(m.width * s), round(m.height * s)), Image.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), bg or (0, 0, 0, 0))
    out.alpha_composite(m, ((canvas - m.width) // 2, (canvas - m.height) // 2))
    return out

# Adaptive icon: 108dp canvas, visible circle 72dp. 0.46 keeps the rotated card corners inside it.
FRAC = 0.46
for dpi, px in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
    place(mark, px, FRAC).save(RES / f"mipmap-{dpi}/ic_launcher_foreground.png", optimize=True)
    place(mono, px, FRAC).save(RES / f"mipmap-{dpi}/ic_launcher_monochrome.png", optimize=True)

h = 384
mark.resize((round(mark.width * h / mark.height), h), Image.LANCZOS).save(RES / "drawable-nodpi/ether_mark.png", optimize=True)
print("ether_mark aspect:", round(mark.width * h / mark.height), "/", h)

# Instagram avatar: 1080 square, shown as a circle, so keep the mark well inside it.
ig = place(mark, 1080, 0.56, bg=(0xFF, 0x5A, 0x36, 255)).convert("RGB")
ig.save(ROOT / "brand/ether-instagram-1080.png", optimize=True)

# Play Store icon: 512 full-bleed square (Play applies its own mask).
place(mark, 512, 0.62, bg=(0xFF, 0x5A, 0x36, 255)).convert("RGB").save(ROOT / "brand/ether-playstore-512.png", optimize=True)
