"""Render the first AILUA UI Rescue wallpaper studies, without external assets.

All geometry, palettes and textures in this file are authored for AILUA. There
are no photographs, stock images, third-party textures or generative API calls.
Soft Home is a deliberately illustrated still-life study, not a photograph.
Output: native 1080 x 2340 lossless WebP images in drawable-nodpi.

Run: python scripts/render_theme_wallpapers.py
Requires Pillow and numpy. The fixed seed makes each output reproducible.
"""

from pathlib import Path
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


WIDTH, HEIGHT = 1080, 2340
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/drawable-nodpi"
Y, X = np.mgrid[0:HEIGHT, 0:WIDTH].astype(np.float32)
RNG = np.random.default_rng(530)


def color(hex_value):
    return np.array([int(hex_value[i : i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)


def blend(top, bottom, amount):
    fraction = np.clip(amount, 0, 1)[..., None]
    return color(top) * (1 - fraction) + color(bottom) * fraction


def image_from(array):
    return Image.fromarray(np.uint8(np.clip(array, 0, 255)), "RGB").convert("RGBA")


def aa_mask(draw_function):
    """Rasterize vector masks at 2x, preserving clean silhouettes."""
    mask = Image.new("L", (WIDTH * 2, HEIGHT * 2))
    draw_function(ImageDraw.Draw(mask), 2)
    return mask.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)


def polygon_mask(points):
    return aa_mask(lambda draw, scale: draw.polygon(
        [(round(x * scale), round(y * scale)) for x, y in points], fill=255
    ))


def ellipse_mask(bounds):
    return aa_mask(lambda draw, scale: draw.ellipse(
        tuple(round(v * scale) for v in bounds), fill=255
    ))


def soft_shadow(canvas, mask, shift=(0, 20), radius=32, opacity=55, tone=(25, 24, 26)):
    shadow_mask = Image.new("L", (WIDTH, HEIGHT))
    shadow_mask.paste(mask, shift)
    shadow_mask = shadow_mask.filter(ImageFilter.GaussianBlur(radius))
    shadow_mask = shadow_mask.point(lambda value: value * opacity // 255)
    layer = Image.new("RGBA", canvas.size, tone + (0,))
    layer.putalpha(shadow_mask)
    canvas.alpha_composite(layer)


def paste_shaded(canvas, rgb_array, mask):
    canvas.paste(image_from(rgb_array), (0, 0), mask)


def noise(array, amount=0.75):
    return array + RNG.normal(0, amount, (HEIGHT, WIDTH, 1))


def elliptical_ribbon(canvas, center, axes, angle, inner, outer, top, bottom,
                      highlight=20, shadow=45):
    """A softly lit curved paper ribbon, with its own geometric backdrop shadow."""
    dx, dy = X - center[0], Y - center[1]
    cosine, sine = math.cos(angle), math.sin(angle)
    local_x = (dx * cosine + dy * sine) / axes[0]
    local_y = (-dx * sine + dy * cosine) / axes[1]
    radius = np.sqrt(local_x * local_x + local_y * local_y)
    # A feather only at the silhouette keeps the wallpaper itself crisp.
    alpha = np.minimum(np.clip((radius - inner) * 260, 0, 1),
                       np.clip((outer - radius) * 260, 0, 1))
    mask = Image.fromarray(np.uint8(alpha * 255), "L")
    soft_shadow(canvas, mask, shift=(13, 35), radius=28, opacity=shadow)
    cross_section = np.clip((radius - inner) / (outer - inner), 0, 1)
    lighting = np.sin(cross_section * math.pi) * highlight
    directional = -local_x * 6 - local_y * 8
    gradient = blend(top, bottom, (Y / HEIGHT) * 0.7 + local_x * 0.15 + 0.15)
    paste_shaded(canvas, noise(gradient + (lighting + directional)[..., None], 0.48), mask)


def render_default():
    """Large folded forms; restrained blue, paper cream and warm coral."""
    canvas = image_from(noise(blend("D1E4E9", "89B9CE", Y / HEIGHT), 0.55))
    glow = np.exp(-((X - 940) ** 2 / 800000 + (Y - 200) ** 2 / 1200000))
    base = np.asarray(canvas)[..., :3].astype(np.float32) + glow[..., None] * 16
    canvas = image_from(base)
    # The cream fold is deliberately large enough to read behind widgets.
    elliptical_ribbon(canvas, (1380, 330), (1000, 1250), -0.18,
                      0.38, 1.07, "F4EBDD", "D2BFA5", highlight=24, shadow=32)
    elliptical_ribbon(canvas, (-115, 1800), (650, 935), 0.2,
                      0.29, 0.79, "6EABB7", "347D91", highlight=18, shadow=48)
    elliptical_ribbon(canvas, (935, 2360), (770, 1070), -0.45,
                      0.46, 0.91, "E2A48E", "C0776C", highlight=21, shadow=36)
    # Smaller warm edge gives an intentional seam rather than another UI tile.
    return canvas.convert("RGB")


def line_layer(canvas, points, fill, width):
    layer = Image.new("RGBA", (WIDTH * 2, HEIGHT * 2))
    draw = ImageDraw.Draw(layer)
    draw.line([(round(x * 2), round(y * 2)) for x, y in points],
              fill=fill, width=round(width * 2), joint="curve")
    canvas.alpha_composite(layer.resize(canvas.size, Image.Resampling.LANCZOS))


def draw_book(canvas):
    # A quiet indigo clothbound volume with two exposed page blocks.
    cover = [(55, 1730), (459, 1504), (825, 1742), (398, 1995)]
    cover_mask = polygon_mask(cover)
    soft_shadow(canvas, cover_mask, shift=(10, 29), radius=26, opacity=50)
    paste_shaded(canvas, noise(blend("50636D", "293D49", (Y - 1480) / 530)), cover_mask)
    lower = [(79, 1726), (440, 1531), (791, 1750), (403, 1971)]
    paste_shaded(canvas, blend("D9CCB0", "B7A989", (Y - 1510) / 430), polygon_mask(lower))
    left_pages = [(92, 1702), (431, 1529), (462, 1742), (403, 1937)]
    right_pages = [(431, 1529), (774, 1730), (403, 1937), (462, 1742)]
    paste_shaded(canvas, noise(blend("EFE8D8", "D8C9AA", (X + Y - 1700) / 700), 0.65),
                 polygon_mask(left_pages))
    paste_shaded(canvas, noise(blend("E7DCC5", "F4ECD9", (X - 350) / 430), 0.65),
                 polygon_mask(right_pages))
    line_layer(canvas, [(431, 1536), (462, 1742), (403, 1930)], (97, 84, 61, 70), 3)
    # Sparse short marks are abstract paper texture, not embedded UI text.
    for index in range(11):
        fraction = index / 11
        start = (139 + 12 * index, 1689 + 14 * index)
        end = (384 + 4 * index, 1573 + 26 * index)
        line_layer(canvas, [start, end], (99, 91, 75, 22), 1.4)
    line_layer(canvas, [(648, 1604), (663, 1630), (565, 1700), (549, 1675)],
               (154, 72, 53, 190), 13)


def draw_cup(canvas, x, y, size, tea_color, body_top, body_bottom):
    """Slightly elevated view of a hand-thrown cup and matching saucer."""
    saucer = ellipse_mask((x - size * 0.73, y + size * 0.25,
                          x + size * 0.77, y + size * 0.85))
    soft_shadow(canvas, saucer, shift=(15, 25), radius=24, opacity=49)
    paste_shaded(canvas, blend("DCD2B9", "F0E8D5", (X - x + size) / (size * 2)), saucer)
    inner_saucer = ellipse_mask((x - size * 0.58, y + size * 0.34,
                                x + size * 0.63, y + size * 0.75))
    paste_shaded(canvas, blend("C2B59B", "E8DFC9", (X - x + size) / (size * 2)), inner_saucer)
    handle_outer = ellipse_mask((x + size * 0.31, y + size * 0.14,
                                x + size * 0.72, y + size * 0.52))
    handle_inner = ellipse_mask((x + size * 0.4, y + size * 0.22,
                                x + size * 0.62, y + size * 0.43))
    outer_array = np.asarray(handle_outer).astype(np.int16)
    inner_array = np.asarray(handle_inner).astype(np.int16)
    handle_mask = Image.fromarray(np.uint8(np.clip(outer_array - inner_array, 0, 255)))
    paste_shaded(canvas, blend(body_top, body_bottom, (X - x) / size), handle_mask)
    body = aa_mask(lambda draw, scale: draw.rounded_rectangle(
        tuple(round(value * scale) for value in
              (x - size * 0.48, y - size * 0.02, x + size * 0.48, y + size * 0.6)),
        radius=round(size * 0.23 * scale), fill=255))
    horizontal = np.clip((X - x + size * 0.5) / size, 0, 1)
    body_color = blend(body_top, body_bottom, horizontal)
    body_color += np.sin(horizontal * math.pi)[..., None] * 10
    paste_shaded(canvas, noise(body_color, 0.65), body)
    rim = ellipse_mask((x - size * 0.48, y - size * 0.21,
                        x + size * 0.48, y + size * 0.16))
    paste_shaded(canvas, blend("EFE5CD", "A79777", (Y - y + size * 0.2) / (size * 0.4)), rim)
    tea = ellipse_mask((x - size * 0.402, y - size * 0.152,
                        x + size * 0.405, y + size * 0.105))
    tea_gradient = blend(tea_color, "D2A668", (X - x + size * 0.4) / size)
    paste_shaded(canvas, tea_gradient, tea)
    glint = ellipse_mask((x - size * 0.28, y - size * 0.102,
                          x - size * 0.02, y - size * 0.074))
    layer = Image.new("RGBA", canvas.size, (255, 232, 189, 0))
    layer.putalpha(glint.point(lambda value: value * 78 // 255))
    canvas.alpha_composite(layer)


def draw_plant(canvas):
    x, y = 948, 1498
    pot_shadow = ellipse_mask((825, 1575, 1090, 1730))
    soft_shadow(canvas, pot_shadow, shift=(22, 30), radius=27, opacity=36)
    pot = polygon_mask([(841, 1478), (1082, 1478), (1050, 1687), (892, 1697)])
    gradient = blend("B97452", "D89C73", (X - 820) / 270)
    paste_shaded(canvas, noise(gradient, 0.7), pot)
    rim = ellipse_mask((830, 1438, 1092, 1525))
    paste_shaded(canvas, blend("DCA381", "AF6B4B", (Y - 1438) / 90), rim)
    soil = ellipse_mask((850, 1452, 1077, 1507))
    paste_shaded(canvas, noise(blend("4D4E38", "6B634B", (X - 850) / 230), 2.5), soil)
    stems = [(936, 1482, 834, 1197), (950, 1485, 1010, 1133),
             (971, 1490, 1098, 1274), (955, 1450, 927, 1014)]
    for x0, y0, x1, y1 in stems:
        line_layer(canvas, [(x0, y0), ((x0 + x1) / 2 - 7, (y0 + y1) / 2), (x1, y1)],
                   (66, 79, 49, 255), 6)
    leaves = [(838, 1190, -0.7, 70, 116), (886, 1327, 0.1, 62, 100),
              (1015, 1140, 0.7, 65, 130), (982, 1260, -0.35, 58, 101),
              (1094, 1280, 0.8, 90, 121), (936, 1037, 0.08, 68, 111),
              (946, 1165, -0.5, 69, 121)]
    for cx, cy, angle, rx, ry in leaves:
        dx, dy = X - cx, Y - cy
        u = (dx * math.cos(angle) + dy * math.sin(angle)) / rx
        v = (-dx * math.sin(angle) + dy * math.cos(angle)) / ry
        contour = np.sqrt(u * u + v * v)
        mask = Image.fromarray(np.uint8(np.clip((1 - contour) * 220, 0, 1) * 255))
        soft_shadow(canvas, mask, shift=(12, 19), radius=14, opacity=18)
        leaf_color = blend("667650", "354F40", (u + 1) / 2)
        leaf_color += np.clip(1 - np.abs(u) * 2, 0, 1)[..., None] * 10
        paste_shaded(canvas, noise(leaf_color, 0.5), mask)


def render_soft_home():
    """Warm illustrated afternoon; quiet wall, wood, book, two cups, leaves."""
    wall = blend("F5EFE0", "D9CEB6", Y / HEIGHT)
    sun = np.exp(-((X - 250) ** 2 / 850000 + (Y - 440) ** 2 / 700000))
    canvas = image_from(noise(wall + sun[..., None] * 6, 0.65))
    # Long afternoon light and a soft, asymmetrical window shadow.
    window_shadow = polygon_mask([(510, 0), (665, 0), (1140, 1230), (983, 1230)])
    light_layer = Image.new("RGBA", canvas.size, (120, 102, 74, 0))
    light_layer.putalpha(window_shadow.filter(ImageFilter.GaussianBlur(24)).point(
        lambda value: value * 13 // 255))
    canvas.alpha_composite(light_layer)
    table_mask = polygon_mask([(-20, 1340), (1100, 1060), (1100, 2360), (-20, 2360)])
    grain_phase = X * 0.024 + Y * 0.075 + np.sin(Y * 0.003) * 4
    grain = np.sin(grain_phase) * 1.5 + np.sin(grain_phase * 4.5) * 0.7
    wood = blend("D4AE80", "B88454", (Y - 1050) / 1400)
    wood += grain[..., None]
    wood += np.exp(-((X - 310) ** 2 + (Y - 1580) ** 2) / 1600000)[..., None] * 12
    paste_shaded(canvas, noise(wood, 0.65), table_mask)
    line_layer(canvas, [(-10, 1340), (1090, 1064)], (218, 188, 146, 120), 4)
    # Plank lines are quiet, warm and directional, never UI-like borders.
    for intercept in (1560, 1884, 2208, 2532):
        line_layer(canvas, [(-20, intercept), (1100, intercept - 280)],
                   (100, 68, 43, 32), 2)
    draw_book(canvas)
    draw_cup(canvas, 277, 1382, 225, "835628", "ECE0C4", "BFA989")
    draw_cup(canvas, 757, 1904, 256, "654429", "84908A", "566B66")
    draw_plant(canvas)
    # Unobtrusive linen in the foreground finishes the composition.
    linen = polygon_mask([(-10, 2206), (279, 2070), (544, 2310), (500, 2350), (-10, 2350)])
    soft_shadow(canvas, linen, shift=(0, 13), radius=15, opacity=18)
    linen_texture = blend("D5CAB3", "E6DCC7", (X + Y - 2200) / 700)
    linen_texture += (np.sin(X * 1.25) + np.sin(Y * 1.2))[..., None] * 0.4
    paste_shaded(canvas, noise(linen_texture, 0.6), linen)
    return canvas.convert("RGB")


def render_midnight():
    """Deep-ocean sculptural ribbon with legible contours and restrained light."""
    canvas = image_from(noise(blend("0C152B", "102E46", Y / HEIGHT), 0.35))
    # A broad sea-blue light separates the silhouette from the dark field.
    glow = np.exp(-((X - 970) ** 2 / 250000 + (Y - 1250) ** 2 / 1200000))
    base = np.asarray(canvas)[..., :3].astype(np.float32)
    base += glow[..., None] * np.array([8, 15, 24], dtype=np.float32)
    canvas = image_from(base)
    elliptical_ribbon(canvas, (945, 1450), (700, 1190), -0.23,
                      0.42, 0.68, "214562", "183C55", highlight=21, shadow=85)
    elliptical_ribbon(canvas, (-165, 2470), (870, 1350), 0.48,
                      0.55, 0.69, "335266", "152D49", highlight=12, shadow=70)
    # A thin blue-silver rim uses an actual silhouette, not a bloom over the image.
    dx, dy = X - 945, Y - 1450
    angle = -0.23
    u = (dx * math.cos(angle) + dy * math.sin(angle)) / 700
    v = (-dx * math.sin(angle) + dy * math.cos(angle)) / 1190
    radius = np.sqrt(u * u + v * v)
    rim = np.exp(-((radius - 0.68) * 340) ** 2)
    rim *= np.clip((-u - v + 0.5) * 0.6, 0, 0.8)
    highlights = Image.new("RGBA", canvas.size, (116, 155, 181, 0))
    highlights.putalpha(Image.fromarray(np.uint8(rim * 150)))
    canvas.alpha_composite(highlights)
    return canvas.convert("RGB")


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    renderers = {
        "wallpaper_default": render_default,
        "wallpaper_soft_home": render_soft_home,
        "wallpaper_midnight_glass": render_midnight,
    }
    for name, renderer in renderers.items():
        target = OUTPUT / f"{name}.webp"
        renderer().save(target, "WEBP", lossless=True, method=6)
        print(f"{name}: {WIDTH}x{HEIGHT}, {target.stat().st_size:,} bytes -> {target}")


if __name__ == "__main__":
    main()
