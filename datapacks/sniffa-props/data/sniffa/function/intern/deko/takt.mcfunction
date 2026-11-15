scoreboard players add #takt sniffa_deko 1
execute if score #takt sniffa_deko matches ..39 run return 0
scoreboard players set #takt sniffa_deko 0
execute as @e[type=minecraft:item_display,tag=deko_dreh] run function sniffa:intern/deko/drehen
execute as @e[type=minecraft:item_display,tag=deko_dreh_z] run function sniffa:intern/deko/drehen_z
