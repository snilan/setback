import subprocess
import re

pics = subprocess.check_output("ls *.png", shell=True).split("\n")
suits = ["clubs", "spades", "hearts", "diamonds"]

for p in pics:
    match = re.match(r"(\d+)\.png", p)
    if match:
        num = int(match.group(1))
        suit =  suits[(num - 1) % 4]
        number = 14 - ((num - 1) / 4)
        subprocess.call(["mv", p, suit + str(number) + ".png"])

