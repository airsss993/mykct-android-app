"""Апскейл x4 через Real-ESRGAN (spandrel). RGB гоним через модель, альфу отдельно тем же x4 моделью как серую картинку.
python3 tools/upscale.py вход.png выход.png путь/к/RealESRGAN_x4plus.pth"""
import sys

import numpy as np
import spandrel
import torch
from PIL import Image

src, dst, weights = sys.argv[1:4]
dev = 'mps' if torch.backends.mps.is_available() else 'cpu'
model = spandrel.ModelLoader().load_from_file(weights).eval().to(dev)


def up(rgb, tile=256, pad=16):
    """Тайлами, чтобы не упереться в память; края тайлов перекрываются на pad."""
    a = torch.from_numpy(np.asarray(rgb, np.float32) / 255).permute(2, 0, 1)[None]
    _, _, h, w = a.shape
    out = torch.zeros(1, 3, h * 4, w * 4)
    for y in range(0, h, tile):
        for x in range(0, w, tile):
            y0, x0, y1, x1 = max(y - pad, 0), max(x - pad, 0), min(y + tile + pad, h), min(x + tile + pad, w)
            with torch.no_grad():
                r = model(a[..., y0:y1, x0:x1].to(dev)).cpu()
            out[..., y * 4:min(y + tile, h) * 4, x * 4:min(x + tile, w) * 4] = \
                r[..., (y - y0) * 4:(y - y0 + min(tile, h - y)) * 4, (x - x0) * 4:(x - x0 + min(tile, w - x)) * 4]
    return Image.fromarray((out[0].permute(1, 2, 0).clamp(0, 1).numpy() * 255).round().astype(np.uint8))


im = Image.open(src).convert('RGBA')
rgb = up(im.convert('RGB'))
alpha = up(im.getchannel('A').convert('RGB')).convert('L')
rgb.putalpha(alpha)
rgb.save(dst)
print(im.size, '->', rgb.size)
