"""Режет лист меловых штрихов gen/chalk-panorama.png на отдельные PNG: белый цвет, прозрачность по яркости."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
K = 2172 / 2000  # рамки сняты на превью шириной 2000
BOXES = {
    'oval': (50, 145, 245, 325), 'asterisk': (370, 185, 510, 345), 'spiral': (635, 160, 845, 415),
    'arrow': (970, 185, 1235, 320), 'wave': (1350, 185, 1590, 340), 'loop': (1675, 135, 1945, 330),
    'zigzag': (65, 415, 225, 530), 'stroke': (345, 440, 595, 510), 'burst': (835, 440, 950, 530),
    'oval2': (1080, 385, 1275, 530), 'scribble': (1415, 405, 1630, 535), 'star': (1770, 400, 1945, 530),
}
sheet = Image.open(ROOT / 'gen/chalk-panorama.png').convert('L')
for name, box in BOXES.items():
    a = sheet.crop(tuple(round(v * K) for v in box))
    a = a.resize((a.width * 3, a.height * 3), Image.LANCZOS).point(lambda v: 0 if v < 40 else min(255, int((v - 40) * 1.4)))
    out = Image.new('RGBA', a.size, (255, 255, 255, 0))
    out.putalpha(a)
    out.save(ROOT / f'assets/chalk/{name}.png')
