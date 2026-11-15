function sniffa:intern/deko/suchen
execute as @e[type=minecraft:marker,tag=deko_gewaehlt] at @s rotated as @s run function sniffa:intern/deko/abbauen
execute if score #id sniffa_deko matches 1.. run tellraw @s {"text":"Deko entfernt.","color":"gold"}
scoreboard players reset #id sniffa_deko
