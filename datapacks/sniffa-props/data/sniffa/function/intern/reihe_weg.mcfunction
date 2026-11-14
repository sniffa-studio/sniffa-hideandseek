kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
$execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] unless entity @e[type=minecraft:item_display,tag=$(art),distance=..6] run return run function sniffa:intern/kette_nicht_da
$execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] run kill @e[type=minecraft:item_display,tag=$(art),distance=..6,sort=nearest,limit=1]
kill @e[type=minecraft:marker,tag=sniffa_ziel]
$tellraw @s {"text":"$(name) entfernt.","color":"gold"}
