"""Промо редизайна: фото рук с зелёным экраном (gen/hands.png) + скрин расписания + подписи по углам -> out/promo-redesign.png."""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
from scipy import ndimage

R = Path(__file__).resolve().parent.parent
W, H = 2160, 2700
# python3 tools/promo.py [фото из gen/] [имя в out/] [скрин из src/]
ARGS = sys.argv[1:] + [None] * 3
PHOTO = ARGS[0] or 'hands.png'
OUTNAME = ARGS[1] or 'promo-redesign.png'
SCREEN = R / 'src' / (ARGS[2] or '15-schedule-week-light.png')

CORNERS = {  # (якорь, строки)
    'tl': ['МойКЦТ'],
    'tc': ['Редизайн приложения'],
    'tr': ['Октябрь 2026'],
    'bl': ['Иван Коломацкий', 'Артём Джапаридзе'],
    'bc': ['Скоро обновление'],
}


def cover(im):
    """Обрезка фото под 4:5 и масштаб до W x H."""
    w, h = im.size
    if w / h > W / H:
        nw = int(h * W / H)
        im = im.crop(((w - nw) // 2, 0, (w + nw) // 2, h))
    else:
        nh = int(w * H / W)
        im = im.crop((0, (h - nh) // 2, w, (h + nh) // 2))
    return im.resize((W, H), Image.LANCZOS)


def green_mask(a):
    r, g, b = (a[..., i].astype(int) for i in range(3))
    m = (g > 110) & (g - np.maximum(r, b) > 50)
    lab, n = ndimage.label(m)
    biggest = 1 + np.argmax(ndimage.sum(m, lab, range(1, n + 1)))
    # дыры внутри экрана (Dynamic Island) не трогаем: остров потом рисуем сами
    return lab == biggest


def quad(m):
    """Углы экрана: крайние точки маски вдоль осей экрана, оси уточняются по самим углам.
    Правый нижний угол часто закрыт пальцем - тогда он достраивается как у параллелограмма."""
    ys, xs = np.nonzero(m)
    P = np.stack([xs, ys], 1).astype(float)
    u, v = np.array([1., 0]), np.array([0., 1])
    for _ in range(4):
        tl, tr, bl = (P[(P @ d).argmax()] for d in (-(u + v), u - v, v - u))
        u, v = (tr - tl) / np.linalg.norm(tr - tl), (bl - tl) / np.linalg.norm(bl - tl)
    br = P[(P @ (u + v)).argmax()]
    guess = tr + bl - tl
    if np.linalg.norm(br - guess) > .1 * np.linalg.norm(tr - tl):
        br = guess
    # крайние точки лежат на дугах скруглений; настоящий угол дальше на k по диагонали,
    # k = r(1 - 1/sqrt2), радиус экрана r ~ 0.135 ширины (как в мокапе shared.css)
    k = .043 * np.linalg.norm(tr - tl)
    return np.array([tl - k * (u + v), tr + k * (u - v), br + k * (u + v), bl + k * (v - u)])


def perspective(src_size, dst):
    """Коэффициенты Image.PERSPECTIVE: точка результата -> точка исходника."""
    w, h = src_size
    srcp = [(0, 0), (w, 0), (w, h), (0, h)]
    A, B = [], []
    for (x, y), (u, v) in zip(dst, srcp):
        A += [[x, y, 1, 0, 0, 0, -u * x, -u * y], [0, 0, 0, x, y, 1, -v * x, -v * y]]
        B += [u, v]
    return np.linalg.solve(np.array(A, float), np.array(B, float))


def font(size, weight):
    f = ImageFont.truetype(str(R / 'fonts' / 'onest_variable.ttf'), size)
    f.set_variation_by_axes([weight])
    return f


def bear_logo(out):
    """Стеклянный медведь с экрана входа (assets/bear-cut.png) в правом нижнем углу."""
    pad, h = 110, 150
    bear = Image.open(R / 'assets' / 'bear-cut@4x.png')
    bear = bear.resize((round(bear.width * h / bear.height), h), Image.LANCZOS)
    x, y = W - pad - bear.width, H - pad - h + 8
    glow = Image.new('RGBA', out.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((x - 40, y - 30, x + bear.width + 40, y + h + 30), fill=(116, 100, 238, 90))
    base = Image.alpha_composite(out.convert('RGBA'), glow.filter(ImageFilter.GaussianBlur(50)))
    base.alpha_composite(bear, (x, y))
    out.paste(base.convert('RGB'))


def main():
    photo = cover(Image.open(R / 'gen' / PHOTO).convert('RGB'))
    a = np.asarray(photo)
    m = green_mask(a)
    q = quad(m)

    shot = Image.open(SCREEN).convert('RGB')
    warped = shot.transform((W, H), Image.PERSPECTIVE, tuple(perspective(shot.size, q)), Image.BICUBIC)

    # маска с мягким краем и убранным зелёным ореолом на пальце и рамке
    alpha = Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(1.2))
    out = Image.composite(warped, photo, alpha)
    o = np.asarray(out).astype(int)
    near = np.asarray(alpha.filter(ImageFilter.MaxFilter(15))) > 0
    spill = near & (o[..., 1] > np.maximum(o[..., 0], o[..., 2]))
    o[..., 1][spill] = np.maximum(o[..., 0], o[..., 2])[spill]
    out = Image.fromarray(o.astype(np.uint8))

    d = ImageDraw.Draw(out)
    f, pad, lh = font(40, 450), 110, 54
    for key, lines in CORNERS.items():
        for i, t in enumerate(lines):
            tw = d.textlength(t, font=f)
            x = {'l': pad, 'c': (W - tw) / 2, 'r': W - pad - tw}[key[1]]
            y = pad + i * lh if key[0] == 't' else H - pad - (len(lines) - i) * lh
            d.text((x, y), t, font=f, fill=(200, 198, 220))
    bear_logo(out)

    out.save(R / 'out' / OUTNAME)
    print('quad', q.round().tolist())


if __name__ == '__main__':
    main()
