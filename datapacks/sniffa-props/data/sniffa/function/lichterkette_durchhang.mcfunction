execute if entity @e[type=minecraft:item_display,tag=lichterkette_letzte,tag=lichterkette_baum] run return run tellraw @s {"text":"Am Baum gibt es keinen Durchhang. Nimm lichterkette_runden.","color":"red"}
$execute as @e[type=minecraft:item_display,tag=lichterkette_letzte] run data modify entity @s item.components."minecraft:custom_model_data".floats[3] set value $(bloecke)f
$tellraw @s {"text":"Durchhang der letzten Lichterkette: $(bloecke) Blöcke (0 = automatisch).","color":"gold"}
