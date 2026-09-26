"""Compose 0.5.5 review sheets from actual runtime renders, including Hilo motion."""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

output, old_snapshots, new_snapshots = map(Path, sys.argv[1:])
font = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 25)
small = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 19)
background = '#252b32'


def canvas(size, title):
    image = Image.new('RGB', size, background)
    draw = ImageDraw.Draw(image)
    draw.text((24, 14), title, font=font, fill='white')
    footer = ('服务端实体几何预览，非游戏截图。' if size[0] < 1000 else
              '实际服务端实体几何渲染；方块采用代表色，不是 Minecraft 客户端截图。')
    draw.text((24, size[1]-32), footer,
              font=small, fill='#bdc7d2')
    return image, draw


def paste(image, path, x, y, size):
    preview = Image.open(path).convert('RGB')
    preview.thumbnail(size, Image.Resampling.LANCZOS)
    image.paste(preview, (x+(size[0]-preview.width)//2, y+(size[1]-preview.height)//2))


image, draw = canvas((1800, 1360), '0.5.5 原版机器 | 纯色按钮与桌面、紧凑龙塔、移除悬浮说明')
games = [('blackjack-dealt', 'Blackjack · 纯色桌面和统一按钮'),
         ('slots', 'Slots · 移除按钮下方长方体'),
         ('dragon_tower', '龙塔 · 深色圆角格子 / 红色立式机身'),
         ('plinko', 'Plinko · 平整按钮与连续圆角'),
         ('keno-selected', 'Keno · 橙色混凝土选中格'),
         ('hilo', 'Hilo · 指针往返后停到实际结果')]
for i, (game, label) in enumerate(games):
    x, y = i%3*600, 58+i//3*630
    paste(image, output/'previews'/f'{game}-vanilla.png', x, y, (600, 595))
    draw.text((x+18, y+598), label, font=small, fill='white')
image.save(output/'finish-overview.png')

image, draw = canvas((1600, 1020), '同相机对照 | 每组左：0.5.4，右：0.5.5')
pairs = [('details/cabinet_button_play', '通用按钮：平整键面与圆角', 'after'),
         ('previews/slots', 'Slots：删除底托并修复拉宽后的圆角', 'vanilla'),
         ('previews/blackjack-dealt', 'Blackjack：纯色桌面与按钮', 'vanilla'),
         ('previews/dragon_tower', '龙塔：去除桌子、降低格子和按钮', 'vanilla')]
for i, (stem, label, after) in enumerate(pairs):
    x, y = i%2*800, 54+i//2*465
    for side, suffix in enumerate(('before', after)):
        paste(image, output/f'{stem}-{suffix}.png', x+side*400, y, (400, 400))
        draw.text((x+side*400+16, y+403), '0.5.4' if side==0 else '0.5.5', font=small, fill='#bdc7d2')
    draw.text((x+16, y+431), label, font=small, fill='white')
image.save(output/'finish-comparison.png')

frames, numbers = [], []
for path in sorted((output/'hilo-motion').glob('*-vanilla.png')):
    number = int(path.name.split('-')[0])
    frame, draw = canvas((640, 710), 'Hilo · 向右 → 返回 → 揭晓')
    paste(frame, path, 0, 50, (640, 620))
    frames.append(frame)
    numbers.append(number)
if frames:
    durations = [(b-a)*50 for a,b in zip(numbers, numbers[1:])] + [1400]
    frames[0].save(output/'hilo-motion.gif', save_all=True, append_images=frames[1:],
                   duration=durations, loop=0, optimize=False)

counts = {}
for path in sorted(new_snapshots.glob('*.json')):
    def count(file):
        if not file.exists(): return None
        return sum(e['kind'].endswith('BlockDisplay') and e.get('visible', True)
                   for e in json.loads(file.read_text(encoding='utf-8')))
    counts[path.stem] = {'before': count(old_snapshots/path.name), 'after': count(path)}
(output/'evidence').mkdir(exist_ok=True)
(output/'evidence/entity-counts.json').write_text(json.dumps(counts, indent=2)+'\n', encoding='utf-8')
