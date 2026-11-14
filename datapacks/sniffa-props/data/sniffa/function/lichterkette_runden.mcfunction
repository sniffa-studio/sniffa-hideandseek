execute unless entity @e[type=minecraft:item_display,tag=lichterkette_letzte,tag=lichterkette_baum] run return run tellraw @s {"text":"Die letzte Lichterkette hängt an keinem Baum.","color":"red"}
$execute as @e[type=minecraft:item_display,tag=lichterkette_letzte,tag=lichterkette_baum] run data modify entity @s item.components."minecraft:custom_model_data".floats[2] set value $(runden)f
$tellraw @s {"text":"Die Kette läuft jetzt $(runden) Mal um den Baum (0 = automatisch).","color":"gold"}
