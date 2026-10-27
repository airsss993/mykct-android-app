"""Собирает Android-ленту 6480x1920 из оригинальных скриншотов и HTML-декора."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LAYOUTS = ROOT / 'layouts'
F = 1080


def phone(name, x, y, width, angle=0, screen=None):
    content = screen if screen is not None else f'<img src="../src-android/{name}" alt="Экран приложения МойКЦТ">'
    return f'''<div class="android-phone" style="left:{x}px;top:{y}px;--device-width:{width}px;--angle:{angle}deg">
<i class="side-button power"></i><i class="side-button volume"></i>
<div class="android-screen">{content}<i class="camera"></i></div></div>'''


def title(x, y, content, size=108):
    return f'<h1 style="left:{x}px;top:{y}px;font-size:{size}px">{content}</h1>'


def chalk(name, x, y, width, angle=0):
    return f'<img class="chalk" src="../assets/chalk/{name}.png" alt="" style="left:{x}px;top:{y}px;width:{width}px;transform:rotate({angle}deg)">'


def ring(x, y, d, value, label, pct, color, cls=''):
    return f'<div class="ring-big {cls}" style="left:{x}px;top:{y}px;width:{d}px;--p:{pct}%;--c:{color}"><div><b>{value}</b><span>{label}</span></div></div>'


def tile(x, y, w, h, body, cls=''):
    return f'<div class="tile-dark {cls}" style="left:{x}px;top:{y}px;width:{w}px;height:{h}px">{body}</div>'


source = (LAYOUTS / 'appstore-05.html').read_text()
fire = re.search(r'<div class="iphone-screen">(.*?)<div class="dynamic-island">', source, re.S).group(1)
parts = [
    '<div class="glow" style="left:0;top:-280px"></div>',
    phone('01-welcome.png', 255, -250, 650, 13),
    title(56, 1270, 'весь<br><em>колледж</em><br>в одном<br>приложении', 139),
    title(F + 60, 105, 'расписание<br><em>твоей группы</em>', 106),
    f'<p class="sub" style="left:{F+66}px;top:415px">Неделя целиком, звонки и кабинеты.<br>Работает без входа.</p>',
    f'<div class="glow" style="left:{F+70}px;top:690px"></div>',
    phone('15-schedule-light.png', F + 110, 585, 850, -1),
    ring(2140, 85, 320, '9', 'пар<br>на неделе', 75, '#9B8CFF', 'week-ring'),
    title(F * 2 + 60, 710, 'пропуски<br><em>под контролем</em>', 107),
    f'<div class="glow" style="left:{F*2}px;top:1000px"></div>',
    phone('24-attendance-dark.png', F * 2 + 130, 975, 770, 3),
    ring(F * 2 + 650, 220, 440, '79%', 'посещаемость<br><small>Был на 46 из 58 пар</small>', 79, '#30D158', 'attendance-ring'),
    title(F * 3 + 60, 105, 'все <em>баллы</em><br>по предметам', 107),
    tile(F * 3 + 595, 390, 420, 230, '<b>6</b><span>предметов в<br>полугодии</span>', 'count'),
    tile(F * 3 + 60, 740, 465, 360, '<i>20.09</i><b class="v">5<small>/5</small></b><span>Вёрстка макета</span>'),
    tile(F * 3 + 550, 740, 465, 360, '<i>5 оценок</i><b>4.2</b><span>средний балл</span>'),
    tile(F * 3 + 60, 1125, 955, 230, '<div class="bars"><s style="--v:80%"></s><s style="--v:100%"></s><s style="--v:60%"></s><s style="--v:80%"></s><s style="--v:100%"></s></div><span>Разработка модулей ПО</span>', 'wide'),
    tile(F * 3 + 60, 1380, 465, 320, '<b>3<small>/5</small></b><span>Формы и валидация</span>'),
    tile(F * 3 + 550, 1380, 465, 320, '<b class="v">4<small>/5</small></b><span>Реферат</span>'),
    tile(F * 3 + 60, 1725, 955, 145, '<b class="v">4<small>/5</small></b><span>Тест по модулю</span>', 'test-score'),
    title(F * 4 + 60, 105, 'система учёта<br><em class="fire">посещаемости</em>', 102),
    f'<div class="glow fire" style="left:{F*4}px;top:720px"></div>',
    phone(None, F * 4 + 60, 570, 940, -7, fire),
    f'<div class="glow" style="left:{F*5}px;top:100px"></div>',
    phone('26-schedule-black.png', F * 5 + 180, 80, 670, -8),
    title(F * 5 + 60, 1630, 'тёмная тема<br><em>для OLED</em>', 114),
    chalk('spiral', 870, 1090, 190, 10),
    chalk('burst', 885, 1380, 120, 20),
    chalk('stroke', F + 310, 305, 260, -3),
    chalk('arrow', 1980, 475, 365, 8),
    chalk('asterisk', F * 2 + 890, 895, 135),
    chalk('loop', F * 3 + 900, 270, 210),
    chalk('star', F * 4 + 80, 410, 115, -8),
    chalk('zigzag', F * 4 + 915, 440, 115, 10),
    chalk('scribble', F * 5 + 830, 1460, 180, -6),
]
html = '''<!doctype html>
<html lang="ru">
<head><meta charset="utf-8"><meta name="viewport" content="width=6480"><title>МойКЦТ - Android</title><link rel="stylesheet" href="shared.css"><link rel="stylesheet" href="android.css"></head>
<body><main class="canvas android" aria-label="Шесть кадров МойКЦТ для RuStore и Google Play">
''' + '\n'.join(parts) + '\n</main></body></html>\n'
assert html.count('class="android-phone"') == 5
assert 'dynamic-island' not in html
assert not re.search('[\u2014\u2013\u00ab\u00bb\u201c\u201d\u201e\u2018\u2019\u2026\u00a0\u200b]', html)
(LAYOUTS / 'android.html').write_text(html)
