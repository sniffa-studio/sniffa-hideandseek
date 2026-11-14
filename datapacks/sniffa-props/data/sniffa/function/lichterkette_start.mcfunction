function sniffa:intern/punkt
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
kill @e[type=minecraft:marker,tag=lichterkette_anfang]
tag @e[type=minecraft:marker,tag=sniffa_ziel] add lichterkette_anfang
tag @e[type=minecraft:marker,tag=lichterkette_anfang] remove sniffa_ziel
tellraw @s [{"text":"Anfang gesetzt. ","color":"gold"},{"text":"Jetzt das Ende anschauen und ","color":"gray"},{"text":"/function sniffa:lichterkette_ende","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:lichterkette_ende"}}]
