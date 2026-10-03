"""Assemble original device screenshots into an unretouched UI review board.

Run after capturing the final APK, for example:
    python scripts/render_theme_review_board.py dist/qa/P5.V-UI-Theme/UIR-2
    python scripts/render_theme_review_board.py dist/qa/P5.V-UI-Theme/UIR-2 \
        --before-dir dist/qa/P5.V-UI-Theme/rejected-gray-frame

Only proportional resizing and RGB conversion for JPEG output are performed.
There is no cropping, recoloring, blurring, UI repainting or source-file write.
Dependencies: Pillow. Sources must be screenshots of the Android 10 / API 29 run.
"""

import argparse
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


PANELS = (
    ("01-default-home.png", "AILUA Default"),
    ("02-soft-home.png", "Soft Home"),
    ("09-midnight-glass.png", "Midnight Glass"),
)
PANEL_WIDTH = 420
MARGIN = 32
HEADER_HEIGHT = 112
FOOTER_HEIGHT = 110
BACKGROUND = "#F5F5F4"
PRIMARY = "#20252D"
SECONDARY = "#5E646E"
API_NOTE = "Android 10 / API 29: tint + highlight fallback, not hardware backdrop blur."
SOURCE_NOTE = "Original device PNGs are preserved. Screenshots are only scaled proportionally."


def font(size, bold=False):
    windows_fonts = Path(os.environ.get("WINDIR", "C:/Windows")) / "Fonts"
    candidates = (
        windows_fonts / ("segoeuib.ttf" if bold else "segoeui.ttf"),
        Path("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"),
    )
    for candidate in candidates:
        try:
            return ImageFont.truetype(str(candidate), size)
        except OSError:
            pass
    return ImageFont.load_default()


def required_image(path):
    if not path.is_file():
        raise FileNotFoundError(f"Missing device screenshot: {path}")
    with Image.open(path) as source:
        return source.convert("RGB")


def wrap_text(draw, text, text_font, width):
    lines, current = [], ""
    for word in text.split():
        proposed = f"{current} {word}" if current else word
        if current and draw.textlength(proposed, font=text_font) > width:
            lines.append(current)
            current = word
        else:
            current = proposed
    if current:
        lines.append(current)
    return lines


def render_board(panels, title, output):
    """Panels are (label, original image); only an equal width scale is applied."""
    scaled = []
    for label, source in panels:
        height = round(source.height * PANEL_WIDTH / source.width)
        image = source.resize((PANEL_WIDTH, height), Image.Resampling.LANCZOS)
        scaled.append((label, image))
    image_height = max(image.height for _, image in scaled)
    board_width = PANEL_WIDTH * len(scaled) + MARGIN * (len(scaled) + 1)
    board_height = HEADER_HEIGHT + image_height + FOOTER_HEIGHT
    board = Image.new("RGB", (board_width, board_height), BACKGROUND)
    draw = ImageDraw.Draw(board)
    title_font, label_font, note_font = font(28, True), font(22), font(17)
    draw.text((MARGIN, 20), title, fill=PRIMARY, font=title_font)
    for index, (label, screenshot) in enumerate(scaled):
        x = MARGIN + index * (PANEL_WIDTH + MARGIN)
        draw.text((x, 72), label, fill=PRIMARY, font=label_font)
        # No crop: differing aspect ratios receive neutral space below the image.
        board.paste(screenshot, (x, HEADER_HEIGHT))
    note_y = HEADER_HEIGHT + image_height + 18
    for note in (API_NOTE, SOURCE_NOTE):
        for line in wrap_text(draw, note, note_font, board_width - MARGIN * 2):
            draw.text((MARGIN, note_y), line, fill=SECONDARY, font=note_font)
            note_y += 24
    output.parent.mkdir(parents=True, exist_ok=True)
    board.save(output, "JPEG", quality=95, subsampling=0, optimize=True)
    print(f"Saved {output} ({board_width}x{board_height}); original PNGs unchanged.")


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("screenshot_dir", type=Path,
                        help="Directory containing 01-default-home.png, 02-soft-home.png and 09-midnight-glass.png")
    parser.add_argument("--before-dir", type=Path,
                        help="Optional rejected-gray-frame directory containing old 01-default-home.png")
    parser.add_argument("--output-dir", type=Path,
                        help="Output directory; defaults to screenshot_dir")
    args = parser.parse_args()
    output_dir = args.output_dir or args.screenshot_dir
    try:
        screenshots = [(name, title, required_image(args.screenshot_dir / name))
                       for name, title in PANELS]
        before_image = None
        if args.before_dir:
            before_path = args.before_dir / PANELS[0][0]
            if not before_path.is_file():
                before_path = args.before_dir / "rejected-gray-frame" / PANELS[0][0]
            before_image = required_image(before_path)
        render_board([(title, image) for _, title, image in screenshots],
                     "AILUA P5.V - Device UI / Theme Review", output_dir / "ui-theme-review.jpg")
        if before_image is not None:
            render_board([("Before: rejected gray material", before_image),
                          ("After: AILUA Default", screenshots[0][2])],
                         "AILUA P5.V - Material Before / After",
                         output_dir / "material-before-after.jpg")
    except (OSError, ValueError) as error:
        parser.exit(1, f"Review board was not generated: {error}\n")


if __name__ == "__main__":
    main()
