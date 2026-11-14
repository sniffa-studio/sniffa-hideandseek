execute unless entity @e[type=minecraft:item_display,tag=lichterkette_letzte] run return run tellraw @s {"text":"Keine letzte Lichterkette zum Zurücknehmen.","color":"red"}
execute at @e[type=minecraft:item_display,tag=lichterkette_letzte,tag=!lichterkette_baum,limit=1] run tp @e[type=minecraft:marker,tag=lichterkette_anfang] ~ ~ ~
kill @e[type=minecraft:item_display,tag=lichterkette_letzte]
tellraw @s {"text":"Letzte Lichterkette entfernt.","color":"gold"}
