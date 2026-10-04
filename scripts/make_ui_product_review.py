"""Build review boards from unmodified real-device captures, with source hashes."""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

NAMES = (
    "01-default", "02-soft", "03-rainy", "04-sakura", "05-y2k", "06-midnight",
    "07-theme-center", "08-theme-detail", "09-wallpaper-center", "10-icon-center",
    "11-my-theme", "12-theme-lab", "13-app-library", "14-lockscreen",
    "15-notification", "16-control-center", "17-chat", "18-living",
)


def make(source, destination, names, columns, width, title):
    sources = [source / (name + ".png") for name in names]
    missing = [str(path) for path in sources if not path.is_file()]
    if missing:
        raise SystemExit("Missing actual captures: " + ", ".join(missing))
    height, gap, heading, caption = round(width * 2340 / 1080), 24, 66, 34
    rows = (len(names) + columns - 1) // columns
    board = Image.new("RGB", (gap + columns * (width + gap), heading + rows * (height + caption + gap)), "#eeeef0")
    draw = ImageDraw.Draw(board)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 18)
    except OSError:
        font = ImageFont.load_default()
    draw.text((gap, 18), title + " | real Xiaomi Android 10 captures", fill="#22232b", font=font)
    metadata = []
    for index, path in enumerate(sources):
        x, y = gap + index % columns * (width + gap), heading + index // columns * (height + caption + gap)
        with Image.open(path) as image:
            if image.size != (1080, 2340):
                raise SystemExit("Unexpected device screenshot dimensions: " + str(path) + " " + str(image.size))
            board.paste(image.convert("RGB").resize((width, height), Image.Resampling.LANCZOS), (x, y))
        draw.text((x, y + height + 7), path.stem, fill="#22232b", font=font)
        metadata.append({"source": str(path.resolve()), "width": 1080, "height": 2340,
                         "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    destination.parent.mkdir(parents=True, exist_ok=True)
    board.save(destination, quality=94, subsampling=0)
    destination.with_suffix(".json").write_text(json.dumps({
        "board": str(destination.resolve()), "unmodifiedDeviceScreenshots": True,
        "boardTransform": "Proportional resize only; captions outside device images", "sources": metadata,
    }, indent=2), encoding="utf-8")
    print(destination)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--section", choices=("themes", "center", "system", "all"), default="all")
    args = parser.parse_args()
    sections = {
        "themes": (NAMES[:6], "six-theme-home-review.jpg", 3, 360, "Six theme homes"),
        "center": (NAMES[6:12], "theme-center-review.jpg", 3, 360, "Theme Center and Theme Lab"),
        "system": (NAMES[12:], "system-app-review.jpg", 3, 360, "System surfaces and apps"),
        "all": (NAMES, "ui-product-review-board.jpg", 6, 240, "AILUA UI product review"),
    }
    selected = list(sections) if args.section == "all" else [args.section]
    for section in selected:
        names, filename, columns, width, title = sections[section]
        make(args.source, args.output / filename, names, columns, width, title)


if __name__ == "__main__":
    main()
