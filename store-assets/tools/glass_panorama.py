# Панорама фона App Store: стеклянные ленты на фиолетовом, 6 кадров по 1290 px.
# python3 tools/glass_panorama.py > assets/glass-panorama.svg
import math
W, H = 1290 * 6, 2796

def path(points):
    # Catmull-Rom -> кубические Безье, гладкая кривая через точки
    d = f"M{points[0][0]:.0f} {points[0][1]:.0f}"
    for i in range(len(points) - 1):
        p0 = points[max(i - 1, 0)]; p1 = points[i]; p2 = points[i + 1]; p3 = points[min(i + 2, len(points) - 1)]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += f" C{c1[0]:.0f} {c1[1]:.0f} {c2[0]:.0f} {c2[1]:.0f} {p2[0]:.0f} {p2[1]:.0f}"
    return d

def wave(y0, amp, period, phase, step=430):
    return [(x, y0 + amp * math.sin(2 * math.pi * x / period + phase) + 0.45 * amp * math.sin(2 * math.pi * x / (period * 0.47) + phase * 1.7))
            for x in range(-600, W + 601, step)]

def ribbon(pts, width, twist):
    # Тело ленты, два стеклянных края со смещением (даёт эффект закрученной ленты) и блики
    body = path(pts)
    top = path([(x, y - width / 2 + twist * math.sin(x / 900)) for x, y in pts])
    bot = path([(x, y + width / 2 - twist * math.sin(x / 900)) for x, y in pts])
    return f'''
<g>
  <path d="{body}" stroke="url(#body)" stroke-width="{width}" fill="none" opacity=".55" filter="url(#soft)"/>
  <path d="{top}" stroke="url(#edge)" stroke-width="7" fill="none" filter="url(#glow)"/>
  <path d="{bot}" stroke="url(#edge)" stroke-width="4" fill="none" opacity=".7" filter="url(#glow)"/>
  <path d="{top}" stroke="#fff" stroke-width="10" fill="none" stroke-dasharray="260 1400 120 2200" stroke-linecap="round" opacity=".95" filter="url(#spec)"/>
  <path d="{body}" stroke="#E8E5FF" stroke-width="{width*0.18:.0f}" fill="none" opacity=".16" filter="url(#blur)"/>
</g>'''

ribbons = [
    ribbon(wave(1250, 260, 3100, 0.6), 230, 70),
    ribbon(wave(2150, 300, 3900, 2.4), 300, 110),
    ribbon(wave(620, 170, 2600, 4.1), 150, 50),
]
print(f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <linearGradient id="base" x2="0" y2="1"><stop stop-color="#7464EE"/><stop offset=".55" stop-color="#5546D2"/><stop offset="1" stop-color="#2E2390"/></linearGradient>
  <radialGradient id="light"><stop stop-color="#C3B6FF" stop-opacity=".45"/><stop offset="1" stop-color="#C3B6FF" stop-opacity="0"/></radialGradient>
  <radialGradient id="deep"><stop stop-color="#A347E0" stop-opacity=".35"/><stop offset="1" stop-color="#A347E0" stop-opacity="0"/></radialGradient>
  <linearGradient id="body" gradientUnits="userSpaceOnUse" x2="{W}"><stop stop-color="#ffffff" stop-opacity=".22"/><stop offset=".3" stop-color="#C3B6FF" stop-opacity=".1"/><stop offset=".6" stop-color="#ffffff" stop-opacity=".26"/><stop offset="1" stop-color="#E8E5FF" stop-opacity=".12"/></linearGradient>
  <linearGradient id="edge" gradientUnits="userSpaceOnUse" x2="{W}"><stop stop-color="#fff"/><stop offset=".18" stop-color="#C3B6FF"/><stop offset=".35" stop-color="#fff"/><stop offset=".5" stop-color="#F0A8FF"/><stop offset=".68" stop-color="#fff"/><stop offset=".85" stop-color="#B0E4FF"/><stop offset="1" stop-color="#fff"/></linearGradient>
  <filter id="soft" x="-5%" y="-50%" width="110%" height="200%"><feGaussianBlur stdDeviation="14"/></filter>
  <filter id="blur" x="-5%" y="-50%" width="110%" height="200%"><feGaussianBlur stdDeviation="28"/></filter>
  <filter id="glow" x="-5%" y="-50%" width="110%" height="200%"><feGaussianBlur stdDeviation="2.5" result="b"/><feGaussianBlur stdDeviation="14" in="SourceGraphic" result="g"/><feMerge><feMergeNode in="g"/><feMergeNode in="b"/></feMerge></filter>
  <filter id="spec" x="-5%" y="-50%" width="110%" height="200%"><feGaussianBlur stdDeviation="5"/></filter>
</defs>
<rect width="{W}" height="{H}" fill="url(#base)"/>
<ellipse cx="900" cy="1500" rx="1300" ry="1100" fill="url(#light)"/>
<ellipse cx="3300" cy="900" rx="1500" ry="1000" fill="url(#deep)"/>
<ellipse cx="5200" cy="1700" rx="1500" ry="1100" fill="url(#light)"/>
<ellipse cx="7300" cy="800" rx="1200" ry="1000" fill="url(#deep)"/>
{''.join(ribbons)}
</svg>''')
