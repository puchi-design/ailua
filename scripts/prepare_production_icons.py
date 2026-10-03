"""Prepare and finish the UIR-3 icon assets; this script makes no API calls.

    python scripts/prepare_production_icons.py prompts
    python scripts/prepare_production_icons.py audit
    python scripts/prepare_production_icons.py normalize
    python scripts/prepare_production_icons.py board --source raw --trial
    python scripts/prepare_production_icons.py board
    python scripts/prepare_production_icons.py manifest

Generation uses the imagegen skill's bundled CLI, outside this script. Pass
--no-augment to that CLI: it is a global flag, not a supported JSONL job field.
Production-candidate status requires an existing normalized asset and a recorded
visual review of its hash. Commercial service rights remain pending review.
Never replace another asset or any wallpaper in this script.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
from datetime import datetime, timezone
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / "output/imagegen/uir3"
RAW = WORK / "raw"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
MANIFEST = ROOT / "docs/design/UIR3_ICON_ASSET_MANIFEST.json"
VERSIONED_PROMPTS = ROOT / "docs/design/UIR3_ICON_PROMPTS.jsonl"
PACKS = ("default", "soft", "midnight", "y2k")
KEYS = (
    "chat", "living", "moments", "gallery", "contacts", "mailbox", "call",
    "memories", "relations", "diary", "check_phone", "theater", "world",
    "lore", "creator", "assistant", "apps", "settings",
)
TRIAL_KEYS = ("chat", "living", "gallery", "settings")
SERVICE = "https://www.vibework.live/v1"
MODEL = "gpt-image-2"

# Subjects are distinct objects, not the same glyph tinted eighteen times.
# There are no character portraits, third-party marks, letters, or numbers.
SUBJECTS = {
    "chat": (
        "a single bold asymmetric rounded conversation bubble with a short tail and three large clean inset dots",
        "two overlapping plump ceramic conversation bubbles, the front apricot bubble with a tiny speech tail, the rear ivory bubble, no face",
        "a substantial cobalt enamel conversation bubble with an angular tail and three pale recessed dots",
        "a pink pixel speech balloon with an angular stepped tail and three dark purple square dots",
    ),
    "living": (
        "a bold white house silhouette with a coral roof peak, a wide blue door cutout and one square window",
        "a large warm ivory ceramic teacup with a terracotta tea surface, curved handle and two gentle steam strokes",
        "a substantial sculpted little navy house with a violet roof and one warm lit square window",
        "a chunky lavender pixel house with a cobalt stepped roof, pink square window and dark violet door",
    ),
    "moments": (
        "a bold custom six-petal coral bloom with a golden circular center, large petals forming a lively social-feed symbol",
        "a single large coral cosmos flower with uneven hand-painted petals, golden center and a short sage stem",
        "a six-petal violet enamel flower with a softly luminous warm pink center, sculptural broad petals",
        "a bright pink pixel flower with six stepped petals, lemon center and a short cobalt stem",
    ),
    "gallery": (
        "two slightly overlapping rectangular photo prints; the front has a simple teal mountain and a golden sun, thick white margin",
        "two cream paper photograph prints with warm peach borders; the top landscape contains a sage hill and a golden sun",
        "two overlapping deep blue physical photo tiles; the front carries a pale indigo mountain and small amber sun",
        "two offset pixel photograph tiles with square white borders, blue stepped mountains and a pink square sun",
    ),
    "contacts": (
        "a chunky coral address book with a thick blue spine, two side index tabs and a simple white abstract person glyph on its cover",
        "a small cloth-bound tan address book with three warm colored side index tabs and a plain round brass closure, no writing",
        "a thick violet address book with three blue index tabs and a large silver abstract person glyph embossed on its cover",
        "a lavender pixel address book with cobalt square index tabs and a simple tiny pink abstract contact glyph",
    ),
    "mailbox": (
        "a large white closed envelope with a strong blue V-shaped fold and small coral wax dot, clean distinct silhouette",
        "a single warm cream paper envelope with an apricot flap and a small round terracotta wax seal, tactile folded edges",
        "a substantial dark violet folded envelope with a crisp pale blue V-fold and small warm pink wax seal",
        "a large pink pixel envelope with a dark purple stepped V-fold and cobalt square seal",
    ),
    "call": (
        "a large sculpted white telephone receiver diagonally placed, with broad rounded ends and a short coral signal arc",
        "a warm coral vintage telephone handset with a tiny curved cord ending, ceramic collectible quality, no keypad",
        "a substantial indigo telephone receiver with broad polished enamel ends and a very small cyan highlight",
        "a cobalt pixel telephone handset with two chunky square ends and a short stepped pink signal mark",
    ),
    "memories": (
        "a white memory keepsake box with a coral lift-off lid and a golden circular latch, simple bold three-quarter view",
        "a little honey-colored wooden keepsake box with a lid slightly ajar revealing a folded cream photograph, no writing",
        "a substantial blue-violet keepsake box with a satin indigo lid and one tiny amber latch, lid slightly ajar",
        "a pink pixel treasure keepsake chest with cobalt lid, square brass latch and visibly stepped corners",
    ),
    "relations": (
        "two large interlocking coral and white chain links, clearly woven together, bold asymmetrical rounded link shapes",
        "two interlocking little glazed ceramic links, one terracotta and one cream, tactile and gently irregular",
        "two interlocking substantial metallic blue and violet links with one faint rose edge highlight",
        "two interlocked pink and cobalt pixel chain links, square loop openings and stepped corners",
    ),
    "diary": (
        "a closed white diary with a coral vertical binding strip and a golden ribbon bookmark hanging from its lower edge",
        "a cloth-bound rose diary with cream page edges, a sage ribbon bookmark and a little plain elastic band, no writing",
        "a substantial indigo diary with dark blue page edges and a narrow warm pink ribbon bookmark",
        "a lavender pixel diary with blue block spine, white square page edges and a pink stepped ribbon bookmark",
    ),
    "check_phone": (
        "one upright white smartphone pictogram with a dark blue screen, small coral notification dot and a thick bottom home line",
        "one little vintage ivory mobile phone object with a muted sage screen, coral notification dot and plain cream buttons",
        "one upright substantial midnight-blue phone object with an indigo screen and a single tiny warm notification dot",
        "one chunky lavender pixel mobile phone with a blue rectangular screen, pink square notification and three plain square buttons",
    ),
    "theater": (
        "a white theater stage curtain shape parted around a large coral play triangle, custom broad sweeping folds",
        "a small warm terracotta theater curtain, softly folded fabric parted around a cream triangular play emblem",
        "substantial deep blue theater curtains framing a large violet play triangle with a very restrained pink edge light",
        "pink pixel theater curtains with stepped folds, parted around a cobalt pixel play triangle",
    ),
    "world": (
        "a bold white globe with thick teal meridian bands and a coral tilted orbit stroke, no letters or map labels",
        "a tiny ceramic globe with sage land shapes, powder-blue sea and a warm wooden support, no geographic labels",
        "a substantial blue planet globe with violet land silhouettes and a thin pale blue meridian, gentle physical depth",
        "a cobalt and lavender pixel globe with stepped pink continents and a short square pedestal",
    ),
    "lore": (
        "a bold open white book with golden page surfaces and a coral diamond bookmark hovering immediately above its central fold",
        "an open cream storybook with warm tan binding and one pressed sage leaf in the central fold, no text or page marks",
        "a substantial open violet codex with pale indigo blank pages and a small warm amber diamond bookmark",
        "an open pink pixel book with white block pages, cobalt spine and one lavender diamond bookmark above the fold",
    ),
    "creator": (
        "a bold white paintbrush with a coral handle crossing a small golden four-point creative sparkle, no pencil letters",
        "a wooden paintbrush with apricot bristles crossing a tiny cream painter palette bearing three warm colored paint spots",
        "a substantial violet paintbrush with an indigo handle and a tiny pale cyan four-point creative sparkle",
        "a cobalt and pink pixel paintbrush next to a single small lavender pixel sparkle, strong block silhouette",
    ),
    "assistant": (
        "one large white four-point compass-like assistant sparkle with a coral central diamond, precise custom non-Material design",
        "one little glazed golden star charm with a soft four-point shape and a plain round cream center, no face",
        "one substantial violet four-point assistant star with a pale cyan central diamond and restrained luminous edges",
        "one large pink four-point pixel star with cobalt stepped outline and a lavender square center",
    ),
    "apps": (
        "four unequal colorful mini app tiles in a clear two-by-two cluster, coral, golden yellow, white and teal, large square silhouettes",
        "four little tactile ceramic tiles in a tidy two-by-two group, apricot, sage, ivory and dusty blue, no glyphs or lettering",
        "four substantial blue and violet cuboid tiles arranged in a tidy two-by-two group, each clearly solid with restrained highlights",
        "four square pixel program tiles in a two-by-two cluster, pink, lavender, cobalt and white, dark purple block outlines",
    ),
    "settings": (
        "a large custom white six-tooth mechanical gear with a coral circular hub, thick friendly teeth and very clean silhouette",
        "a small warm ivory ceramic six-tooth gear with an apricot center hole, tactile handcrafted edges and minimal shade",
        "a substantial sculpted violet six-tooth mechanical gear with an indigo center opening and restrained pale-blue bevel highlights",
        "a chunky lavender pixel gear with exactly six broad stepped teeth, cobalt square center opening and dark purple outline",
    ),
}

TILE_COLORS = {
    "chat": "bright cobalt blue", "living": "fresh teal", "moments": "warm sunflower yellow",
    "gallery": "coral red", "contacts": "warm apricot", "mailbox": "saturated sky blue",
    "call": "vivid leaf green", "memories": "deep lavender", "relations": "rose pink",
    "diary": "soft cyan blue", "check_phone": "saturated violet", "theater": "deep coral",
    "world": "emerald teal", "lore": "warm terracotta", "creator": "rich violet",
    "assistant": "cobalt blue", "apps": "deep cobalt", "settings": "slate blue",
}
PACK_STYLE = {
    "default": (
        "AILUA Default: mature real-smartphone app artwork. Crisp custom vector-like shapes, medium to high saturation, full-bleed colored tile, excellent tiny-size readability. Broad flat forms with very slight physical shading, no long shadow, no generic Material glyph.",
        "Opaque full-canvas {color} background, smooth and clean; it must fill every edge and corner. No outer frame. The launcher applies its squircle mask outside this asset.",
        "Confident vivid color with bright white subject parts and restrained coral or golden accent. Minimal shadow; avoid white fog.",
    ),
    "soft": (
        "Soft Home: warm collectible still-life illustration, gently hand-painted ceramic, folded paper, cloth or wood according to the subject. Warm tactile detail and a simple unmistakable silhouette, polished charming illustration rather than a photographic room.",
        "Opaque full-bleed warm cream and pale peach tile backdrop with a very subtle painted texture, filling every edge and corner. No tabletop scene, no room, no outer card or inset border. The launcher applies its rounded mask outside this asset.",
        "Apricot, terracotta, warm ivory, muted sage, dusty blue, honey wood. Soft warm diffuse light, subtle compact contact shade directly underneath the object.",
    ),
    "midnight": (
        "Midnight Glass icon artwork: a substantial opaque physical sculpted icon, satin enamel and solid blue-violet forms, precise large silhouette. Deep color gradient is inside the solid tile and object. A few restrained cyan or rose edge highlights, no glass rings, no translucent object.",
        "Opaque full-bleed deep navy-to-indigo-to-violet gradient tile, filling every edge and corner, rich dark color without pure black emptiness. No floating surrounding halo, no outer border. The launcher applies its rounded mask outside this asset.",
        "Dark navy, cobalt, indigo, violet; small pale blue or warm pink highlights only. Controlled studio lighting and physical depth, no bloom haze or busy neon.",
    ),
    "y2k": (
        "Y2K Love PC: authentic 16/32-bit late-1990s desktop pixel art, designed on a visible 32-by-32 pixel grid. Each coarse square pixel and stepped diagonal remains explicit. Limited pastel pink, lavender, cobalt blue and deep purple palette. Old PC program-icon charm, selective one-pixel object bevels, no modern smooth vector or Material style.",
        "Opaque flat lavender full-bleed square background with sharp square corners. No rounded app card, no glass tile, no window title bar or interface frame. The asset is one independent square pixel icon, not a screenshot.",
        "Pastel pink, lavender, electric cobalt, white, deep purple outlines. Discrete hard color steps, no antialiasing, no continuous gradients, no smooth airbrush glow.",
    ),
}


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def valid_raw_image(path: Path) -> bool:
    if not path.is_file():
        return False
    try:
        with Image.open(path) as image:
            if image.format != "PNG":
                return False
            image.verify()
        return True
    except (OSError, ValueError, SyntaxError):
        return False


def icon_prompt(pack: str, key: str) -> str:
    style, backdrop, light = PACK_STYLE[pack]
    subject = SUBJECTS[key][PACKS.index(pack)]
    phone_note = (
        "The mobile-phone object is the actual app metaphor; do not place the asset inside an additional phone mockup."
        if key == "check_phone" else "Do not place the artwork inside a phone mockup."
    )
    return "\n".join((
        "Use case: stylized-concept",
        "Asset type: one production AILUA launcher app icon, single independent image; export 1024x1024 for downsampling to 256x256.",
        f"Primary request: create only the {key} app icon in the {pack} icon family.",
        f"Scene/backdrop: {backdrop.format(color=TILE_COLORS[key])}",
        f"Subject: {subject}.",
        f"Style/medium: {style}",
        "Composition/framing: one centered large subject occupying approximately 68 to 78 percent of the image width and height. Keep silhouette intact, with a consistent compact margin. Front-facing or restrained three-quarter view as needed for the physical object; strong contrast at 56dp, dock size and folder thumbnail size.",
        f"Lighting/mood: {light}",
        "Text: none. All book pages, screens, envelopes and objects are blank or use abstract shapes only. No readable letters, numbers or fake writing.",
        f"Constraints: exactly one icon asset per image; opaque image including all four corners; no icon sheet or montage; no character portrait or character art; {phone_note}",
        "Avoid: people, faces, human hands, logos, brands, watermarks, captions, labels, UI controls, device screenshot, background scene, additional card frame, thin faint glyphs, transparent circular bases, excessive shadow, tiny peripheral decoration.",
    ))


def jobs() -> list[dict]:
    return [{
        "prompt": icon_prompt(pack, key), "model": MODEL, "n": 1,
        "size": "1024x1024", "quality": "medium", "output_format": "png",
        "background": "opaque", "out": f"{pack}-{key}.png",
    } for pack in PACKS for key in KEYS]


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_jsonl(path: Path, records: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(job, ensure_ascii=False) + "\n" for job in records), encoding="utf-8")
    print(f"Wrote {relative(path)}: {len(records)} single-icon jobs")


def prepare_prompts(args) -> None:
    all_jobs = jobs()
    # Keep reproducible prompt text in Git while raw API outputs stay local.
    write_jsonl(VERSIONED_PROMPTS, all_jobs)
    trial_names = {f"{pack}-{key}.png" for pack in PACKS for key in TRIAL_KEYS}
    selections = {
        "all": all_jobs,
        "trial": [job for job in all_jobs if job["out"] in trial_names],
        "remaining": [job for job in all_jobs if job["out"] not in trial_names],
    }
    for name, records in selections.items():
        write_jsonl(WORK / f"prompts-{name}.jsonl", records)
    for pack in PACKS:
        write_jsonl(WORK / f"prompts-{pack}.jsonl", [j for j in all_jobs if j["out"].startswith(pack + "-")])
        write_jsonl(WORK / f"prompts-remaining-{pack}.jsonl", [j for j in selections["remaining"] if j["out"].startswith(pack + "-")])
    write_jsonl(WORK / "prompts-remaining-first-three.jsonl", [j for j in selections["remaining"] if not j["out"].startswith("y2k-")])
    write_json(WORK / "generation-request.json", {
        "serviceBaseUrl": SERVICE, "requestedModel": MODEL,
        "size": "1024x1024", "quality": "medium", "outputFormat": "png",
        "background": "opaque", "imagesPerJob": 1,
        "globalCliFlags": ["--no-augment"], "supportedJobFields": list(all_jobs[0]),
        "generator": "C:/Users/fan/.codex/skills/.system/imagegen/scripts/image_gen.py generate-batch",
        "rawOutputDirectory": relative(RAW),
        "note": "This file contains no credential. The preparation script does not call the service.",
    })
    RAW.mkdir(parents=True, exist_ok=True)
    update_manifest(args)


def selected_pairs(args):
    return [(pack, key) for pack in PACKS if not args.pack or pack in args.pack
            for key in (TRIAL_KEYS if getattr(args, "trial", False) else KEYS)]


def audit_raw(args) -> None:
    """Produce a resumable batch containing only absent or invalid PNG assets."""
    complete, incomplete = [], []
    for pack, key in selected_pairs(args):
        path = RAW / f"{pack}-{key}.png"
        item = {"packId": pack, "iconKey": key, "rawPath": relative(path)}
        if valid_raw_image(path):
            with Image.open(path) as image:
                item.update({"widthPx": image.width, "heightPx": image.height})
            item.update({"sizeBytes": path.stat().st_size, "sha256": sha256(path)})
            complete.append(item)
        else:
            item.update({"status": "invalid" if path.is_file() else "missing",
                         "sizeBytes": path.stat().st_size if path.is_file() else 0})
            incomplete.append(item)
    names = {f"{item['packId']}-{item['iconKey']}.png" for item in incomplete}
    write_jsonl(WORK / "prompts-recovery-missing.jsonl", [job for job in jobs() if job["out"] in names])
    write_json(WORK / "raw-audit.json", {
        "createdAtUtc": now(), "completeCount": len(complete),
        "complete": complete, "incomplete": incomplete,
    })
    print(f"PNG audit: {len(complete)} complete, {len(incomplete)} absent or invalid")
    for item in incomplete:
        print(f"{item['status']}: {item['rawPath']} ({item['sizeBytes']} bytes)")


def resource_path(pack: str, key: str) -> Path:
    return DRAWABLE / f"ti_{pack}_{key}.webp"


def fit_square_dimensions(width: int, height: int, target: int):
    scale = target / max(width, height)
    fitted = (round(width * scale), round(height * scale))
    left, top = (target - fitted[0]) // 2, (target - fitted[1]) // 2
    return fitted, (left, top, target - fitted[0] - left, target - fitted[1] - top)


def fit_square_with_edge_extension(image, target, resampling):
    """Preserve proportion and the full image; extend only sub-1% edge gaps."""
    fitted, (left, top, right, bottom) = fit_square_dimensions(*image.size, target)
    scaled = image.resize(fitted, resampling)
    if fitted == (target, target):
        return scaled
    square = Image.new("RGB", (target, target), scaled.getpixel((0, 0)))
    square.paste(scaled, (left, top))
    # Copy the actual opaque border color rather than adding a new visible frame.
    if left:
        square.paste(scaled.crop((0, 0, 1, fitted[1])).resize((left, fitted[1]), Image.Resampling.NEAREST), (0, top))
    if right:
        square.paste(scaled.crop((fitted[0] - 1, 0, fitted[0], fitted[1])).resize((right, fitted[1]), Image.Resampling.NEAREST), (left + fitted[0], top))
    if top:
        square.paste(square.crop((0, top, target, top + 1)).resize((target, top), Image.Resampling.NEAREST), (0, 0))
    if bottom:
        square.paste(square.crop((0, top + fitted[1] - 1, target, top + fitted[1])).resize((target, bottom), Image.Resampling.NEAREST), (0, top + fitted[1]))
    return square


def normalize(args) -> None:
    count = 0
    for pack, key in selected_pairs(args):
        source = RAW / f"{pack}-{key}.png"
        if not valid_raw_image(source):
            if source.is_file():
                print(f"Skipped incomplete/invalid PNG: {relative(source)}")
            continue
        output = resource_path(pack, key)
        with Image.open(source) as original:
            original.load()
            if min(original.size) < 256 or max(original.size) / min(original.size) > 1.01:
                raise ValueError(f"Expected one square icon of at least 256px, within 1% aspect tolerance; got {original.size}: {source}")
            rgba = original.convert("RGBA")
            if rgba.getchannel("A").getextrema() != (255, 255):
                raise ValueError(f"Icon must have an opaque tile, including corners: {source}")
            rgb = original.convert("RGB")
            if pack == "y2k":
                # Keep every image's own silhouette and colors, explicitly rasterized.
                image = fit_square_with_edge_extension(rgb, 32, Image.Resampling.NEAREST).resize((256, 256), Image.Resampling.NEAREST)
            else:
                image = fit_square_with_edge_extension(rgb, 256, Image.Resampling.LANCZOS)
            output.parent.mkdir(parents=True, exist_ok=True)
            image.save(output, "WEBP", lossless=True, method=6, exact=True)
        print(f"Normalized {relative(output)}")
        count += 1
    print(f"Normalized {count} existing generated assets; absent assets were not invented")
    update_manifest(args)


def load_font(size: int, bold=False):
    font_name = "segoeuib.ttf" if bold else "segoeui.ttf"
    candidates = (Path(os.environ.get("WINDIR", "C:/Windows")) / "Fonts" / font_name,
                  Path("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"))
    for candidate in candidates:
        try:
            return ImageFont.truetype(str(candidate), size)
        except OSError:
            pass
    return ImageFont.load_default()


def make_board(args) -> None:
    pairs = selected_pairs(args)
    column_count = len(TRIAL_KEYS) if args.trial else 6
    tile_size, margin, label_height, header = 144, 24, 34, 80
    rows_per_pack = math.ceil((len(TRIAL_KEYS) if args.trial else len(KEYS)) / column_count)
    selected_packs = [pack for pack in PACKS if any(p == pack for p, _ in pairs)]
    width = margin * 2 + column_count * (tile_size + margin)
    height = header + len(selected_packs) * (rows_per_pack * (tile_size + label_height + margin) + 58) + margin
    board = Image.new("RGB", (width, height), "#f0edf3")
    draw = ImageDraw.Draw(board)
    draw.text((margin, 18), f"AILUA UIR-3 | {str(len(pairs)) + ' trial icons' if args.trial else str(len(pairs)) + ' core icons'} | {args.source}", fill="#202436", font=load_font(25, True))
    draw.text((margin, 50), "One independently generated image per icon. Empty tiles indicate missing assets.", fill="#606477", font=load_font(16))
    y = header
    existing = []
    for pack in selected_packs:
        draw.text((margin, y), pack.upper(), fill="#202436", font=load_font(22, True))
        y += 38
        keys = [key for p, key in pairs if p == pack]
        for idx, key in enumerate(keys):
            x = margin + idx % column_count * (tile_size + margin)
            ty = y + idx // column_count * (tile_size + label_height + margin)
            path = RAW / f"{pack}-{key}.png" if args.source == "raw" else resource_path(pack, key)
            draw.rectangle((x, ty, x + tile_size - 1, ty + tile_size - 1), fill="#dbd7e1")
            if path.is_file() and (args.source != "raw" or valid_raw_image(path)):
                with Image.open(path) as source:
                    image = source.convert("RGB").resize((tile_size, tile_size), Image.Resampling.NEAREST if pack == "y2k" else Image.Resampling.LANCZOS)
                board.paste(image, (x, ty))
                existing.append({"packId": pack, "iconKey": key, "path": relative(path), "sha256": sha256(path)})
            else:
                draw.text((x + 14, ty + 57), "MISSING", fill="#777083", font=load_font(18, True))
            if not args.no_labels:
                draw.text((x, ty + tile_size + 7), key, fill="#36384d", font=load_font(17))
        y += rows_per_pack * (tile_size + label_height + margin) + 20
    pack_suffix = "-" + "-".join(selected_packs) if tuple(selected_packs) != PACKS else ""
    stem = f"icons-{'trial' if args.trial else 'all'}-{args.source}{pack_suffix}{'-no-labels' if args.no_labels else ''}"
    output = WORK / "review" / f"{stem}.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    board.save(output, "PNG")
    write_json(output.with_suffix(".json"), {"board": relative(output), "createdAtUtc": now(), "assets": existing})
    print(f"Board: {relative(output)}; {len(existing)}/{len(pairs)} assets exist")
    if args.source == "normalized":
        make_size_board(args, pairs)


def make_size_board(args, pairs) -> None:
    """Native pixel displays supplement the large inspection board.

    These are artwork readability samples, not substituted device screenshots.
    Runtime Android dp values must still be checked on actual device captures.
    """
    sizes, gap, pack_header, top = (56, 44, 24), 16, 40, 92
    cell_width, cell_height = 104, 174
    columns = len(TRIAL_KEYS) if args.trial else 6
    selected_packs = [pack for pack in PACKS if any(p == pack for p, _ in pairs)]
    rows_per_pack = math.ceil((len(TRIAL_KEYS) if args.trial else len(KEYS)) / columns)
    width = gap * 2 + columns * cell_width
    height = top + len(selected_packs) * (pack_header + rows_per_pack * cell_height + gap)
    board = Image.new("RGB", (width, height), "#f0edf3")
    draw = ImageDraw.Draw(board)
    draw.text((gap, 14), "AILUA UIR-3 | small artwork readability", fill="#202436", font=load_font(22, True))
    draw.text((gap, 46), "Native 56 / 44 / 24 pixels, top to bottom. Device dp remains separate.", fill="#606477", font=load_font(14))
    y = top
    for pack in selected_packs:
        draw.text((gap, y), pack.upper(), fill="#202436", font=load_font(20, True))
        y += pack_header
        keys = [key for p, key in pairs if p == pack]
        for index, key in enumerate(keys):
            x = gap + index % columns * cell_width
            ty = y + index // columns * cell_height
            path = resource_path(pack, key)
            if path.is_file():
                with Image.open(path) as source:
                    source_rgb = source.convert("RGB")
                offset = 0
                for size in sizes:
                    image = source_rgb.resize((size, size), Image.Resampling.NEAREST if pack == "y2k" else Image.Resampling.LANCZOS)
                    board.paste(image, (x + (cell_width - size) // 2, ty + offset))
                    offset += size + 9
            if not args.no_labels:
                draw.text((x + 3, ty + 153), key, fill="#36384d", font=load_font(13))
        y += rows_per_pack * cell_height + gap
    pack_suffix = "-" + "-".join(selected_packs) if tuple(selected_packs) != PACKS else ""
    output = WORK / "review" / f"icons-{'trial' if args.trial else 'all'}-small-sizes{pack_suffix}{'-no-labels' if args.no_labels else ''}.png"
    board.save(output, "PNG")
    print(f"Small artwork board: {relative(output)}")


def now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def update_manifest(args) -> None:
    old = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.is_file() else {}
    reviews = old.get("visualReviews", {})
    if getattr(args, "reviewed_board", None):
        board_path = args.reviewed_board.resolve()
        if not board_path.is_relative_to(ROOT):
            raise ValueError("Review board must be saved inside this repository")
        board_metadata = json.loads(board_path.with_suffix(".json").read_text(encoding="utf-8"))
        if board_metadata.get("board") != relative(board_path) or not board_path.is_file():
            raise ValueError("Review board and its matching metadata are required")
        for item in board_metadata["assets"]:
            path = ROOT / item["path"]
            if path != resource_path(item["packId"], item["iconKey"]) or not path.is_file():
                continue
            if sha256(path) != item["sha256"]:
                raise ValueError(f"Asset changed after board creation: {path}")
            reviews[f"{item['packId']}/{item['iconKey']}"] = {
                "sha256": item["sha256"], "board": relative(board_path),
                "reviewedAtUtc": now(), "reviewedBy": args.reviewer,
                "method": "Human or agent inspected actual review-board image; approval explicitly recorded by CLI invocation",
            }
    assets = []
    planned = []
    for pack in PACKS:
        for key in KEYS:
            raw, output = RAW / f"{pack}-{key}.png", resource_path(pack, key)
            common = {
                "packId": pack, "iconKey": key, "resourceName": output.stem,
                "path": relative(output), "rawPath": relative(raw),
                "requestedModel": MODEL, "serviceBaseUrl": SERVICE,
                "requestedSizePx": [1024, 1024], "requestedQuality": "medium",
                "promptFile": relative(VERSIONED_PROMPTS),
                "promptJobOutput": f"{pack}-{key}.png",
                "promptSha256": hashlib.sha256(icon_prompt(pack, key).encode("utf-8")).hexdigest(),
                "source": "Original independent AI-generated AILUA icon via user-designated API and imagegen skill bundled CLI; requested model gpt-image-2",
                "licenseStatus": "pending-rights-review",
                "containsCharacterPortrait": False, "containsUiText": False,
                "thirdPartyAssetIncluded": False,
                "normalization": "proportional nearest-neighbor 32px pixel raster -> 256px, edge extension only when needed, lossless RGB WebP" if pack == "y2k" else "proportional Lanczos -> 256px, edge extension only when needed, lossless RGB WebP",
            }
            if not output.is_file():
                raw_valid = valid_raw_image(raw)
                common["status"] = "raw-generated-pending-normalization" if raw_valid else "invalid-raw-pending-generation" if raw.is_file() else "planned-pending-generation"
                if raw.is_file():
                    common["rawSizeBytes"] = raw.stat().st_size
                    common["rawImageValid"] = raw_valid
                planned.append(common)
                continue
            with Image.open(output) as image:
                width, height = image.size
                opaque = image.mode == "RGB" or image.convert("RGBA").getchannel("A").getextrema() == (255, 255)
            digest = sha256(output)
            reviewed = reviews.get(f"{pack}/{key}", {}).get("sha256") == digest
            if (width, height) != (256, 256) or not opaque or not valid_raw_image(raw):
                raise ValueError(f"Normalized icon is not a sourced, opaque 256x256 asset: {output}")
            with Image.open(raw) as image:
                source_width, source_height = image.size
            common.update({
                "status": "visually-reviewed-production-candidate" if reviewed else "generated-pending-visual-review",
                "sha256": digest, "widthPx": width, "heightPx": height,
                "sizeBytes": output.stat().st_size, "encoding": "lossless WebP RGB", "opaque": opaque,
                "rawSha256": sha256(raw), "rawWidthPx": source_width, "rawHeightPx": source_height,
                "normalizationPaddingPx": list(fit_square_dimensions(source_width, source_height, 32 if pack == "y2k" else 256)[1]),
                "visuallyReviewed": reviewed,
            })
            assets.append(common)
    document = {
        "manifestVersion": 1, "project": "AILUA", "phase": "UIR-3 Dock and icon production",
        "frozenHomeBaseline": "161fa13", "updatedAtUtc": now(),
        "generationScript": "imagegen skill bundled scripts/image_gen.py generate-batch",
        "preparationScript": relative(Path(__file__)), "serviceBaseUrl": SERVICE,
        "requestedModel": MODEL, "generativeApiUsed": any(valid_raw_image(RAW / f"{p}-{k}.png") for p in PACKS for k in KEYS),
        "formalCharacterArtStarted": False, "thirdPartyAssetsUsed": False,
        "authorshipAndUsage": "Original requested artwork for AILUA. No downloaded art or commercial icon-pack source is included. Service contractual usage rights are not independently established by this manifest.",
        "expectedPackIds": list(PACKS), "expectedIconKeys": list(KEYS),
        "expectedCount": len(PACKS) * len(KEYS), "normalizedCount": len(assets),
        "productionCandidateCount": sum(asset["status"] == "visually-reviewed-production-candidate" for asset in assets),
        "licenseStatus": "pending-rights-review; visual approval does not establish service commercial usage terms",
        "coverage": {pack: {"expected": len(KEYS), "normalized": sum(a["packId"] == pack for a in assets),
                            "visuallyReviewedCandidates": sum(a["packId"] == pack and a["status"] == "visually-reviewed-production-candidate" for a in assets)} for pack in PACKS},
        "assets": assets, "pendingAssets": planned, "visualReviews": reviews,
    }
    write_json(MANIFEST, document)
    print(f"Manifest: {len(assets)} normalized, {document['productionCandidateCount']} visually reviewed candidates, {len(planned)} pending")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("prompts", help="Write all/trial/remaining and per-pack JSONL jobs without making API calls")
    for command in ("audit", "normalize", "board", "manifest"):
        command_parser = sub.add_parser(command)
        command_parser.add_argument("--pack", nargs="+", choices=PACKS)
        if command in ("normalize", "board"):
            command_parser.add_argument("--trial", action="store_true")
        if command == "board":
            command_parser.add_argument("--source", choices=("raw", "normalized"), default="normalized")
            command_parser.add_argument("--no-labels", action="store_true")
        if command == "manifest":
            command_parser.add_argument("--reviewed-board", type=Path, help="Record explicit visual approval after inspecting this saved normalized board")
            command_parser.add_argument("--reviewer", default="Codex visual review")
    args = parser.parse_args()
    try:
        {"prompts": prepare_prompts, "audit": audit_raw, "normalize": normalize, "board": make_board, "manifest": update_manifest}[args.command](args)
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, f"Icon preparation failed: {error}\n")


if __name__ == "__main__":
    main()
