"""Render the AILUA moon mark for pre-Android-8 launcher icon resources."""

from pathlib import Path
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
SCALE = 4
SIZE = 192 * SCALE


def rgba(hex_color: str, alpha: int = 255):
    value = hex_color.removeprefix("#")
    return tuple(int(value[index : index + 2], 16) for index in (0, 2, 4)) + (alpha,)


def render(round_icon: bool) -> Image.Image:
    background = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    mask = Image.new("L", (SIZE, SIZE), 0)
    md = ImageDraw.Draw(mask)
    if round_icon:
        md.ellipse((0, 0, SIZE - 1, SIZE - 1), fill=255)
    else:
        md.rounded_rectangle((0, 0, SIZE - 1, SIZE - 1), radius=SIZE // 5, fill=255)

    gradient = Image.new("RGBA", (SIZE, SIZE))
    pixels = gradient.load()
    top, bottom = (38, 34, 53), (20, 19, 28)
    for y in range(SIZE):
        fraction = y / (SIZE - 1)
        color = tuple(round(top[i] * (1 - fraction) + bottom[i] * fraction) for i in range(3)) + (255,)
        for x in range(SIZE):
            pixels[x, y] = color
    background.paste(gradient, (0, 0), mask)

    ink = ImageDraw.Draw(background)
    s = SCALE
    ink.arc((33 * s, 88 * s, 166 * s, 145 * s), 8, 321, fill=rgba("#8798D4", 150), width=3 * s)
    crescent = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    cd = ImageDraw.Draw(crescent)
    cd.ellipse((60 * s, 47 * s, 146 * s, 151 * s), fill=rgba("#F6F1E6"))
    cd.ellipse((88 * s, 37 * s, 166 * s, 127 * s), fill=(0, 0, 0, 0))
    background.alpha_composite(crescent)
    ink = ImageDraw.Draw(background)
    ink.polygon([(138 * s, 72 * s), (143 * s, 83 * s), (154 * s, 88 * s), (143 * s, 93 * s), (138 * s, 104 * s), (133 * s, 93 * s), (122 * s, 88 * s), (133 * s, 83 * s)], fill=rgba("#E1D7FF"))
    ink.polygon([(70 * s, 62 * s), (73 * s, 69 * s), (80 * s, 72 * s), (73 * s, 75 * s), (70 * s, 82 * s), (67 * s, 75 * s), (60 * s, 72 * s), (67 * s, 69 * s)], fill=rgba("#C6D3FF"))
    return background


for density, pixels in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    folder = ROOT / f"mipmap-{density}"
    for round_icon, suffix in ((False, ""), (True, "_round")):
        image = render(round_icon).resize((pixels, pixels), Image.Resampling.LANCZOS)
        image.save(folder / f"ic_launcher{suffix}.webp", "WEBP", quality=95)
