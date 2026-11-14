kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
execute as @e[type=minecraft:item_display,tag=scheinwerfer_kopf,distance=..4,sort=nearest,limit=1] at @s facing entity @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] feet run tp @s ~ ~ ~ ~ ~
execute as @e[type=minecraft:item_display,tag=scheinwerfer,tag=!scheinwerfer_kopf,distance=..4,sort=nearest,limit=1] at @s facing entity @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] feet run tp @s ~ ~ ~ ~ 0
kill @e[type=minecraft:marker,tag=sniffa_ziel]
