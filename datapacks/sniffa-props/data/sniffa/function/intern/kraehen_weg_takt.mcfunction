scoreboard players add @s sniffa_kraehe 1
execute if score @s sniffa_kraehe matches ..1400 run return 0
execute if entity @a[distance=..12,gamemode=!spectator,gamemode=!creative] run return 0
tag @s remove kraehen_weg
data modify entity @s item.components."minecraft:custom_model_data".floats[1] set value 0f
function sniffa:intern/kraehen_uhr
