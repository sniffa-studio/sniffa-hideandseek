scoreboard objectives add sniffa_kette dummy
scoreboard players set #farbe sniffa_kette 4
execute as @e[type=minecraft:item_display,tag=lichterkette_letzte] run data modify entity @s item.components."minecraft:custom_model_data".floats[4] set value 4f
tellraw @s {"text":"Tannengirlande für die letzte und alle neuen Lichterketten.","color":"gold"}
