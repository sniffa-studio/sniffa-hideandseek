scoreboard players operation #vid sniffa_deko = @s sniffa_deko
execute as @e[type=minecraft:item_display,tag=verbund_teil,distance=..0.3] if score @s sniffa_deko = #vid sniffa_deko run kill @s
$execute store success score #vn sniffa_deko positioned ~ ~ ~-1 if entity @e[type=minecraft:marker,tag=sniffa_deko,tag=!verbund_weg,distance=..0.3,nbt={data:{art:"$(art)"}}]
$execute store success score #vs sniffa_deko positioned ~ ~ ~1 if entity @e[type=minecraft:marker,tag=sniffa_deko,tag=!verbund_weg,distance=..0.3,nbt={data:{art:"$(art)"}}]
$execute store success score #ve sniffa_deko positioned ~1 ~ ~ if entity @e[type=minecraft:marker,tag=sniffa_deko,tag=!verbund_weg,distance=..0.3,nbt={data:{art:"$(art)"}}]
$execute store success score #vw sniffa_deko positioned ~-1 ~ ~ if entity @e[type=minecraft:marker,tag=sniffa_deko,tag=!verbund_weg,distance=..0.3,nbt={data:{art:"$(art)"}}]
scoreboard players set #vg sniffa_deko 0
execute if score #vn sniffa_deko matches 1 if score #vs sniffa_deko matches 1 if score #ve sniffa_deko matches 0 if score #vw sniffa_deko matches 0 run scoreboard players set #vg sniffa_deko 1
execute if score #ve sniffa_deko matches 1 if score #vw sniffa_deko matches 1 if score #vn sniffa_deko matches 0 if score #vs sniffa_deko matches 0 run scoreboard players set #vg sniffa_deko 2
$execute if score #vg sniffa_deko matches 1 run function sniffa:intern/deko/verbund/teil {art:"$(art)",teil:"gerade",dreh:0}
$execute if score #vg sniffa_deko matches 2 run function sniffa:intern/deko/verbund/teil {art:"$(art)",teil:"gerade",dreh:90}
$execute if score #vg sniffa_deko matches 0 run function sniffa:intern/deko/verbund/teil {art:"$(art)",teil:"pfosten",dreh:0}
$execute if score #vg sniffa_deko matches 0 if score #vn sniffa_deko matches 1 run function sniffa:intern/deko/verbund/teil {art:"$(art)",teil:"arm",dreh:180}
$execute if score #vg sniffa_deko matches 0 if score #vs sniffa_deko matches 1 run function sniffa:intern/deko/verbund/teil {art:"$(art)",teil:"arm",dreh:0}
$execute if score #vg sniffa_deko matches 0 if score #ve sniffa_deko matches 1 run function sniffa:intern/deko/verbund/teil {art:"$(art)",teil:"arm",dreh:270}
$execute if score #vg sniffa_deko matches 0 if score #vw sniffa_deko matches 1 run function sniffa:intern/deko/verbund/teil {art:"$(art)",teil:"arm",dreh:90}
scoreboard players operation @e[type=minecraft:item_display,tag=verbund_neu] sniffa_deko = #vid sniffa_deko
tag @e[type=minecraft:item_display,tag=verbund_neu] remove verbund_neu
