$execute unless entity @e[type=minecraft:item_display,tag=$(art)_letzte] run return run tellraw @s {"text":"Nichts zum Zurücknehmen.","color":"red"}
$execute at @e[type=minecraft:item_display,tag=$(art)_letzte,tag=!lichterkette_baum,limit=1] run tp @e[type=minecraft:marker,tag=$(art)_anfang] ~ ~ ~
$kill @e[type=minecraft:item_display,tag=$(art)_letzte]
$tellraw @s {"text":"Letzte $(name) entfernt.","color":"gold"}
