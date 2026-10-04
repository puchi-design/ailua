"""Prepare UIR-3D/4 artwork without making any network/API calls.

Generation is performed separately with the imagegen skill bundled CLI. This
script writes prompts, checks valid PNGs, normalizes supplied originals, makes
artwork review boards, and maintains source/hash metadata. Production denotes
visual integration; it is never a claim that provider terms were verified.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
from collections import Counter, deque
from datetime import datetime, timezone
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / "output/imagegen/ui-product"
RAW = WORK / "raw"
OIL_RAW = WORK / "oil-candidates"
OIL_PROMPTS = ROOT / "docs/design/UI_OIL_WALLPAPER_PROMPTS.jsonl"
OLD_RAW = ROOT / "output/imagegen/uir3/raw"
DRAWABLE = ROOT / "app/src/main/res/drawable-nodpi"
PROMPTS = ROOT / "docs/design/UI_PRODUCTIZATION_PROMPTS.jsonl"
MANIFEST = ROOT / "docs/design/THEME_ASSET_MANIFEST.json"
REVIEW = ROOT / "docs/design/ui-product-assets"
SERVICE = "https://www.vibework.live/v1"
MODEL = "gpt-image-2"
LICENSE = "unverified-provider-output-terms"
PACKS = ("default", "soft", "rain", "sakura", "y2k", "midnight")
GENERATED_PACKS = tuple(p for p in PACKS if p != "y2k")
GENERATED_WALLPAPER_PACKS = tuple(p for p in PACKS if p != "default")
THEMES = {
    "default": "default", "soft": "soft_home", "rain": "rainy_study",
    "sakura": "sakura_diary", "y2k": "y2k_love", "midnight": "midnight_glass",
}
KEYS = (
    "chat", "living", "moments", "gallery", "contacts", "mailbox", "call",
    "memories", "relations", "diary", "check_phone", "theater", "world",
    "lore", "creator", "assistant", "apps", "settings",
)
SUBJECTS = {
    "chat": "one large conversation bubble with a short tail and three strong dots",
    "living": "one large warm tea cup with a clearly open handle and two short steam strokes, a quiet daily-life symbol; no house or building",
    "moments": "one large six-petal flower with a strong contrasting round center",
    "gallery": "two offset photo prints, the front with one broad mountain and one sun; clear wide margins",
    "contacts": "one address book with three large side index tabs and one simple abstract contact silhouette on its cover",
    "mailbox": "one large sealed envelope with a clear V-shaped fold and a small round wax seal",
    "call": "one diagonal telephone receiver with two broad ends and a clear open curve",
    "memories": "one keepsake box with its lid slightly ajar and a single broad photograph edge showing",
    "relations": "two boldly interlocking rounded links with clearly open centers",
    "diary": "one closed diary with a broad contrasting spine and one short ribbon bookmark, blank cover",
    "check_phone": "one upright mobile phone with a plain dark rectangular screen and one small notification dot; the phone is the actual subject, not a framing mockup",
    "theater": "one pair of broad stage curtains parted around a large contrasting play triangle",
    "world": "one globe with broad simple continent shapes and a compact stand, no tiny geographic detail",
    "lore": "one open storybook with thick blank pages and a single broad leaf-shaped bookmark in the center",
    "creator": "one bold paintbrush crossing a small four-point creative sparkle",
    "assistant": "one custom large four-point star with a small contrasting central diamond, no face",
    "apps": "four clearly separated square app tiles in a two-by-two group, no inner tiny symbols",
    "settings": "one bold friendly six-tooth gear with a large clearly open center, simple broad teeth",
}
DEFAULT_COLORS = {
    "chat": "cobalt blue", "living": "muted teal", "moments": "golden yellow",
    "gallery": "coral red", "contacts": "warm apricot", "mailbox": "sky blue",
    "call": "leaf green", "memories": "lavender", "relations": "rose pink",
    "diary": "soft cyan", "check_phone": "violet", "theater": "deep coral",
    "world": "emerald", "lore": "terracotta", "creator": "rich violet",
    "assistant": "blue", "apps": "deep cobalt", "settings": "slate blue",
}
STYLES = {
    "default": (
        "Modern real-phone icon artwork: bold custom forms with light physical depth, broad flat colored surfaces, very restrained compact shading. Polished but approachable, clearly legible, no glossy toy plastic or generic AI 3D collectible render.",
        "A full-bleed opaque {color} tile. Subject mainly ivory/white with a single restrained complementary accent. No inset frame: the launcher supplies the squircle mask.",
        "Unified soft light from upper left; compact contact shadow only, no long cast shadow or rim halo.",
    ),
    "soft": (
        "A deliberately simple flat hand-drawn app symbol, two-dimensional pastel gouache graphic. One clean broad silhouette and at most two simple interior strokes. This is a small phone icon, not an illustrated object collectible. Absolutely no volume, bevel, 3D render, realistic material or decorative detail.",
        "Full-bleed smooth opaque very pale blush/ivory tile. Main symbol in muted rose, powder blue or sage, clear contrast. No paper grain, stains, ornamental frame, yellow patina, mini scene or inset background.",
        "Flat printed color with slight natural hand-drawn contour, no lighting effects, shadow, glossy highlight or cream sculpted depth.",
    ),
    "rain": (
        "A crisp flat two-dimensional phone app glyph made from simple broad navy or white shapes, quiet contemporary graphic design. Use only one bold silhouette and one simple contrasting interior cutout. It must read instantly at 24 pixels, not be a realistic item, enamel collectible or illustrated miniature scene.",
        "Full-bleed opaque muted ocean/slate-blue tile with one white or very pale blue symbol. Exactly two or three solid color areas, no gradient, leather, metal, gold, navy window collage, raised frame or translucent rim.",
        "Completely flat graphic color. Zero cast shadow, bevel, extrusion, glow, specular reflection, paper texture or sculpted lighting. Soft blue-white contrast, no amber.",
    ),
    "sakura": (
        "Sakura Diary: clean hand-drawn scrapbook sticker or postage-paper cutout, one bold object with a narrow irregular cream paper edge. Simple rose or sage ink contour, tiny flat pastel fill, one small tape accent at most. This is paper collage, not watercolor still life.",
        "Full-bleed opaque pale blush stationery paper. Cream, dusty rose, sakura pink, soft sage and a little plum ink; slight paper fibers, no decorative mini scene.",
        "Flat daylight and an extremely light short paper-edge shade. No 3D ceramic highlight, no smooth enamel gloss.",
    ),
    "midnight": (
        "Midnight Glass: refined contemporary graphic icon with solid satin blue/violet broad forms and only slight physical depth. Reduce game-item sculpting and metal by a third: simple material, calm clean geometry, no armor bevels or trophy object.",
        "Full-bleed opaque deep navy/indigo tile with a very restrained violet tone. Main symbol in pale blue or lavender, one subtle cyan edge highlight; the tile and symbol remain fully opaque.",
        "Soft upper-left studio light with a small selective cyan rim, not luminous outlines everywhere. No bloom, chrome, hard metallic facets or neon gaming mood.",
    ),
    "y2k": (
        "Authentic late-1990s desktop pixel artwork designed on a visible 32x32 grid: every coarse square pixel, stepped edge and hard one-pixel bevel remains visible. Simple pastel old-PC program icon, no modern vector smoothing.",
        "Full-bleed opaque baby-blue square background with sharp corners. Pastel pink, lavender, cobalt, cream and deep purple outline; no rounded card or window frame.",
        "Discrete flat colors and one-pixel bevels only, no gradient or antialiasing.",
    ),
}
WALLPAPERS = {
    "sakura": (
        "stylized-concept",
        "A premium cream-paper scrapbook wallpaper: tactile blank journal paper, subtle vertical margin, a small sakura petal cluster, one restrained postage-like blank paper stamp and short piece of washi tape near the lower edge. Flat paper collage rather than a photographed room.",
        "Upper 55 percent nearly empty cream paper with subtle fibers. Sparse paper/tape/petal composition near bottom corners; center stays quiet and usable. No large pseudo-card, no writable form, no fake buttons or lettering.",
        "Ivory cotton paper, pale sakura pink, dusty rose, soft sage, low-contrast paper shadows.",
    ),
    "y2k": (
        "stylized-concept",
        "A nostalgic pastel pixel-desktop wallpaper pattern: large simple checker/grid rhythm, stepped abstract cloud-like forms, a few noninteractive rectangular outline fragments near bottom edges. Authentic coarse 32-bit pixel texture.",
        "Upper half calm broad lavender/cream field. Pixel pattern concentrated in lower half and corners. No full software window, titlebar, window controls, taskbar, fake buttons, icons, symbols intended to be clicked, text or numbers.",
        "Pastel pink, lavender, baby blue and cream, hard visible square pixel steps, very little saturation, no smooth gradients.",
    ),
    "midnight": (
        "stylized-concept",
        "A sophisticated deep-blue abstract glass-light wallpaper: two broad softly curved translucent ribbon forms crossing near lower third, subtle violet and cyan refracted edge light, calm dark background with premium photographic material detail.",
        "Upper 52 percent quiet deep navy with only gentle large-scale light variation. Broad elegant light ribbons gather in lower half and leave space for app icons. No city, building, circuitry, lens interface or gaming object.",
        "Deep navy, indigo, restrained violet and a small cyan edge accent, no white ring, no neon bloom or glitter.",
    ),
}

# Rejected desk/window and photographic replacements stay in local history.
# Production source selection uses the independently prompted oil paintings.
OIL_WALLPAPERS = {
    "rose_garden": "oil-01-rose-garden.png",
    "lake_wildflowers": "oil-02-lake-wildflowers.png",
    "pink_bloom": "oil-03-single-pink-bloom.png",
    "woodland_path": "oil-04-woodland-path.png",
    "waterlilies": "oil-05-waterlilies.png",
    "garden_still_life": "oil-06-garden-still-life.png",
}
SELECTED_OIL_WALLPAPERS = {"soft": "rose_garden", "rain": "lake_wildflowers"}
VERSIONED_ICON_PACKS = {"soft", "rain"}


def icon_output(pack: str, key: str) -> str:
    if pack == "soft" and key == "apps":
        return "soft-v3-apps.png"
    return f"{pack}-v2-{key}.png" if pack in VERSIONED_ICON_PACKS else f"{pack}-{key}.png"


def wallpaper_source(pack: str) -> Path:
    if pack in SELECTED_OIL_WALLPAPERS:
        return OIL_RAW / OIL_WALLPAPERS[SELECTED_OIL_WALLPAPERS[pack]]
    return RAW / f"wallpaper-{pack}.png"


def oil_jobs() -> dict[str, dict]:
    return {job["out"]: job for job in map(json.loads, OIL_PROMPTS.read_text(encoding="utf-8").splitlines())}


def raw_source_for_job(job: dict) -> Path:
    for pack in GENERATED_WALLPAPER_PACKS:
        source = wallpaper_source(pack)
        if source.name == job["out"]:
            return source
    return RAW / job["out"]


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def write_jsonl(path: Path, jobs: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(job, ensure_ascii=False) + "\n" for job in jobs), encoding="utf-8")
    print(f"{relative(path)}: {len(jobs)} separate image jobs")


def icon_prompt(pack: str, key: str) -> str:
    style, background, lighting = STYLES[pack]
    subject = SUBJECTS[key]
    if pack == "y2k" and key == "settings":
        subject = "one chunky old-PC control panel with three large horizontal sliders, one pink square switch and one cobalt dial, designed as a single simple 32x32 pixel settings object; no gear and no legible text"
    if pack == "soft" and key == "apps":
        subject = "exactly four separate large rounded squares arranged as a clean 2 by 2 application grid, equal size and equal spacing, colored dusty rose and muted blue; no flowers, petals, stems, leaves, objects or extra decorations"
    return "\n".join((
        "Use case: stylized-concept",
        f"Asset type: one independent opaque AILUA {pack} launcher app icon, production artwork for 56dp launcher and compact dock.",
        f"Primary request: make the {key} icon only, not an icon sheet or app screenshot.",
        f"Scene/backdrop: {background.format(color=DEFAULT_COLORS[key])}",
        f"Subject: {subject}.",
        f"Style/medium: {style}",
        "Composition/framing: one centered large unmistakable subject, about 70 percent of image width and height, clear silhouette and compact equal optical margins. At most one tiny helper; no busy miniature scene.",
        f"Lighting/mood: {lighting}",
        "Text: none. Book covers, pages, screens and paper surfaces contain no letters, numbers, fake writing or logos.",
        "Constraints: exactly one square app-icon bitmap, image fully opaque to all four corners, no outer border or inset icon frame; no person, character portrait, human hands, brand, watermark, device mockup, transparent circle, white ring or glass halo.",
        "Avoid: small decorative details, complicated background scene, all-over glossy plastic, excessively thick highlights, long shadows, low-contrast washed-out symbols.",
    ))


def wallpaper_prompt(pack: str) -> str:
    if pack in SELECTED_OIL_WALLPAPERS:
        return oil_jobs()[wallpaper_source(pack).name]["prompt"]
    usecase, scene, framing, color = WALLPAPERS[pack]
    return "\n".join((
        f"Use case: {usecase}",
        f"Asset type: original AILUA {THEMES[pack]} portrait phone wallpaper, 1024x2208, for normalization to 1080x2340.",
        f"Primary request: {scene}",
        f"Composition/framing: {framing}",
        f"Lighting/mood/color palette: {color}",
        "Text: absolutely none. Do not add book writing, numerals, labels, watermark or signature.",
        "Constraints: artwork only, full portrait wallpaper canvas, not a phone/device/mockup, no UI overlay, no icons, no statusbar, no artificial app panels, no character portrait, man or woman or human body.",
    ))


def jobs() -> list[dict]:
    result = [{"prompt": icon_prompt(pack, key), "model": MODEL, "n": 1,
               "size": "1024x1024", "quality": "medium", "output_format": "png",
               "background": "opaque", "out": icon_output(pack, key)}
              for pack in GENERATED_PACKS for key in KEYS]
    result.append({"prompt": icon_prompt("y2k", "settings"), "model": MODEL, "n": 1,
                   "size": "1024x1024", "quality": "medium", "output_format": "png",
                   "background": "opaque", "out": "y2k-settings.png"})
    result.extend({"prompt": wallpaper_prompt(pack), "model": MODEL, "n": 1,
                   "size": "1024x2208", "quality": "medium", "output_format": "png",
                   "background": "opaque", "out": wallpaper_source(pack).name}
                  for pack in GENERATED_WALLPAPER_PACKS)
    return result


def prepare_prompts(args) -> None:
    history = OIL_RAW / "prompt-history"
    history.mkdir(parents=True, exist_ok=True)
    for prior in (PROMPTS, *WORK.glob("prompts-*.jsonl")):
        if prior.is_file():
            saved = history / f"{prior.stem}-{sha256(prior)}.jsonl"
            if not saved.is_file():
                saved.write_bytes(prior.read_bytes())
    records = jobs()
    assert len(records) == 96
    write_jsonl(PROMPTS, records)
    write_jsonl(WORK / "prompts-all.jsonl", records)
    for pack in PACKS:
        write_jsonl(WORK / f"prompts-{pack}.jsonl", [j for j in records if j["out"].startswith(pack + "-") or j["out"] == wallpaper_source(pack).name])
    wallpaper_outputs = {wallpaper_source(p).name for p in GENERATED_WALLPAPER_PACKS}
    write_jsonl(WORK / "prompts-wallpapers.jsonl", [j for j in records if j["out"] in wallpaper_outputs])
    RAW.mkdir(parents=True, exist_ok=True)
    write_json(WORK / "generation-request.json", {
        "serviceBaseUrl": SERVICE, "requestedModel": MODEL,
        "generator": "C:/Users/fan/.codex/skills/.system/imagegen/scripts/image_gen.py generate-batch",
        "globalCliFlags": ["--no-augment"], "count": len(records),
        "rawOutputDirectory": relative(RAW), "selectedOilOutputDirectory": relative(OIL_RAW), "quality": "medium",
        "sourceDirectories": {j["out"]: relative(raw_source_for_job(j).parent) for j in records},
        "note": "No API call or credential in this preparation script. Each image has a separate prompt.",
    })


def valid_png(path: Path) -> bool:
    if not path.is_file():
        return False
    try:
        with Image.open(path) as image:
            if image.format != "PNG":
                return False
            image.verify()
        return True
    except (OSError, SyntaxError, ValueError):
        return False


def selected_packs(args) -> tuple[str, ...]:
    return tuple(pack for pack in PACKS if not args.pack or pack in args.pack)


def filtered_jobs(args) -> list[dict]:
    packs = selected_packs(args)
    return [job for job in jobs() if any(job["out"].startswith(pack + "-") or job["out"] == wallpaper_source(pack).name for pack in packs)]


def audit(args) -> None:
    complete, incomplete = [], []
    for job in filtered_jobs(args):
        raw = raw_source_for_job(job)
        entry = {"out": job["out"], "rawPath": relative(raw)}
        if valid_png(raw):
            with Image.open(raw) as image:
                entry.update({"width": image.width, "height": image.height,
                              "opaque": image.convert("RGBA").getchannel("A").getextrema() == (255, 255)})
            entry.update({"sha256": sha256(raw), "sizeBytes": raw.stat().st_size})
            complete.append(entry)
        else:
            entry.update({"status": "invalid" if raw.is_file() else "missing",
                          "sizeBytes": raw.stat().st_size if raw.is_file() else 0})
            incomplete.append(entry)
    missing = {entry["out"] for entry in incomplete}
    write_jsonl(WORK / "prompts-recovery-missing.jsonl", [job for job in filtered_jobs(args) if job["out"] in missing])
    write_json(WORK / "raw-audit.json", {"createdAtUtc": now(), "completeCount": len(complete),
                                        "expectedCount": len(filtered_jobs(args)), "complete": complete, "incomplete": incomplete})
    print(f"Valid PNGs: {len(complete)}; missing/invalid: {len(incomplete)}")
    for entry in incomplete:
        print(f"{entry['status']}: {entry['rawPath']} ({entry['sizeBytes']} bytes)")
    oil_complete, oil_incomplete = [], []
    for key, filename in OIL_WALLPAPERS.items():
        source = OIL_RAW / filename
        entry = {"wallpaperId": f"oil_{key}", "rawPath": relative(source)}
        if valid_png(source):
            with Image.open(source) as image:
                image.load()
                entry.update({"width": image.width, "height": image.height,
                              "opaque": image.convert("RGBA").getchannel("A").getextrema() == (255, 255)})
            entry.update({"sha256": sha256(source), "sizeBytes": source.stat().st_size})
            oil_complete.append(entry)
        else:
            entry["status"] = "invalid" if source.is_file() else "missing"
            oil_incomplete.append(entry)
    write_json(OIL_RAW / "library-source-audit.json", {"createdAtUtc": now(), "expectedCount": 6,
        "completeCount": len(oil_complete), "complete": oil_complete, "incomplete": oil_incomplete})
    print(f"Oil library PNGs: {len(oil_complete)}; missing/invalid: {len(oil_incomplete)}")


def icon_source(pack: str, key: str) -> Path:
    return (OLD_RAW if pack == "y2k" and key != "settings" else RAW) / icon_output(pack, key)


def icon_resource(pack: str, key: str) -> Path:
    return DRAWABLE / f"ti_{pack}_{key}.webp"


def save_normalized_webp(image: Image.Image, output: Path) -> None:
    # Keep temporary files outside Android's resource folder, then atomically
    # replace the final resource. Concurrent read-only builds see a whole file.
    staging = WORK / "normalization-staging" / output.name
    staging.parent.mkdir(parents=True, exist_ok=True)
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(staging, "WEBP", lossless=True, method=6, exact=True)
    staging.replace(output)


def wallpaper_resource(pack: str) -> Path:
    return DRAWABLE / f"wallpaper_{THEMES[pack]}.webp"


def edge_extend_square(rgb: Image.Image, target: int, sampling) -> tuple[Image.Image, dict]:
    if min(rgb.size) < 256 or max(rgb.size) / min(rgb.size) > 1.01:
        raise ValueError(f"One square icon of >=256px and aspect tolerance 1% is required, got {rgb.size}")
    ratio = target / max(rgb.size)
    fitted = (round(rgb.width * ratio), round(rgb.height * ratio))
    offset = ((target - fitted[0]) // 2, (target - fitted[1]) // 2)
    scaled = rgb.resize(fitted, sampling)
    result = Image.new("RGB", (target, target))
    result.paste(scaled, offset)
    # Preserve the complete subject; only replicate the tiny canvas edge gap.
    for x in range(target):
        for y in range(target):
            if offset[0] <= x < offset[0] + fitted[0] and offset[1] <= y < offset[1] + fitted[1]:
                continue
            sx = min(max(x - offset[0], 0), fitted[0] - 1)
            sy = min(max(y - offset[1], 0), fitted[1] - 1)
            result.putpixel((x, y), scaled.getpixel((sx, sy)))
    return result, {"proportionalFittedDimensions": list(fitted), "edgeExtensionOffset": list(offset)}


Y2K_BACKGROUNDS = ("#ffe0ec", "#e7dcfa", "#deedff", "#fff1d6")


def y2k_background_variation(image: Image.Image, key: str) -> tuple[Image.Image, dict]:
    """Change only the edge-connected old background; preserve all icon pixels.

    The original was separately generated in UIR-3. Its 32px subject is retained,
    so varied backgrounds are a recorded raster edit rather than new API work.
    """
    pixels = image.load()
    n = image.width
    edge_points = [(x, 0) for x in range(n)] + [(x, n - 1) for x in range(n)] + [(0, y) for y in range(1, n - 1)] + [(n - 1, y) for y in range(1, n - 1)]
    # Quantization groups inconsequential slight variations in old edge color.
    bins = Counter(tuple(channel // 16 for channel in pixels[x, y]) for x, y in edge_points)
    background_bin = bins.most_common(1)[0][0]
    samples = [pixels[x, y] for x, y in edge_points if tuple(channel // 16 for channel in pixels[x, y]) == background_bin]
    representative = tuple(round(sum(color[c] for color in samples) / len(samples)) for c in range(3))
    def matches(point):
        color = pixels[point]
        return sum((color[c] - representative[c]) ** 2 for c in range(3)) <= 36 ** 2
    queue = deque(point for point in edge_points if matches(point))
    visited = set(queue)
    while queue:
        x, y = queue.popleft()
        for next_point in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            nx, ny = next_point
            if 0 <= nx < n and 0 <= ny < n and next_point not in visited and matches(next_point):
                visited.add(next_point)
                queue.append(next_point)
    if not 0.12 <= len(visited) / (n * n) <= 0.85:
        raise ValueError(f"Unsafe background separation for {key}: {len(visited)} pixels of {n*n}")
    target_hex = Y2K_BACKGROUNDS[KEYS.index(key) % len(Y2K_BACKGROUNDS)]
    target = tuple(int(target_hex[i:i + 2], 16) for i in (1, 3, 5))
    result = image.copy()
    for point in visited:
        result.putpixel(point, target)
    return result, {"editedLayer": "edge-connected background only",
                    "originalBackgroundRepresentativeRgb": list(representative),
                    "replacementBackgroundHex": target_hex, "modifiedPixelCountAt32px": len(visited),
                    "subjectPixelsPreserved": True, "method": "32px edge-connected flood fill; color distance <=36, no antialiasing"}


def normalize_wallpaper(source: Path, output: Path, pixel: bool = False) -> dict:
    with Image.open(source) as original:
        original.load()
        if original.height <= original.width or min(original.size) < 512:
            raise ValueError(f"Expected a sufficiently large portrait wallpaper: {source}")
        if original.convert("RGBA").getchannel("A").getextrema() != (255, 255):
            raise ValueError(f"An opaque wallpaper is required: {source}")
        rgb = original.convert("RGB")
        aspect = 1080 / 2340
        crop_w, crop_h = (rgb.width, rgb.width / aspect) if rgb.width / rgb.height < aspect else (rgb.height * aspect, rgb.height)
        crop_box = [(rgb.width - crop_w) / 2, (rgb.height - crop_h) / 2,
                    (rgb.width + crop_w) / 2, (rgb.height + crop_h) / 2]
        target_size = (72, 156) if pixel else (1080, 2340)
        sampling = Image.Resampling.NEAREST if pixel else Image.Resampling.LANCZOS
        target = ImageOps.fit(rgb, target_size, sampling, centering=(0.5, 0.5))
        if pixel:
            target = target.resize((1080, 2340), Image.Resampling.NEAREST)
        save_normalized_webp(target, output)
        return {"sourcePath": relative(source), "sourceSha256": sha256(source),
                "sourceDimensions": list(original.size), "outputSha256": sha256(output),
                "outputDimensions": [1080, 2340], "proportionalCropBoxInSourcePx": crop_box,
                "method": "proportional centered crop -> nearest72x156 -> integer15x nearest1080x2340" if pixel else "proportional centered crop -> Lanczos1080x2340, lossless RGB WebP",
                "normalizedAtUtc": now()}


def normalize(args) -> None:
    log_path = WORK / "normalization.json"
    normalization = json.loads(log_path.read_text(encoding="utf-8")) if log_path.is_file() else {}
    count = 0
    for pack in selected_packs(args):
        for key in KEYS:
            source = icon_source(pack, key)
            if not valid_png(source):
                print(f"Skipped absent/incomplete source: {relative(source)}")
                continue
            with Image.open(source) as original:
                original.load()
                if original.convert("RGBA").getchannel("A").getextrema() != (255, 255):
                    raise ValueError(f"An opaque icon tile is required: {source}")
                target, fit = edge_extend_square(original.convert("RGB"), 32 if pack == "y2k" else 256,
                                                 Image.Resampling.NEAREST if pack == "y2k" else Image.Resampling.LANCZOS)
                variation = None
                if pack == "y2k":
                    if key != "settings":
                        target, variation = y2k_background_variation(target, key)
                    target = target.resize((256, 256), Image.Resampling.NEAREST)
                output = icon_resource(pack, key)
                save_normalized_webp(target, output)
                normalization[relative(output)] = {"sourcePath": relative(source), "sourceSha256": sha256(source),
                    "sourceDimensions": list(original.size), "outputSha256": sha256(output),
                    "outputDimensions": [256, 256], "fit": fit,
                    "method": "nearest32px -> nearest256px" if pack == "y2k" else "proportional Lanczos -> 256px, lossless RGB WebP",
                    "backgroundVariation": variation, "normalizedAtUtc": now()}
            count += 1
        if pack in GENERATED_WALLPAPER_PACKS:
            source = wallpaper_source(pack)
            if not valid_png(source):
                print(f"Skipped absent/incomplete wallpaper: {relative(source)}")
                continue
            output = wallpaper_resource(pack)
            normalization[relative(output)] = normalize_wallpaper(source, output, pixel=pack == "y2k")
            count += 1
    for key, filename in OIL_WALLPAPERS.items():
        source = OIL_RAW / filename
        if not valid_png(source):
            print(f"Skipped absent/incomplete oil wallpaper: {relative(source)}")
            continue
        output = DRAWABLE / f"wallpaper_oil_{key}.webp"
        normalization[relative(output)] = normalize_wallpaper(source, output)
        count += 1
    write_json(log_path, normalization)
    print(f"Normalized {count} sourced assets. No source was invented for missing images.")


def load_font(size: int, bold=False):
    name = "segoeuib.ttf" if bold else "segoeui.ttf"
    try:
        return ImageFont.truetype(str(Path(os.environ.get("WINDIR", "C:/Windows")) / "Fonts" / name), size)
    except OSError:
        return ImageFont.load_default()


def current_normalized_paths() -> set[str]:
    log = WORK / "normalization.json"
    result = {relative(wallpaper_resource("default"))} if wallpaper_resource("default").is_file() else set()
    if not log.is_file():
        return result
    for path, entry in json.loads(log.read_text(encoding="utf-8")).items():
        resource, source = ROOT / path, ROOT / entry["sourcePath"]
        if resource.is_file() and valid_png(source) and sha256(resource) == entry["outputSha256"] and sha256(source) == entry["sourceSha256"]:
            result.add(path)
    return result


def make_boards(args) -> None:
    REVIEW.mkdir(parents=True, exist_ok=True)
    packs = selected_packs(args)
    current_paths = current_normalized_paths()
    for size in (144, 96, 56):
        gap, text_height, header, columns = 20, 25, 66, 6
        rows = math.ceil(len(KEYS) / columns)
        cell_w, cell_h = max(size, 92) + gap, size + text_height + gap
        width = gap * 2 + columns * cell_w
        height = header + len(packs) * (40 + rows * cell_h) + gap
        board = Image.new("RGB", (width, height), "#f1edf3")
        draw = ImageDraw.Draw(board)
        draw.text((gap, 12), f"AILUA production artwork | {size}px native review", font=load_font(23, True), fill="#25263a")
        draw.text((gap, 40), "Artwork samples only. Launcher dp and Dock must be reviewed on device.", font=load_font(14), fill="#676575")
        reviewed_assets = []
        y = header
        for pack in packs:
            draw.text((gap, y), THEMES[pack], font=load_font(22, True), fill="#25263a")
            y += 40
            for index, key in enumerate(KEYS):
                x = gap + index % columns * cell_w
                ty = y + index // columns * cell_h
                path = icon_source(pack, key) if args.source == "raw" else icon_resource(pack, key)
                draw.rectangle((x, ty, x + size - 1, ty + size - 1), fill="#d8d4df")
                if path.is_file() and (valid_png(path) if args.source == "raw" else relative(path) in current_paths):
                    with Image.open(path) as image:
                        tile = image.convert("RGB").resize((size, size), Image.Resampling.NEAREST if pack == "y2k" else Image.Resampling.LANCZOS)
                    board.paste(tile, (x, ty))
                    reviewed_assets.append({"packId": pack, "assetType": "icon", "iconKey": key,
                                            "path": relative(path), "sha256": sha256(path)})
                else:
                    draw.text((x + 2, ty + size // 2 - 7), "MISSING", font=load_font(12), fill="#737081")
                draw.text((x, ty + size + 3), key, font=load_font(12 if size == 56 else 14), fill="#39364a")
            y += rows * cell_h
        suffix = "-" + "-".join(packs) if packs != PACKS else ""
        output = REVIEW / f"icons-{args.source}-{size}px{suffix}.png"
        board.save(output, "PNG")
        write_json(output.with_suffix(".json"), {"board": relative(output), "createdAtUtc": now(), "assets": reviewed_assets})
        print(f"Board: {relative(output)} ({len(reviewed_assets)} assets)")
    make_compact_board(packs, current_paths)
    make_wallpaper_board(packs, current_paths)
    make_oil_wallpaper_board(current_paths)


def make_compact_board(packs, current_paths) -> None:
    gap, cols, cell_w, cell_h, header = 18, 6, 120, 145, 65
    rows = math.ceil(len(KEYS) / cols)
    board = Image.new("RGB", (gap * 2 + cols * cell_w, header + len(packs) * (38 + rows * cell_h) + gap), "#f1edf3")
    draw = ImageDraw.Draw(board)
    draw.text((gap, 12), "AILUA compact artwork | 48px / 24px", font=load_font(23, True), fill="#25263a")
    draw.text((gap, 41), "No enlargement. Folder thumbnails remain recognizable at the smaller size.", font=load_font(13), fill="#676575")
    reviewed_assets = []
    y = header
    for pack in packs:
        draw.text((gap, y), THEMES[pack], font=load_font(21, True), fill="#25263a")
        y += 38
        for index, key in enumerate(KEYS):
            x = gap + index % cols * cell_w
            ty = y + index // cols * cell_h
            path = icon_resource(pack, key)
            if path.is_file() and relative(path) in current_paths:
                with Image.open(path) as image:
                    image = image.convert("RGB")
                for size, offset in ((48, 0), (24, 63)):
                    board.paste(image.resize((size, size), Image.Resampling.NEAREST if pack == "y2k" else Image.Resampling.LANCZOS), (x + (cell_w - size) // 2, ty + offset))
                reviewed_assets.append({"packId": pack, "assetType": "icon", "iconKey": key,
                                        "path": relative(path), "sha256": sha256(path)})
            draw.text((x + 4, ty + 110), key, font=load_font(13), fill="#39364a")
        y += rows * cell_h
    suffix = "-" + "-".join(packs) if packs != PACKS else ""
    output = REVIEW / f"icons-compact-48px-24px{suffix}.png"
    board.save(output, "PNG")
    write_json(output.with_suffix(".json"), {"board": relative(output), "createdAtUtc": now(),
                                             "reviewedSizesPx": [48, 24], "assets": reviewed_assets})
    print(f"Board: {relative(output)}")


def make_oil_wallpaper_board(current_paths) -> None:
    width, height, gap, header, footer, cols = 360, 780, 24, 70, 46, 3
    board = Image.new("RGB", (gap + cols * (width + gap), header + 2 * (height + footer + gap)), "#f1edf3")
    draw = ImageDraw.Draw(board)
    draw.text((gap, 15), "AILUA oil wallpaper library | normalized artwork", font=load_font(24, True), fill="#25263a")
    draw.text((gap, 43), "1080x2340 lossless RGB WebP; proportionally centered crop, not stretched.", font=load_font(16), fill="#676575")
    reviewed_assets = []
    for index, key in enumerate(OIL_WALLPAPERS):
        path = DRAWABLE / f"wallpaper_oil_{key}.webp"
        if relative(path) not in current_paths:
            raise ValueError(f"Missing or stale normalized oil wallpaper: {path}")
        x = gap + index % cols * (width + gap)
        y = header + index // cols * (height + footer + gap)
        with Image.open(path) as image:
            board.paste(image.convert("RGB").resize((width, height), Image.Resampling.LANCZOS), (x, y))
        draw.text((x, y + height + 8), f"oil_{key}", font=load_font(18), fill="#39364a")
        reviewed_assets.append({"packId": "oil-library", "assetType": "wallpaper", "wallpaperId": f"oil_{key}",
                                "path": relative(path), "sha256": sha256(path), "reviewSizePx": [width, height]})
    output = REVIEW / "wallpapers-oil-library.png"
    board.save(output, "PNG")
    write_json(output.with_suffix(".json"), {"board": relative(output), "createdAtUtc": now(), "assets": reviewed_assets})
    print(f"Board: {relative(output)} ({len(reviewed_assets)} assets)")


def make_wallpaper_board(packs, current_paths) -> None:
    width, height, gap, header = 216, 468, 24, 70
    board = Image.new("RGB", (gap + len(packs) * (width + gap), header + height + 48), "#f1edf3")
    draw = ImageDraw.Draw(board)
    draw.text((gap, 15), "AILUA six-theme wallpaper materials | static artwork", font=load_font(24, True), fill="#25263a")
    draw.text((gap, 43), "Upper region stays quiet for existing widgets. These are wallpaper assets, not launcher screenshots.", font=load_font(14), fill="#676575")
    reviewed_assets = []
    for index, pack in enumerate(packs):
        path = wallpaper_resource(pack)
        x = gap + index * (width + gap)
        if path.is_file() and relative(path) in current_paths:
            with Image.open(path) as image:
                board.paste(image.convert("RGB").resize((width, height), Image.Resampling.NEAREST if pack == "y2k" else Image.Resampling.LANCZOS), (x, header))
            reviewed_assets.append({"packId": pack, "assetType": "wallpaper", "path": relative(path), "sha256": sha256(path)})
        draw.text((x, header + height + 8), THEMES[pack], font=load_font(16), fill="#39364a")
    suffix = "-" + "-".join(packs) if packs != PACKS else ""
    output = REVIEW / f"wallpapers-six-materials{suffix}.png"
    board.save(output, "PNG")
    write_json(output.with_suffix(".json"), {"board": relative(output), "createdAtUtc": now(), "assets": reviewed_assets})
    print(f"Board: {relative(output)}")


def manifest(args) -> None:
    old = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.is_file() else {}
    reviews = old.get("visualReviews", {})
    normalization_path = WORK / "normalization.json"
    normalization = json.loads(normalization_path.read_text(encoding="utf-8")) if normalization_path.is_file() else {}
    for board_path in args.reviewed_board or []:
        board_path = board_path.resolve()
        if not board_path.is_relative_to(ROOT) or not board_path.is_file():
            raise ValueError("An existing saved board inside the repository is required")
        metadata = json.loads(board_path.with_suffix(".json").read_text(encoding="utf-8"))
        if metadata["board"] != relative(board_path):
            raise ValueError(f"Review metadata does not match board: {board_path}")
        for entry in metadata["assets"]:
            resource = ROOT / entry["path"]
            if resource.parent != DRAWABLE:
                continue
            if sha256(resource) != entry["sha256"]:
                raise ValueError(f"Reviewed asset changed since board was made: {resource}")
            reviews[entry["path"]] = {"sha256": entry["sha256"], "board": relative(board_path),
                                      "reviewedAtUtc": now(), "reviewedBy": args.reviewer,
                                      "method": "Actual saved artwork board opened and inspected; review recorded after inspection"}
    assets, pending = [], []
    old_y2k_prompts = ROOT / "docs/design/UIR3_ICON_PROMPTS.jsonl"
    old_jobs = {job["out"]: job for job in map(json.loads, old_y2k_prompts.read_text(encoding="utf-8").splitlines())} if old_y2k_prompts.is_file() else {}
    source_groups = []
    for pack in PACKS:
        sources = [("icon", key, icon_source(pack, key), icon_resource(pack, key)) for key in KEYS]
        sources.append(("wallpaper", None, None if pack == "default" else wallpaper_source(pack), wallpaper_resource(pack)))
        source_groups.append((pack, THEMES[pack], sources))
    source_groups.append(("oil-library", "wallpaper-library", [
        ("wallpaper", key, OIL_RAW / filename, DRAWABLE / f"wallpaper_oil_{key}.webp")
        for key, filename in OIL_WALLPAPERS.items()
    ]))
    actual_oil_jobs = oil_jobs()
    for pack, theme_id, sources in source_groups:
        for asset_type, key, raw, output in sources:
            source_exists = pack == "default" and asset_type == "wallpaper" or raw is not None and valid_png(raw)
            processed = normalization.get(relative(output))
            if pack == "default" and asset_type == "wallpaper":
                source = "Original project procedural geometry from scripts/render_theme_wallpapers.py render_default(); existing approved geometric wallpaper retained"
                license_status, license_label = "project-authored-original", "Original project artwork; no downloaded or provider-generated component"
            else:
                source = "Original individually prompted AI-generated AILUA artwork via user-designated API and imagegen skill bundled CLI"
                license_status, license_label = LICENSE, "Provider output terms are recorded as unverified; not a UI development prerequisite"
            entry = {"themeId": theme_id, "packId": pack, "assetType": asset_type,
                     "iconKey": key if asset_type == "icon" else None, "path": relative(output), "resourceName": output.stem,
                     "source": source, "license": license_label, "licenseStatus": license_status,
                     "containsCharacterPortrait": False, "containsUiText": False, "thirdPartyAssetIncluded": False}
            if raw is not None:
                reuse = pack == "y2k" and key != "settings" and asset_type == "icon"
                is_oil = raw.parent == OIL_RAW
                prompt = actual_oil_jobs[raw.name]["prompt"] if is_oil else old_jobs.get(f"y2k-{key}.png", {}).get("prompt", "") if reuse else icon_prompt(pack, key) if asset_type == "icon" else wallpaper_prompt(pack)
                entry.update({"rawPath": relative(raw), "serviceBaseUrl": SERVICE, "requestedModel": MODEL,
                              "requestedQuality": "medium", "requestedDimensions": [1024, 1024] if asset_type == "icon" else [1024, 2208],
                              "promptFile": relative(OIL_PROMPTS if is_oil else old_y2k_prompts if reuse else PROMPTS),
                              "promptSha256": hashlib.sha256(prompt.encode("utf-8")).hexdigest(),
                              "reusedPriorIndependentGeneration": reuse})
                if is_oil:
                    oil_key = key if pack == "oil-library" else SELECTED_OIL_WALLPAPERS[pack]
                    entry["wallpaperLibraryId"] = f"oil_{oil_key}"
            # Never treat older resource leftovers as completed new source work.
            normalized = output.is_file() and source_exists and (processed is not None or raw is None)
            if normalized and processed is not None and processed["outputSha256"] != sha256(output):
                normalized = False
            if normalized and raw is not None and (processed["sourcePath"] != relative(raw) or processed["sourceSha256"] != sha256(raw)):
                normalized = False
            if not normalized:
                entry["status"] = "raw-generated-pending-normalization" if source_exists else "planned-pending-generation"
                pending.append(entry)
                continue
            with Image.open(output) as image:
                expected = (256, 256) if asset_type == "icon" else (1080, 2340)
                if image.size != expected or image.convert("RGBA").getchannel("A").getextrema() != (255, 255):
                    raise ValueError(f"Unexpected normalized size or transparency: {output}")
                entry.update({"width": image.width, "height": image.height, "widthPx": image.width, "heightPx": image.height})
            digest = sha256(output)
            reviewed = reviews.get(relative(output), {}).get("sha256") == digest
            entry.update({"sha256": digest, "sizeBytes": output.stat().st_size,
                          "encoding": "lossless WebP RGB", "opaque": True,
                          "status": "production" if reviewed else "generated-pending-visual-review",
                          "visuallyReviewed": reviewed, "normalization": processed})
            if raw is not None:
                with Image.open(raw) as image:
                    entry.update({"rawWidth": image.width, "rawHeight": image.height})
                entry["rawSha256"] = sha256(raw)
            assets.append(entry)
    successful_new_raw = [path for path in (*RAW.glob("*.png"), *OIL_RAW.glob("oil-*.png")) if valid_png(path)]
    write_json(MANIFEST, {
        "manifestVersion": 3, "project": "AILUA", "phase": "P5.V UIR-3D/4 six production themes and six oil wallpapers",
        "baselineHead": "f6e0d2b1adf147eb1700776b56ae3ef05fa423bb",
        "uiCodeBaselineHead": "9ee6887eb77fd3b2f709c6e60eb7e16a72461e99", "updatedAtUtc": now(),
        "generationScript": "imagegen skill bundled scripts/image_gen.py generate-batch",
        "preparationScript": relative(Path(__file__)), "serviceBaseUrl": SERVICE, "requestedModel": MODEL,
        "generativeApiUsed": any(valid_png(raw_source_for_job(job)) for job in jobs()),
        "thirdPartyAssetsUsed": False, "downloadedAssetsUsed": False, "formalCharacterArtStarted": False,
        "expectedThemeIds": list(THEMES.values()), "expectedIconKeys": list(KEYS),
        "expectedIconCount": 108, "expectedThemeWallpaperCount": 6, "expectedExtraWallpaperCount": 6,
        "expectedWallpaperCount": 12,
        "normalizedCount": len(assets), "productionCount": sum(a["status"] == "production" for a in assets),
        "pendingCount": len(pending),
        "visualReviewPendingCount": sum(a["status"] == "generated-pending-visual-review" for a in assets), "licenseStatus": LICENSE,
        "generationHistory": {"successfulNewRawPngCount": len(successful_new_raw),
            "currentUniqueNewRawSources": len({a["rawPath"] for a in assets if a.get("rawPath") and (ROOT / a["rawPath"]).parent in (RAW, OIL_RAW)}),
            "oldRawFilesRetained": True, "availablePromptHistoryRetained": True,
            "localPromptHistoryDirectory": relative(OIL_RAW / "prompt-history"),
            "historicalPromptBodyGaps": ["Rejected initial desk wallpaper", "Rejected initial window wallpaper"],
            "historicalPromptBodyGapNote": "Their original prompt bodies were not present in retained JSONL when this correction began; recorded historical hashes are preserved locally. They are not production sources. No missing prompt body was reconstructed or claimed complete.",
            "countMeaning": "Actual valid new PNG files retained locally, including rejected replacements; not the number of API attempts or currently shipped images."},
        "productionStatusMeaning": "Visual artwork integration/review status only. Provider output commercial rights have not been independently verified; user removed that development gate.",
        "coverage": {THEMES[p]: {"icons": sum(a["packId"] == p and a["assetType"] == "icon" for a in assets),
                                 "wallpapers": sum(a["packId"] == p and a["assetType"] == "wallpaper" for a in assets),
                                 "production": sum(a["packId"] == p and a["status"] == "production" for a in assets)} for p in PACKS},
        "extraWallpaperAssets": [a for a in assets if a["themeId"] == "wallpaper-library"],
        "assets": assets, "pendingAssets": pending, "visualReviews": reviews,
    })
    print(f"Manifest: {len(assets)} normalized, {sum(a['status'] == 'production' for a in assets)} production, {len(pending)} pending")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("prompts")
    for command in ("audit", "normalize", "board", "manifest"):
        p = sub.add_parser(command)
        p.add_argument("--pack", nargs="+", choices=PACKS)
        if command == "board":
            p.add_argument("--source", choices=("raw", "normalized"), default="normalized")
        if command == "manifest":
            p.add_argument("--reviewed-board", nargs="+", type=Path)
            p.add_argument("--reviewer", default="Codex actual-image visual review")
    args = parser.parse_args()
    try:
        {"prompts": prepare_prompts, "audit": audit, "normalize": normalize,
         "board": make_boards, "manifest": manifest}[args.command](args)
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, f"Asset preparation failed: {error}\n")


if __name__ == "__main__":
    main()
