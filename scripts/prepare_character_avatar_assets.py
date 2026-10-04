"""Audit and prepare supplied six-character bitmap avatars; never calls an API.

Actual generation/editing belongs to the imagegen skill's bundled CLI. Source
images remain intact. Review boards only resize/circle-clip supplied artwork;
they never draw replacement faces or automatically approve visual quality.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps, ImageStat


ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / "output/imagegen/six-characters"
RAW = WORK / "raw"
FINAL = WORK / "final"
ASSETS = ROOT / "app/src/main/assets/characters"
MAIN_PROMPTS = ROOT / "docs/design/SIX_CHARACTER_AVATAR_PROMPTS.jsonl"
ALT_PROMPTS = WORK / "alt-prompts.jsonl"
SELECTION = WORK / "selection.json"
NORMALIZATION_RECEIPT = WORK / "normalization.json"
MANIFEST = ROOT / "docs/design/SIX_CHARACTER_AVATAR_ASSET_MANIFEST.json"
REVIEW = ROOT / "docs/design/six-character-assets"
SERVICE = "https://www.vibework.live/v1"
MODEL = "gpt-image-2"
TERMS = "unverified-provider-output-terms"
CHARACTERS = (
    ("hewenchuan", "hewenchuan", "贺闻川"),
    ("zhoujianye", "zhoujianye", "周见野"),
    ("peixubai", "peixubai", "裴叙白"),
    ("suwanning", "mira", "苏晚宁"),
    ("xuchaoyan", "yuna", "许朝颜"),
    ("songzhiwei", "noa", "宋知微"),
)
VARIANTS = ("main", "alt")
RESAMPLE = Image.Resampling.LANCZOS
MATTE = (242, 241, 239)


def relative(path: Path) -> str:
    return path.resolve().relative_to(ROOT).as_posix()


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def read_json(path: Path, default: object) -> object:
    return json.loads(path.read_text(encoding="utf-8-sig")) if path.is_file() else default


def read_jobs(path: Path) -> dict[str, dict]:
    if not path.is_file():
        return {}
    jobs: dict[str, dict] = {}
    for line_number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if not line.strip():
            continue
        job = json.loads(line)
        if not isinstance(job.get("prompt"), str) or not job["prompt"].strip():
            raise ValueError(f"{relative(path)}:{line_number}: missing prompt")
        output = Path(job.get("out", "")).name
        if not output:
            raise ValueError(f"{relative(path)}:{line_number}: missing output filename")
        if output in jobs:
            raise ValueError(f"{relative(path)}:{line_number}: duplicate output filename {output}")
        jobs[output] = {**job, "recordPath": relative(path), "recordFileSha256": sha256(path)}
    return jobs


def selection(args: argparse.Namespace) -> dict:
    chosen = read_json(args.selection, {})
    if not isinstance(chosen, dict):
        raise ValueError("selection must be a JSON object keyed by public asset slug")
    known = {slug for slug, _, _ in CHARACTERS}
    if set(chosen) - known:
        raise ValueError(f"unknown selection character(s): {sorted(set(chosen) - known)}")
    for slug, variants in chosen.items():
        if not isinstance(variants, dict) or set(variants) - set(VARIANTS):
            raise ValueError(f"invalid selected variants for {slug}")
        for variant, filename in variants.items():
            if not isinstance(filename, str) or Path(filename).name != filename or not filename.startswith(slug + "-"):
                raise ValueError(f"{slug}/{variant}: selected source must be that character's raw filename")
            if not filename.endswith(".png"):
                raise ValueError(f"{slug}/{variant}: selected source must be PNG")
    return chosen


def source_for(slug: str, variant: str, chosen: dict) -> Path:
    return RAW / chosen.get(slug, {}).get(variant, f"{slug}-{variant}-v1.png")


def inspect_png(path: Path) -> dict:
    result: dict = {"path": relative(path), "valid": False, "exists": path.is_file()}
    if not path.is_file():
        result["error"] = "source-missing"
        return result
    result["bytes"] = path.stat().st_size
    result["sha256"] = sha256(path)
    try:
        with Image.open(path) as image:
            if image.format != "PNG":
                raise ValueError(f"expected PNG, found {image.format}")
            image.verify()
        with Image.open(path) as image:
            image.load()
            result["actualDimensions"] = list(image.size)
            result["actualMode"] = image.mode
            oriented = ImageOps.exif_transpose(image)
            result["orientedDimensions"] = list(oriented.size)
            if min(oriented.size) < 512:
                raise ValueError("short edge under 512 pixels; source insufficient for avatar")
            rgba = oriented.convert("RGBA")
            result["alphaRange"] = list(rgba.getchannel("A").getextrema())
            result["sourceOpaque"] = result["alphaRange"] == [255, 255]
            probe = rgba.convert("RGB").resize((64, 64), RESAMPLE)
            spread = max(ImageStat.Stat(probe).stddev)
            result["sampleColorStdDev"] = round(spread, 4)
            if spread < 2:
                raise ValueError("near-blank source; file existence alone is insufficient")
            result["valid"] = True
    except Exception as error:
        result["error"] = f"{type(error).__name__}: {error}"
    return result


def normalized_image(source: Path, edge: int) -> Image.Image:
    with Image.open(source) as original:
        oriented = ImageOps.exif_transpose(original)
        rgba = oriented.convert("RGBA")
        matte = Image.new("RGBA", rgba.size, (*MATTE, 255))
        rgb = Image.alpha_composite(matte, rgba).convert("RGB")
        return ImageOps.fit(rgb, (edge, edge), method=RESAMPLE, centering=(0.5, 0.5))


def paths_for(slug: str, variant: str) -> tuple[Path, Path]:
    return ASSETS / slug / f"avatar_{variant}.webp", FINAL / slug / f"avatar_{variant}.png"


def audit(args: argparse.Namespace) -> list[dict]:
    chosen = selection(args)
    records = []
    for slug, storage_id, name in CHARACTERS:
        for variant in VARIANTS:
            records.append({"characterId": slug, "storageCharacterId": storage_id, "name": name, "variant": variant,
                            **inspect_png(source_for(slug, variant, chosen))})
    write_json(WORK / "audit.json", {"checkedAt": utc_now(), "expected": 12,
                                     "valid": sum(row["valid"] for row in records), "sources": records})
    return records


def normalize(args: argparse.Namespace) -> None:
    rows = audit(args)
    receipt = read_json(NORMALIZATION_RECEIPT, {"schemaVersion": 1, "assets": {}})
    missing = []
    for row in rows:
        if row["variant"] not in args.variants:
            continue
        if not row["valid"]:
            missing.append(f"{row['characterId']}/{row['variant']}: {row['error']}")
            continue
        source = ROOT / row["path"]
        webp, backup = paths_for(row["characterId"], row["variant"])
        webp.parent.mkdir(parents=True, exist_ok=True)
        backup.parent.mkdir(parents=True, exist_ok=True)
        normalized_image(source, 512).save(webp, "WEBP", quality=95, method=6, exact=True)
        normalized_image(source, 1024).save(backup, "PNG", optimize=True)
        receipt["assets"][f"{row['characterId']}/{row['variant']}"] = {
            "normalizedAt": utc_now(), "source": row["path"], "sourceSha256": row["sha256"],
            "resource": relative(webp), "resourceSha256": sha256(webp),
            "pngBackup": relative(backup), "pngBackupSha256": sha256(backup),
        }
        print(f"normalized {row['characterId']}/{row['variant']}: source {row['actualDimensions']} → WebP512 / PNG1024")
    write_json(NORMALIZATION_RECEIPT, receipt)
    manifest(args)
    if missing and args.require_complete:
        raise ValueError("incomplete selected source set:\n" + "\n".join(missing))


def prompt_info(job: dict | None) -> dict | None:
    if not job:
        return None
    prompt = job["prompt"]
    reference = None
    if job.get("reference_image"):
        reference_path = (ROOT / job["reference_image"]).resolve()
        reference = {"path": job["reference_image"], "recordedSha256": job.get("reference_sha256")}
        if reference_path.is_relative_to(ROOT) and reference_path.is_file():
            reference["actualSha256"] = sha256(reference_path)
            reference["verified"] = reference["actualSha256"] == reference["recordedSha256"]
        else:
            reference["verified"] = False
    return {"recordPath": job["recordPath"], "recordFileSha256": job["recordFileSha256"],
            "prompt": prompt, "promptSha256": hashlib.sha256(prompt.encode("utf-8")).hexdigest(),
            "model": job.get("model", MODEL), "requestedSize": job.get("size", "1024x1024"),
            "requestedQuality": job.get("quality", "high"), "augmentation": "disabled-cli-no-augment",
            "outputFilename": job["out"], "operation": job.get("operation", "generate"),
            "inputReference": reference}


def inspect_output(path: Path, expected_edge: int) -> dict | None:
    if not path.is_file():
        return None
    with Image.open(path) as image:
        image.load()
        actual_dimensions = list(image.size)
        actual_mode = image.mode
    return {"path": relative(path), "sha256": sha256(path), "bytes": path.stat().st_size,
            "actualDimensions": actual_dimensions, "actualMode": actual_mode,
            "valid": actual_dimensions == [expected_edge, expected_edge] and actual_mode == "RGB"}


def reference_info(job: dict | None, selected_main: Path) -> dict:
    reference = job.get("reference_image") if job else None
    if not isinstance(reference, str):
        return {"valid": False, "error": "alt-reference-image-not-recorded"}
    path = Path(reference)
    if not path.is_absolute():
        path = ROOT / path
    path = path.resolve()
    if not path.is_relative_to(ROOT):
        return {"valid": False, "error": "alt-reference-outside-workspace"}
    info = {"path": relative(path), "exists": path.is_file(), "selectedMainPath": relative(selected_main)}
    if path.is_file():
        info["sha256"] = sha256(path)
    info["matchesSelectedMain"] = path == selected_main.resolve()
    recorded = job.get("reference_sha256")
    if recorded:
        info["recordedSha256"] = recorded
        info["recordedHashMatches"] = recorded == info.get("sha256")
    else:
        info["recordedHashMatches"] = None
    # The hash should be recorded when the edit is requested. Without it this
    # proves the current file only, not what a historical request actually used.
    info["valid"] = bool(info["exists"] and info["matchesSelectedMain"] and info["recordedHashMatches"])
    if not info["valid"]:
        info["error"] = "alt-reference-missing-or-not-bound-to-selected-main-hash"
    return info


def manifest(args: argparse.Namespace) -> dict:
    chosen = selection(args)
    main_jobs = read_jobs(args.main_prompts)
    alt_jobs = read_jobs(args.alt_prompts)
    previous = read_json(MANIFEST, {})
    receipts = read_json(NORMALIZATION_RECEIPT, {"assets": {}})["assets"]
    previous_rows = {(row.get("characterId"), row.get("variant")): row for row in previous.get("assets", [])}
    rows = []
    for slug, storage_id, name in CHARACTERS:
        main_source = source_for(slug, "main", chosen)
        for variant in VARIANTS:
            source = source_for(slug, variant, chosen)
            source_info = inspect_png(source)
            job = (main_jobs if variant == "main" else alt_jobs).get(source.name)
            webp, backup = paths_for(slug, variant)
            output = inspect_output(webp, 512)
            png_backup = inspect_output(backup, 1024)
            reference = reference_info(job, main_source) if variant == "alt" else None
            prompt = prompt_info(job)
            normalization_receipt = receipts.get(f"{slug}/{variant}")
            metadata_errors = []
            if not prompt:
                metadata_errors.append("selected-source-prompt-not-recorded")
            if job and (job.get("characterId") != slug or job.get("storageCharacterId") != storage_id):
                metadata_errors.append("prompt-character-identity-mismatch")
            if job and job.get("operation", "generate").startswith("edit") and not (prompt.get("inputReference") or {}).get("verified"):
                metadata_errors.append("edit-input-reference-hash-mismatch")
            if variant == "alt" and not reference["valid"]:
                metadata_errors.append(reference["error"])
            if (not normalization_receipt or not output or not png_backup or
                    normalization_receipt.get("sourceSha256") != source_info.get("sha256") or
                    normalization_receipt.get("resourceSha256") != output.get("sha256") or
                    normalization_receipt.get("pngBackupSha256") != png_backup.get("sha256")):
                metadata_errors.append("normalization-receipt-missing-or-stale")
            old = previous_rows.get((slug, variant), {})
            review = {"status": "pending", "reason": "Actual original, main/alt identity, circular thumbnails and UI await human visual review"}
            # Approval is a root-authored record tied to exact artwork and
            # instructions. Changing any one invalidates that previous review.
            if (source_info.get("sha256") and output and prompt and
                    old.get("source", {}).get("sha256") == source_info["sha256"] and
                    old.get("resource", {}).get("sha256") == output["sha256"] and
                    old.get("prompt", {}).get("promptSha256") == prompt["promptSha256"] and
                    old.get("referenceMain") == reference):
                review = old.get("visualReview", review)
            ready = source_info["valid"] and output and output["valid"] and png_backup and png_backup["valid"] and not metadata_errors
            status = "production" if ready and review.get("status") == "approved" else "normalized-candidate" if ready else "pending-source" if not source_info["valid"] else "pending-metadata-or-normalization"
            rows.append({"characterId": slug, "storageCharacterId": storage_id, "name": name, "variant": variant,
                         "status": status, "source": source_info, "prompt": prompt, "referenceMain": reference,
                         "resource": output, "pngBackup": png_backup,
                         "normalizationReceipt": normalization_receipt,
                         "normalization": {"orientation": "EXIF transpose before processing", "crop": "centered proportional square crop, centering(0.5,0.5)",
                                           "resample": "Pillow LANCZOS", "resource": "512x512 RGB WebP quality95 method6",
                                           "backup": "1024x1024 RGB PNG", "originalPreserved": True,
                                           "alphaMatteRGB": list(MATTE), "alphaMatteApplied": source_info.get("sourceOpaque") is False},
                         "provenance": {"type": "AI-generated", "service": SERVICE, "model": job.get("model", MODEL) if job else MODEL,
                                        "rightsStatus": TERMS, "rightsVerified": False,
                                        "productionMeaning": "Visual integration status only; provider commercial terms are not certified"},
                         "metadataErrors": metadata_errors, "visualReview": review})
    board_files = sorted(REVIEW.glob("*.json")) if REVIEW.is_dir() else []
    value = {"schemaVersion": 1, "updatedAt": utc_now(), "scope": "six adult characters, main + same-identity expression alternate; no CG/full-body production",
             "assetDirectoryIdentity": "public asset slug; storageCharacterId preserves mira/yuna/noa for existing female saves",
             "summary": {"expected": 12, "validOriginals": sum(row["source"]["valid"] for row in rows),
                         "statusCounts": dict(Counter(row["status"] for row in rows)),
                         "visualReviewCounts": dict(Counter(row["visualReview"]["status"] for row in rows))},
             "generation": {"model": MODEL, "service": SERVICE, "requestedSize": "1024x1024", "requestedQuality": "high", "actualDimensionsRecordedPerSource": True,
                            "networkRunner": "imagegen skill bundled image_gen.py only; this preparation script has no network or SDK calls",
                            "batchNoAugment": "Pass --no-augment globally; the JSONL no_augment field alone is not interpreted by the bundled batch CLI"},
             "reviewBoards": [{"path": relative(path), "sha256": sha256(path)} for path in board_files], "assets": rows}
    write_json(MANIFEST, value)
    print(f"manifest: {value['summary']}")
    return value


def font(size: int) -> ImageFont.ImageFont:
    # Known fonts only, no installation-directory searching.
    for path in (Path("C:/Windows/Fonts/msyh.ttc"), Path("C:/Windows/Fonts/arial.ttf")):
        if path.is_file():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def resource_image(slug: str, variant: str) -> Image.Image | None:
    path, _ = paths_for(slug, variant)
    if not path.is_file():
        return None
    with Image.open(path) as image:
        return image.convert("RGB")


def placeholder(draw: ImageDraw.ImageDraw, rectangle: tuple[int, int, int, int]) -> None:
    draw.rectangle(rectangle, fill=(232, 233, 236))
    draw.text((rectangle[0] + 12, rectangle[1] + 16), "PENDING SOURCE", fill=(90, 96, 107), font=font(17))


def save_board(canvas: Image.Image, name: str, source_rows: list[dict], description: str) -> None:
    REVIEW.mkdir(parents=True, exist_ok=True)
    path = REVIEW / name
    canvas.save(path, "PNG", optimize=True)
    write_json(path.with_suffix(".json"), {"createdAt": utc_now(), "board": relative(path), "sha256": sha256(path),
                                         "dimensions": list(canvas.size), "method": description,
                                         "reviewStatus": "await-root-actual-view", "sources": source_rows})
    print(f"board {relative(path)}")


def source_row(slug: str, storage_id: str, name: str, variant: str) -> dict:
    path, _ = paths_for(slug, variant)
    return {"characterId": slug, "storageCharacterId": storage_id, "name": name, "variant": variant,
            "resource": relative(path), "resourceSha256": sha256(path) if path.is_file() else None}


def board(args: argparse.Namespace) -> None:
    for variant in args.variants:
        tile, gap, margin, label = 320, 22, 24, 62
        width = margin * 2 + tile * 3 + gap * 2
        height = 78 + margin + (tile + label) * 2 + gap
        canvas = Image.new("RGB", (width, height), (249, 247, 244))
        draw = ImageDraw.Draw(canvas)
        draw.text((margin, 18), f"AILUA · 六人{'主头像' if variant == 'main' else '同身份次级头像'}", fill=(32, 39, 47), font=font(27))
        draw.text((margin, 53), "Supplied bitmap artwork · visual review required · labels outside image", fill=(103, 110, 121), font=font(14))
        sources = []
        for index, (slug, storage_id, name) in enumerate(CHARACTERS):
            x = margin + (index % 3) * (tile + gap)
            y = 78 + (index // 3) * (tile + label + gap)
            image = resource_image(slug, variant)
            if image:
                canvas.paste(image.resize((tile, tile), RESAMPLE), (x, y))
            else:
                placeholder(draw, (x, y, x + tile - 1, y + tile - 1))
            draw.text((x, y + tile + 8), name, fill=(36, 42, 51), font=font(23))
            draw.text((x, y + tile + 36), f"{slug} / {storage_id}", fill=(95, 105, 119), font=font(14))
            sources.append(source_row(slug, storage_id, name, variant))
        save_board(canvas, f"six-{variant}-review.png", sources, "Proportional 512→320 LANCZOS thumbnail; original square preserved, no retouching or drawn avatar")

    sizes = (48, 64, 96)
    column, left, top = 164, 32, 80
    row_height = 156
    width = left * 2 + column * 6
    height = top + row_height * len(sizes) * len(args.variants) + 20
    canvas = Image.new("RGB", (width, height), (248, 248, 248))
    draw = ImageDraw.Draw(canvas)
    draw.text((left, 15), "48 / 64 / 96 px · 实际圆裁尺寸", fill=(33, 43, 55), font=font(26))
    draw.text((left, 52), "Full square clipped to circle; faces are not recentered or enlarged", fill=(101, 111, 121), font=font(15))
    sources = []
    for variant_index, variant in enumerate(args.variants):
        for size_index, edge in enumerate(sizes):
            y = top + (variant_index * len(sizes) + size_index) * row_height
            for index, (slug, storage_id, name) in enumerate(CHARACTERS):
                x = left + index * column
                draw.text((x, y), f"{variant} {edge}px", fill=(87, 100, 119), font=font(14))
                image = resource_image(slug, variant)
                if image:
                    reduced = image.resize((edge, edge), RESAMPLE)
                    mask = Image.new("L", (edge, edge), 0)
                    ImageDraw.Draw(mask).ellipse((0, 0, edge - 1, edge - 1), fill=255)
                    canvas.paste(reduced, (x + (column - edge) // 2, y + 24), mask)
                else:
                    draw.text((x + 13, y + 45), "PENDING", fill=(110, 113, 121), font=font(17))
                draw.text((x + 38, y + 126), name, fill=(40, 46, 55), font=font(17))
                if size_index == 0:
                    sources.append(source_row(slug, storage_id, name, variant))
    save_board(canvas, "six-circle-native-review.png", sources, "One LANCZOS resize from formal512 to48/64/96, full-square circle clipping, no image retouching or added face elements; native pixel sizes")
    manifest(args)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("audit", "normalize", "board", "manifest"))
    parser.add_argument("--main-prompts", type=Path, default=MAIN_PROMPTS)
    parser.add_argument("--alt-prompts", type=Path, default=ALT_PROMPTS)
    parser.add_argument("--selection", type=Path, default=SELECTION)
    parser.add_argument("--variants", nargs="+", choices=VARIANTS, default=list(VARIANTS))
    parser.add_argument("--require-complete", action="store_true", help="Fail normalize if any requested valid source is missing")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.command == "audit":
        records = audit(args)
        print(json.dumps({"expected": 12, "valid": sum(row["valid"] for row in records),
                          "missingOrInvalid": [f"{row['characterId']}/{row['variant']}" for row in records if not row["valid"]]}, ensure_ascii=False))
    elif args.command == "normalize":
        normalize(args)
    elif args.command == "board":
        board(args)
    else:
        manifest(args)


if __name__ == "__main__":
    main()
