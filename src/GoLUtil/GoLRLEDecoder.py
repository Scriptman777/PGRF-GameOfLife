
from PIL import Image

#x
columns = 6154
#y
lines = 6154



img = Image.new('RGB', (columns, lines))

def addCell(x,y):
    img.putpixel((x, y), (255, 255, 255))


file = open('rle.txt',mode='r')
RLEstr = file.read()
file.close()


curX = 0
curY = 0
curRLE = 0
doRLE = False

RLEstr = RLEstr.replace("\n", "")

for index in range(0, len(RLEstr)):
    print(str(curX) + " - " + str(curY))
    char = RLEstr[index]
    print(char)

    if char.isnumeric():
        if not curRLE == 0:
            curRLE = int(str(curRLE) + str(char))
        else:
            curRLE = int(char)
        doRLE = True
        continue

    if doRLE:
        for j in range(0,curRLE):
            if char == 'o':
                addCell(curX, curY)
                curX = curX + 1
            elif char == 'b':
                curX = curX + 1
            elif char == '$':
                curX = 0
                curY = curY + 1

        doRLE = False
        curRLE = 0
        continue


    # Put live cell
    elif char == 'o':
        addCell(curX, curY)
    # End line
    elif char == '$':
        curX = 0
        curY = curY + 1
        continue
    # Move one
    curX = curX + 1



img.save('Pil_spinMetafied.png')
