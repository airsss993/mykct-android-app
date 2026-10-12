"""Тизер для чата: сцены layouts/teaser-*.html -> Chrome -> кадры с переходами -> out/teaser.gif и out/teaser.mp4."""
import subprocess
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter

R = Path(__file__).resolve().parent.parent
L, OUT = R / 'layouts', R / 'out'
FR = OUT / '_teaser'
W, H = 1080, 1350
CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'


def phone(src, cls=''):
    return (f'<div class="iphone {cls}" style="--device-width:700px;left:190px;top:470px;transform:perspective(3600px) rotateX(5deg)">'
            f'<div class="iphone-bezel"><div class="iphone-screen"><img src="../src/{src}" alt=""><div class="dynamic-island"></div></div></div></div>')


def chalk(name, x, y, w, rot=0):
    return f'<img class="chalk" src="../assets/chalk/{name}.png" alt="" style="left:{x}px;top:{y}px;width:{w}px;transform:rotate({rot}deg)">'


def themes(active, src):
    pills = ''.join(f'<span class="{"on" if n == active else ""}">{n}</span>' for n in ['светлая', 'тёмная', 'чёрная'])
    return '<h1 style="top:90px"><em>три</em> темы</h1>' f'<div class="pills">{pills}</div>' + phone(src)


def secret(name, src):
    return ('<h1 style="top:90px">и ещё<br><em>кое-что</em></h1>'
            + phone(src, 'secret') + f'<div class="stamp">засекречено</div><div class="tag">{name}</div>')


SCENES = {
    'intro': '<img class="icon" src="../assets/app-icon.png" alt="">'
             '<h1 class="mid" style="top:700px">МойКЦТ</h1><p class="sub mid" style="top:860px">новая версия</p>'
             + chalk('burst', 650, 240, 260, 15) + chalk('spiral', 110, 980, 300, -10),
    'login': '<h1 style="top:90px">новый<br><em>вход</em></h1>' + phone('01-welcome.png') + chalk('arrow', 690, 220, 340, 20),
    'schedule': '<h1 style="top:90px">расписание<br><em>твоей группы</em></h1>' + phone('15-schedule-week-light.png') + chalk('stroke', 70, 330, 520, -3),
    'light': themes('светлая', '15-schedule-week-light.png'),
    'dark': themes('тёмная', '24-attendance-dark.png'),
    'black': themes('чёрная', '26-schedule-week-black.png'),
    's1': secret('баллы по предметам', '11-scores-sheet-light.png'),
    's2': secret('календарь пропусков', '24-attendance-dark.png'),
    's3': secret('стрик посещений', '08-streak-black.png'),
    's4': secret('рейтинг курса', '07-leaderboard-light.png'),
    'soon': '<h1 class="mid huge" style="top:500px"><em>скоро</em></h1><p class="sub mid" style="top:1030px">выход новой версии МойКЦТ</p>'
            '<img class="icon small" src="../assets/app-icon.png" alt="">' + chalk('oval2', 0, 230, 1040, -6) + chalk('star', 860, 160, 150, 10),
}

# (сцена, секунды показа, секунды перехода в неё, тип перехода)
TIMELINE = [('intro', 1.4, 0, ''), ('login', 1.8, .35, 'fade'), ('schedule', 2.0, .35, 'fade'),
            ('light', .9, .25, 'fade'), ('dark', .9, .25, 'fade'), ('black', 1.1, .25, 'fade'),
            ('s1', .45, 0, 'glitch'), ('s2', .45, 0, 'glitch'), ('s3', .45, 0, 'glitch'), ('s4', .55, 0, 'glitch'),
            ('soon', 2.6, .4, 'fade')]
FPS = 25


def render():
    FR.mkdir(parents=True, exist_ok=True)
    for name, body in SCENES.items():
        html = L / f'teaser-{name}.html'
        html.write_text(f'''<!doctype html><html lang="ru"><head><meta charset="utf-8"><title>Тизер {name}</title>
<link rel="stylesheet" href="shared.css"><link rel="stylesheet" href="black.css"><link rel="stylesheet" href="teaser.css"></head>
<body><main class="canvas black teaser" style="--w:{W}px;--h:{H}px"><div class="glow" style="left:40px;top:300px"></div>{body}</main></body></html>''')
        subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=1',
                        '--allow-file-access-from-files', '--virtual-time-budget=3000', f'--window-size={W},{H}',
                        f'--screenshot={FR / name}.png', f'file://{html}'], stderr=subprocess.DEVNULL, check=True)


def glitch(im, k):
    """Сдвиг каналов и рваные полосы - кадр на стыке засекреченных экранов."""
    r, g, b = im.split()
    im = Image.merge('RGB', (ImageChops.offset(r, 18 * k, 0), g, ImageChops.offset(b, -18 * k, 0)))
    for y in range(80 + 90 * k, H, 260):
        band = im.crop((0, y, W, y + 40))
        im.paste(ImageChops.offset(band, 60 * (1 if y % 3 else -1), 0), (0, y))
    return im


def frames():
    shots = {n: Image.open(FR / f'{n}.png').convert('RGB') for n, *_ in TIMELINE}
    out, prev = [], None
    for name, hold, trans, kind in TIMELINE:
        cur = shots[name]
        if kind == 'fade':
            n = int(trans * FPS)
            out += [Image.blend(prev, cur, (i + 1) / (n + 1)) for i in range(n)]
        elif kind == 'glitch':
            out += [glitch(prev, 2), glitch(cur, 1)]
        n = int(hold * FPS)
        for i in range(n):  # медленный наезд камеры
            z = 1 + .03 * i / n
            w, h = int(W / z), int(H / z)
            out.append(cur.crop(((W - w) // 2, (H - h) // 2, (W + w) // 2, (H + h) // 2)).resize((W, H), Image.LANCZOS))
        prev = cur
    return out


if __name__ == '__main__':
    render()
    seq = frames()
    for i, f in enumerate(seq):
        f.save(FR / f'f{i:04d}.png')
    src = ['-framerate', str(FPS), '-i', str(FR / 'f%04d.png')]
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', *src, '-vf', 'scale=720:-2', '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '18',
                    '-movflags', '+faststart', str(OUT / 'teaser.mp4')], check=True)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', *src, '-vf',
                    'fps=15,scale=480:-2:flags=lanczos,split[a][b];[a]palettegen=max_colors=160:stats_mode=diff[p];[b][p]paletteuse=dither=sierra2_4a',
                    str(OUT / 'teaser.gif')], check=True)
    print(len(seq), 'кадров,', round(len(seq) / FPS, 1), 'с')
