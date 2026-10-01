# AILUA Theme Package Schema v1

An .ailuatheme file is a ZIP archive that AILUA can import locally. The importer recognizes its contents even if a file provider supplies a generic or misleading filename.

## Archive layout

manifest.json and theme.json are required at the ZIP root. Image directories are optional:

~~~text
example.ailuatheme
├── manifest.json
├── theme.json
├── preview/
│   └── home.jpg
├── wallpaper/
│   └── home.webp
└── icons/
    └── com.android.contacts.png
~~~

The importer reads .png, .jpg, .jpeg, and .webp files under preview/, wallpaper/, and the icon directory configured in theme.json. It does not currently load fonts/ or sounds/. Use distinct image basenames within each category because imported assets are stored by category and basename.

## manifest.json

~~~json
{
  "schema": 1,
  "id": "ailua.diary.sakura",
  "name": "Sakura Diary",
  "author": "AILUA",
  "version": "1.0.0"
}
~~~

- schema must be the integer 1; other values are rejected.
- id is a naming hint. The installed ID is sanitized and receives a hash of the full ZIP bytes, so different package contents have different IDs. It is not a verified publisher identity.
- name, author, and version are displayed as package metadata. The importer supplies defaults when optional fields are absent.

## theme.json

~~~json
{
  "basePreset": "diary",
  "palette": "sakura",
  "wallpaper": "wallpaper/home.webp",
  "icons": {
    "path": "icons/"
  }
}
~~~

- basePreset and palette identify built-in theme tokens. The importer records both. Applying an imported package currently keeps the user's selected built-in shell and applies the palette when available.
- wallpaper selects which imported wallpaper appears first. If the reference is absent or does not match an included image, other imported wallpapers remain available.
- icons.path is the ZIP path prefix for icon images; it defaults to icons/. Icon filenames are normalized into app keys. For example, com.android.contacts.png maps to com.android.contacts.
- Imported wallpapers and icons can be mixed with the current built-in theme. A package may contain only a subset of these assets.

The earlier nested form is also accepted:

~~~json
{
  "preset": {
    "skin": "diary",
    "palette": "sakura",
    "wallpaper": "wallpaper/home.webp"
  }
}
~~~

The nested skin, palette, and wallpaper fields supply fallback values when their top-level equivalents are absent. Other nested visual token names are reserved and currently have no import effect.

## Validation and installation

ZIP entry paths must be relative and cannot contain empty, . or .. segments. Absolute drive paths, duplicate file paths, and entries outside the archive are rejected. The importer limits the source ZIP and total extracted bytes to 80 MiB, each extracted entry to 12 MiB, and stored file entries to 2,048. Image filenames are filtered by extension; invalid image data can still fail to render.

After preview and confirmation, normalized assets are stored in the app's private files/themes/<installed-id>/ directory, with a generated manifest for later loading. The original package is not executed. Deleting an imported theme removes its stored assets and clears any active wallpaper or icon source that points to it.

The format does not define dynamic clock/calendar icons, font or sound playback, package signatures, or publisher verification. MIUI .mtz, ColorOS .theme, and installed Android icon packs use separate import paths.