"""Compose all-machine and wheel review sheets from actual runtime renders.

Usage: python make_feedback_gallery.py <render-folder> <output-folder>
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFont

source, output = map(Path, sys.argv[1:])
output.mkdir(parents=True, exist_ok=True)
font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 26)
small = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 19)
games = ['blackjack', 'mines', 'crash', 'plinko', 'slots', 'duck_race',
         'wheel_of_fortune', 'money_wheel', 'penguin_cross', 'keno', 'hilo', 'dragon_tower']


def sheet(names, labels, destination, title):
    result = Image.new('RGB', (1440, 1570), '#252b32')
    draw = ImageDraw.Draw(result)
    draw.text((22, 12), title, font=font, fill='white')
    draw.text((22, 48), 'Server Display geometry; representative colors, not Minecraft screenshots.',
              font=small, fill='#bdc7d2')
    for i, (name, label) in enumerate(zip(names, labels)):
        x, y = (i % 2) * 720, 84 + (i // 2) * 735
        preview = Image.open(source / name).convert('RGB')
        preview.thumbnail((700, 700), Image.Resampling.LANCZOS)
        result.paste(preview, (x + 10, y))
        draw.text((x + 20, y + 704), label, font=small, fill='white')
    result.save(destination)


for page in range(3):
    group = games[page*4:page*4+4]
    sheet([f'{game}-result-vanilla.png' for game in group],
          [game.replace('_', ' ').upper() for game in group],
          output / f'machines-{page+1:02}.png', '3DCasinoGames | physical feedback on all 12 machines')

sheet(['wheel_of_fortune-result-vanilla.png', 'money_wheel-result-vanilla.png',
       'wheel_of_fortune-result-vanilla-rear.png', 'money_wheel-result-vanilla-rear.png'],
      ['FORTUNE / FRONT', 'MONEY WHEEL / FRONT', 'FORTUNE / REAR', 'MONEY WHEEL / REAR'],
      output / 'feedback-wheels.png', '3DCasinoGames | reference-inspired vanilla wheels')
