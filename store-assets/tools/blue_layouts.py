"""Синий набор App Store: layouts/blue-01..06.html. Экран стрика берётся из appstore-05.html."""
import re
from pathlib import Path

L = Path(__file__).resolve().parent.parent / 'layouts'
BUTTONS = ''.join(f'<i class="iphone-button {b}"></i>' for b in ['action', 'volume-up', 'volume-down', 'power', 'camera-control'])
fire = re.search(r'<div class="iphone-screen">(.*?)<div class="dynamic-island">', (L / 'appstore-05.html').read_text(), re.S).group(1)


def phone(src=None, screen=None, cls=''):
    inner = screen or f'<img src="../src/{src}" alt="">'
    return f'<div class="iphone {cls}">{BUTTONS}<div class="iphone-bezel"><div class="iphone-screen">{inner}<div class="dynamic-island"></div></div></div></div>'


def tile(cls, body):
    return f'<div class="glass tile {cls}">{body}</div>'


frames = {
    1: ('hero', '''<div class="glass app-badge"><img src="../assets/app-icon.png" alt=""><div><b>МойКЦТ</b><span>расписание, баллы, посещаемость</span></div></div>
<h1 class="top">весь<br><em>колледж</em><br>в одном<br>приложении</h1>
<p class="sub bottom-note">Для студентов колледжа. Расписание открывается даже без входа.</p>'''),
    2: ('schedule', phone('15-schedule-week-light.png') + '''
<h1 class="low">расписание<br><em>твоей группы</em></h1>
<p class="sub low-sub">Неделя целиком, звонки и кабинеты. Работает без входа.</p>'''),
    3: ('scores', '''<h1 class="top">все <em>баллы</em><br>по каждому<br>предмету</h1>
<p class="sub top-sub">Оценки по занятиям и средний балл за полугодие.</p>''' + phone('11-scores-sheet-light.png')),
    4: ('widgets', '<div class="tiles">'
        + tile('t-ring', '<div class="att-ring"><b>92%</b></div><span>посещаемость<br>за месяц</span>')
        + tile('t-pairs', '<b>14</b><span>пар на неделе</span>')
        + tile('t-score', '<b>5<small>/5</small></b><span>Вёрстка макета</span>')
        + tile('t-rank', '<b>14</b><span>место на курсе</span>')
        + tile('t-streak', '<i class="fire-icon"></i><b>30</b><span>дней подряд</span>')
        + '</div><h1 class="low">всё важное<br><em>одним взглядом</em></h1>'),
    5: ('streak', '<h1 class="top">система учёта<br><em>посещаемости</em></h1>' + phone(screen=fire)),
    6: ('oled', '''<h1 class="top">тёмная тема<br><em>для OLED</em></h1>
<p class="sub top-sub">Чистый чёрный фон бережёт заряд и глаза вечером.</p>''' + phone('26-schedule-week-black.png')),
}

for n, (cls, body) in frames.items():
    (L / f'blue-0{n}.html').write_text(f'''<!doctype html>
<html lang="ru">
<head><meta charset="utf-8"><title>МойКЦТ - синий {n}</title><link rel="stylesheet" href="shared.css"><link rel="stylesheet" href="blue.css"></head>
<body><main style="--i:{n - 1}" class="canvas appstore blue {cls}">
{body}
</main></body>
</html>
''')
