"""Arrange actual UIR-3 phone screenshots without retouching their UI pixels."""
import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


SHOTS = (
    ("AILUA Default", "01-default-icons"),
    ("Soft Home", "02-soft-icons"),
    ("Midnight Glass", "03-midnight-icons"),
    ("Y2K icon prototype", "04-y2k-prototype"),
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    args = parser.parse_args()
    directory = args.directory
    font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 20)
    metadata = []
    for suffix, output in (("", "four-styles-device.png"), ("-folder", "four-styles-folders.png")):
        images = []
        for label, stem in SHOTS:
            path = directory / (stem + suffix + ".png")
            with Image.open(path) as source:
                images.append((label, source.convert("RGB")))
                metadata.append({"path": path.name, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                                 "width": source.width, "height": source.height})
        width = 360
        height = round(images[0][1].height * width / images[0][1].width)
        board = Image.new("RGB", (4 * width, height + 72), "#e9eaf0")
        draw = ImageDraw.Draw(board)
        for index, (label, source) in enumerate(images):
            draw.text((index * width + 12, 12), label, font=font, fill="#182134")
            board.paste(source.resize((width, height), Image.Resampling.LANCZOS), (index * width, 48))
        draw.text((12, height + 49), "Same existing Workspace | Xiaomi Android 10 | Actual screenshots", font=font, fill="#182134")
        board.save(directory / output)

    # Deliberately exclude wallpaper and widgets: inspect the same real app and Dock areas.
    # Coordinates are proportions so original screenshots remain independent of device density.
    strips = []
    for label, stem in SHOTS:
        with Image.open(directory / (stem + ".png")) as source:
            for area, bounds in (
                ("Apps", (0.025, 0.495, 0.975, 0.725)),
                ("Dock", (0.025, 0.785, 0.975, 0.895)),
            ):
                crop = source.crop(tuple(round(value * (source.width if i % 2 == 0 else source.height))
                                         for i, value in enumerate(bounds)))
                crop = crop.convert("RGB")
                crop.thumbnail((480, 235), Image.Resampling.LANCZOS)
                strips.append((label, area, crop))
    row_height = 300
    board = Image.new("RGB", (1040, 4 * row_height + 36), "#e9eaf0")
    draw = ImageDraw.Draw(board)
    for index, (label, area, crop) in enumerate(strips):
        row, column = index // 2, index % 2
        draw.text((column * 520 + 12, row * row_height + 8), f"{label} / {area}", font=font, fill="#182134")
        board.paste(crop, (column * 520 + 12, row * row_height + 40))
    board.save(directory / "icons-only-device.png")
    (directory / "comparison-metadata.json").write_text(json.dumps({
        "source": "Actual Android UiAutomation screenshots; no visual retouching",
        "fullBoardTransform": "proportional Lanczos resize and labels outside screenshots",
        "iconBoardTransform": "fixed proportional crops of the real app and Dock areas",
        "screenshots": metadata,
    }, indent=2), encoding="utf-8")
    print(directory / "four-styles-device.png")
    print(directory / "four-styles-folders.png")
    print(directory / "icons-only-device.png")


if __name__ == "__main__":
    main()
