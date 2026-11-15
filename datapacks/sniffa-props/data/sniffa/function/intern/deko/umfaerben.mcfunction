scoreboard players operation #id sniffa_deko = @s sniffa_deko
execute as @e[type=minecraft:item_display,tag=sniffa_deko_teil] if score @s sniffa_deko = #id sniffa_deko run kill @s
execute store result score #nr sniffa_deko run data get entity @s data.nr
scoreboard players add #nr sniffa_deko 1
scoreboard players operation #nr sniffa_deko %= #6 sniffa_deko
data modify storage sniffa:deko args set from entity @s data
execute store result storage sniffa:deko args.nr int 1 run scoreboard players get #nr sniffa_deko
function sniffa:intern/deko/farbe_name
data modify entity @s data set from storage sniffa:deko args
function sniffa:intern/deko/teile with entity @s data
