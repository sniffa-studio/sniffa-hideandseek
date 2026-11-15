scoreboard players operation #id sniffa_deko = @s sniffa_deko
execute as @e[type=minecraft:marker,tag=sniffa_deko] if score @s sniffa_deko = #id sniffa_deko run tag @s add paket_kiste
execute if entity @e[type=minecraft:marker,tag=paket_kiste] run function sniffa:intern/deko/paket/zurueck_an with entity @e[type=minecraft:marker,tag=paket_kiste,limit=1] data
tag @e[type=minecraft:marker,tag=paket_kiste] remove paket_kiste
