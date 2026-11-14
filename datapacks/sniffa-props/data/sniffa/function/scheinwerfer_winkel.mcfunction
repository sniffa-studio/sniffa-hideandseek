$execute as @e[type=minecraft:item_display,tag=scheinwerfer_kopf,distance=..4,sort=nearest,limit=1] at @s run tp @s ~ ~ ~ $(richtung) -$(neigung)
$execute as @e[type=minecraft:item_display,tag=scheinwerfer,tag=!scheinwerfer_kopf,distance=..4,sort=nearest,limit=1] at @s run tp @s ~ ~ ~ $(richtung) 0
