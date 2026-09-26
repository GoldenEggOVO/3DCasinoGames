"""Compose review sheets and orthographic card geometry directly from the shipped JAR."""
import json
import sys
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

jar, output = Path(sys.argv[1]), Path(sys.argv[2])
with zipfile.ZipFile(jar) as archive:
    models = json.loads(archive.read('vanilla-models.json'))
palette = json.loads((Path(__file__).with_name('palette.json')).read_text())
font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 22)
small = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 16)


def card(index, width=240):
    height = round(width*4/3)
    im = Image.new('RGB', (width+12, height+12), '#252b32')
    draw = ImageDraw.Draw(im)
    for part in models[f'card_{index}']['boxes']:
        a, b = part['from'], part['to']
        draw.rectangle((6+(a[0]+2)*width/4, 6+(8/3-b[1])*height/(16/3),
                        6+(b[0]+2)*width/4, 6+(8/3-a[1])*height/(16/3)),
                       fill=palette[part['material']])
    return im


indices = [0, 13, 26, 47, 51, 52]
labels = ['A - SPADES', 'A - HEARTS', 'A - CLUBS', '9 - DIAMONDS', 'K - DIAMONDS', 'BACK']
im = Image.new('RGB', (6*264+24, 410), '#252b32'); draw = ImageDraw.Draw(im)
draw.text((20, 12), 'VANILLA CARDS | actual block geometry from the JAR', font=font, fill='white')
for i, (index, label) in enumerate(zip(indices, labels)):
    im.paste(card(index), (18+i*264, 48))
    draw.text((24+i*264, 385), label, font=small, fill='white')
im.save(output/'cards-preview.png')

im = Image.new('RGB', (13*132+24, 4*204+50), '#252b32'); draw = ImageDraw.Draw(im)
draw.text((16, 12), 'ALL 52 FACES | lower-right corners rotated 180 degrees', font=font, fill='white')
for index in range(52):
    im.paste(card(index, 114), (12+index%13*132, 50+index//13*204))
im.save(output/'cards-52.png')

games = ['blackjack-dealt', 'crash', 'plinko', 'slots', 'duck_race', 'hilo',
         'money_wheel', 'wheel_of_fortune', 'mines', 'dragon_tower', 'keno', 'penguin_cross']
modes = ('before', 'vanilla') if (output/'previews/slots-before.png').exists() else ('resource', 'vanilla')
for page in range(3):
    im = Image.new('RGB', (1600, 970), '#252b32'); draw = ImageDraw.Draw(im)
    draw.text((24, 12), modes[0].upper()+'  |  VANILLA runtime geometry', font=font, fill='white')
    for cell, game in enumerate(games[page*4:page*4+4]):
        x, y = cell%2*800, 52+cell//2*450
        for side, mode in enumerate(modes):
            image = Image.open(output/'previews'/f'{game}-{mode}.png')
            image.thumbnail((400,800))
            im.paste(image, (x+side*400, y))
            draw.text((x+side*400+12, y+405), mode.upper(), font=small, fill='#b7bec9')
        draw.text((x+12, y+426), game.replace('_',' ').upper(), font=small, fill='white')
    # Compact pairs; two rows and two machine pairs per page.
    im.save(output/f'machines-comparison-{page+1}.png')

# A larger single comparison makes the card table and control placement inspectable.
im = Image.new('RGB', (1800, 960), '#252b32'); draw = ImageDraw.Draw(im)
for i, mode in enumerate(modes):
    im.paste(Image.open(output/'previews'/f'blackjack-dealt-{mode}.png'), (i*900, 50))
    draw.text((i*900+24, 14), 'BLACKJACK - '+mode.upper(), font=font, fill='white')
im.save(output/'blackjack-comparison.png')
