function sniffa:intern/deko/suchen
execute as @e[type=minecraft:marker,tag=deko_gewaehlt] unless function sniffa:intern/deko/ist_geschenk run tag @s add deko_kein_geschenk
execute if entity @e[type=minecraft:marker,tag=deko_kein_geschenk] run tellraw @s {"text":"Umfärben geht nur bei Geschenken.","color":"red"}
execute as @e[type=minecraft:marker,tag=deko_gewaehlt,tag=!deko_kein_geschenk] at @s rotated as @s run function sniffa:intern/deko/umfaerben
tag @e[type=minecraft:marker,tag=deko_gewaehlt] remove deko_kein_geschenk
tag @e[type=minecraft:marker,tag=deko_gewaehlt] remove deko_gewaehlt
