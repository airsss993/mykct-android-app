"""Чёрный набор App Store: вся лента одной страницей 7740x2796 (layouts/black.html), режется на 6 кадров."""
import re
from pathlib import Path

L = Path(__file__).resolve().parent.parent / 'layouts'
BUTTONS = ''.join(f'<i class="iphone-button {b}"></i>' for b in ['action', 'volume-up', 'volume-down', 'power', 'camera-control'])
fire = re.search(r'<div class="iphone-screen">(.*?)<div class="dynamic-island">', (L / 'appstore-05.html').read_text(), re.S).group(1)
F = 1290  # ширина кадра


def phone(style, src=None, screen=None):
    inner = screen or f'<img src="../src/{src}" alt="">'
    return f'<div class="iphone" style="{style}">{BUTTONS}<div class="iphone-bezel"><div class="iphone-screen">{inner}<div class="dynamic-island"></div></div></div></div>'


def at(frame, x):
    return frame * F + x


def ring(cx, cy, d, pct, color, label, value):
    return (f'<div class="ring-big" style="left:{cx - d // 2}px;top:{cy - d // 2}px;width:{d}px;font-size:{d}px;--p:{pct}%;--c:{color}">'
            f'<div><b>{value}</b><span>{label}</span></div></div>')


def chalk(name, x, y, w, rot=0):
    return f'<img class="chalk" src="../assets/chalk/{name}.png" alt="" style="left:{x}px;top:{y}px;width:{w}px;transform:rotate({rot}deg)">'


def tile(x, y, w, h, body, cls=''):
    return f'<div class="tile-dark {cls}" style="left:{x}px;top:{y}px;width:{w}px;height:{h}px">{body}</div>'


parts = [
    # 1: телефон сверху, огромный заголовок снизу
    f'<div class="glow" style="left:{at(0, 100)}px;top:-200px"></div>',
    phone(f'--device-width:860px;top:-330px;left:{at(0, 330)}px;transform:perspective(4200px) rotateY(-14deg) rotateX(6deg) rotate(14deg)', '01-welcome.png'),
    f'<h1 style="left:{at(0, 60)}px;top:1560px;font-size:200px">весь<br><em>колледж</em><br>в одном<br>приложении</h1>',
    # 2: расписание
    f'<h1 style="left:{at(1, 70)}px;top:150px">расписание<br><em>твоей группы</em></h1>',
    f'<p class="sub" style="left:{at(1, 76)}px;top:640px">Неделя целиком, звонки и кабинеты.<br>Работает без входа.</p>',
    f'<div class="glow" style="left:{at(1, 150)}px;top:1100px"></div>',
    phone(f'--device-width:1040px;top:840px;left:{at(1, 125)}px;transform:perspective(4200px) rotateY(-8deg) rotateX(4deg)', '15-schedule-week-light.png'),
    # 3: пропуски - телефон с посещаемостью, кольцо поверх заходит в кадр 4
    f'<h1 style="left:{at(2, 60)}px;top:150px;font-size:156px">пропуски<br><em>под контролем</em></h1>',
    f'<p class="sub" style="left:{at(2, 66)}px;top:480px">Календарь месяца: где был,<br>где пропустил.</p>',
    f'<div class="glow" style="left:{at(2, 100)}px;top:1000px"></div>',
    phone(f'--device-width:1000px;top:760px;left:{at(2, 60)}px;transform:perspective(4200px) rotateY(12deg) rotateX(5deg) rotate(-5deg);--side:-1', '24-attendance-dark.png'),
    ring(at(2, 1010), 1950, 600, 92, '#30D158', 'посещаемость', '92%'),
    # 4: баллы - скриншот из приложения и плитка поверх
    f'<h1 style="left:{at(3, 60)}px;top:150px;font-size:160px">все <em>баллы</em><br>по предметам</h1>',
    f'<p class="sub" style="left:{at(3, 66)}px;top:480px">Оценка за каждое занятие<br>и средний балл.</p>',
    f'<div class="glow" style="left:{at(3, 200)}px;top:1000px"></div>',
    phone(f'--device-width:1000px;top:760px;left:{at(3, 230)}px;transform:perspective(4200px) rotateY(-12deg) rotateX(5deg) rotate(5deg)', '11-scores-sheet-light.png'),
    # 5: стрик
    f'<h1 style="left:{at(4, 60)}px;top:150px;font-size:150px">система учёта<br><em class="fire">посещаемости</em></h1>',
    f'<div class="glow fire" style="left:{at(4, 200)}px;top:900px"></div>',
    phone(f'--device-width:1380px;top:700px;left:{at(4, 40)}px;transform:perspective(4200px) rotateY(-14deg) rotateX(6deg) rotate(-11deg)', screen=fire),
    # 6: OLED
    f'<div class="glow" style="left:{at(5, 150)}px;top:200px"></div>',
    phone(f'--device-width:900px;top:60px;left:{at(5, 230)}px;transform:perspective(4200px) rotateY(14deg) rotateX(6deg) rotate(-8deg);--side:-1', '26-schedule-week-black.png'),
    f'<h1 style="left:{at(5, 60)}px;top:2240px">тёмная тема<br><em>для OLED</em></h1>',
    # меловые штрихи: у заголовков и на стыках кадров
    chalk('spiral', at(0, 940), 1250, 330, 10),
    chalk('burst', at(0, 1010), 1700, 170, 20),
    chalk('stroke', at(1, 380), 470, 520, -3),
    chalk('arrow', at(1, 1000), 640, 560, 8),
    chalk('asterisk', at(2, 1000), 110, 190),
    chalk('loop', at(3, 1000), 330, 330, 0),
    chalk('zigzag', at(3, 60), 2300, 230, -10),
    chalk('zigzag', at(4, 1060), 560, 210, 10),
    chalk('star', at(4, 60), 560, 180, -8),
    chalk('scribble', at(5, 960), 1960, 280, -6),
    chalk('oval2', at(5, 390), 2350, 560, -3),
]

(L / 'black.html').write_text(f'''<!doctype html>
<html lang="ru">
<head><meta charset="utf-8"><title>МойКЦТ - чёрная лента</title><link rel="stylesheet" href="shared.css"><link rel="stylesheet" href="black.css"></head>
<body><main class="canvas appstore black">
{chr(10).join(parts)}
</main></body>
</html>
''')
