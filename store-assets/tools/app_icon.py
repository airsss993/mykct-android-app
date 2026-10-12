"""Иконка приложения из стеклянного медведя (assets/bear-cut@4x.png) -> ~/Desktop/mykct-icon/.
iOS: один квадрат 1024 без прозрачности, углы скругляет система.
Android: адаптивная иконка - фон сплошной BG (цвет в drawable/app_icon_adaptive_background.xml),
передний слой - свечение + медведь в безопасной зоне 66 из 108 dp; плюс обычная, круглая и 512 для стора."""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

R = Path(__file__).resolve().parent.parent
OUT = Path.home() / 'Desktop' / 'mykct-icon'
BG = (7, 6, 12)
BEAR = Image.open(R / 'assets' / 'bear-cut@4x.png')
DENSITY = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}


def glow(size, strength=.55):
    """Фиолетовое свечение из центра, прозрачное к краям (как на экране входа)."""
    y, x = np.mgrid[0:size, 0:size] / size - .5
    a = np.clip(1 - np.hypot(x, y * 1.1) / .5, 0, 1) ** 1.6 * strength
    rgba = np.zeros((size, size, 4), np.uint8)
    rgba[..., :3] = (88, 60, 200)
    rgba[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(rgba)


def layer(size, bear_share, opaque):
    """Квадрат с медведем по центру; bear_share - доля стороны, которую занимает медведь по высоте."""
    im = Image.new('RGBA', (size, size), BG + (255,) if opaque else (0, 0, 0, 0))
    im.alpha_composite(glow(size))
    h = round(size * bear_share)
    b = BEAR.resize((round(BEAR.width * h / BEAR.height), h), Image.LANCZOS)
    im.alpha_composite(b, ((size - b.width) // 2, (size - h) // 2 + round(size * .01)))
    return im


def masked(im, shape):
    m = Image.new('L', im.size, 0)
    d, s = ImageDraw.Draw(m), im.width
    if shape == 'circle':
        d.ellipse((0, 0, s - 1, s - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, s - 1, s - 1), round(s * .18), fill=255)
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    out.paste(im, (0, 0), m)
    return out


def main():
    (OUT / 'ios').mkdir(parents=True, exist_ok=True)
    layer(1024, .72, True).convert('RGB').save(OUT / 'ios' / 'AppIcon-1024.png')
    layer(512, .72, True).convert('RGB').save(OUT / 'android-playstore-512.png')

    full = layer(1024, .72, True)
    for name, k in DENSITY.items():
        d = OUT / 'android' / 'res' / f'mipmap-{name}'
        d.mkdir(parents=True, exist_ok=True)
        s = round(48 * k)
        masked(full, 'rect').resize((s, s), Image.LANCZOS).save(d / 'app_icon.webp', lossless=True)
        masked(full, 'circle').resize((s, s), Image.LANCZOS).save(d / 'app_icon_round.webp', lossless=True)
        # передний слой 108 dp, видимая часть - круг 72 dp, гарантированно целая - 66 dp
        layer(round(108 * k), .5, False).save(d / 'app_icon_foreground.webp', lossless=True)
    (OUT / 'android' / 'res' / 'drawable').mkdir(exist_ok=True)
    (OUT / 'android' / 'res' / 'drawable' / 'app_icon_adaptive_background.xml').write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:shape="rectangle">\n    <solid android:color="#%02X%02X%02X" />\n</shape>\n' % BG)
    print('готово:', OUT)


if __name__ == '__main__':
    main()
