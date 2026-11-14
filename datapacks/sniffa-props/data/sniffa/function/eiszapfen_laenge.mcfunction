$execute as @e[type=minecraft:item_display,tag=eiszapfen_letzte] run data modify entity @s item.components."minecraft:custom_model_data".floats[3] set value $(bloecke)f
$tellraw @s {"text":"Längste Eiszapfen der letzten Reihe: $(bloecke) Blöcke (0 = Standard 1,2).","color":"gold"}
