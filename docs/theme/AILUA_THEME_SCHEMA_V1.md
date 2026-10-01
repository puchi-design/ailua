# AILUA Theme Package Schema v1

A `.ailuatheme` file is a ZIP archive. This document reserves a portable package format for a future importer. AILUA P5.3 does **not** parse or install these files yet. Built-in themes currently use `ThemeCatalog` and `ThemeResolver`.

## Archive layout

```text
example.ailuatheme
├── manifest.json             required
├── theme.json                required
├── preview/                  optional preview media
├── wallpaper/                optional wallpaper assets
├── icons/                    reserved for app icon assets
├── fonts/                    reserved for fonts
└── sounds/                   reserved for audio
```

All paths in JSON are relative to the archive root, use forward slashes, and must remain within the archive. Consumers should reject absolute paths and `..` segments. A package must have exactly one root-level `manifest.json` and `theme.json`.

## manifest.json

```json
{
  "schema": 1,
  "id": "ailua.diary.sakura",
  "name": "Sakura Diary",
  "author": "AILUA",
  "version": "1.0.0"
}
```

- `schema` is the integer format version and must be `1`.
- `id` is a stable, namespaced package identifier. It must not collide with built-in theme IDs.
- `name` is the user-facing package name.
- `author` and `version` identify the package creator and release. A later importer will use both for display, not as trusted identity.

## theme.json

```json
{
  "preset": {
    "skin": "diary",
    "palette": "sakura",
    "wallpaper": "wallpaper/light.webp",
    "iconStyle": "paper",
    "widgetStyle": "note",
    "dockStyle": "paper_strip",
    "typography": "serif",
    "motion": "soft"
  }
}
```

`preset` provides visual references. `skin` names a base theme; `palette`, `iconStyle`, `widgetStyle`, `dockStyle`, `typography`, and `motion` select package or built-in tokens. `wallpaper` is an archive-relative image path. A future importer must validate every reference before installing a package and should reject missing resources or unknown token IDs. Theme selections remain separate from runtime visual objects: the resolver builds the final runtime after package resolution.

## Reserved integration points

The engine reserves a `ThemePackageProvider` so a future `AiluaPackageProvider` can list and resolve imported packages next to built-in presets. An `IconSource` abstraction reserves Android icon-pack, theme-package, MTZ, and ColorOS sources. This schema does not specify those external formats, dynamic calendar/clock metadata, font loading, sound playback, image decoding, or package signature verification. Import behavior, limits, and security validation belong to the future importer implementation.
