#!/bin/sh
# Рендер макетов в out/_preview настоящим Chrome. Без аргументов - все appstore-*.
# ./render.sh appstore-01 appstore-05
cd "$(dirname "$0")"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
[ $# -eq 0 ] && set -- $(ls layouts/appstore-*.html | xargs -n1 basename | sed 's/\.html$//')
for name in "$@"; do
  case "$name" in screen-*) size=1080,1920 ;; banner) size=2048,1000 ;; stars-*) size=2160,2700 ;; black) size=7740,2796 ;; *) size=1290,2796 ;; esac
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --allow-file-access-from-files --virtual-time-budget=3000 --window-size=$size \
    --screenshot="$PWD/out/_preview/$name.png" "file://$PWD/layouts/$name.html" 2>/dev/null
  # Лента режется на 6 кадров по 1290
  [ "$name" = black ] && python3 -c "
from PIL import Image
im = Image.open('out/_preview/black.png')
for i in range(6): im.crop((i * 1290, 0, (i + 1) * 1290, 2796)).save(f'out/_preview/black-0{i + 1}.png')"
done
