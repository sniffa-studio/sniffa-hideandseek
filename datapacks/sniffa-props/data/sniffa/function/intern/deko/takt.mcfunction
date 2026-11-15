scoreboard players add #takt sniffa_deko 1
execute if score #takt sniffa_deko matches 40.. run scoreboard players set #takt sniffa_deko 0
execute if score #takt sniffa_deko matches 0 run function sniffa:intern/deko/takt_voll
execute if score #takt sniffa_deko matches 1 as @e[type=minecraft:item_display,tag=deko_band] run data merge entity @s {start_interpolation:0,interpolation_duration:39,transformation:{translation:[-1f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[1f,1f,1f]}}
execute if score #takt sniffa_deko matches 10 as @e[type=minecraft:item_display,tag=deko_blitz] run function sniffa:intern/deko/blitzen
execute if score #takt sniffa_deko matches 20 as @e[type=minecraft:item_display,tag=deko_blitz] run function sniffa:intern/deko/blitzen
execute if score #takt sniffa_deko matches 20 run function sniffa:intern/deko/hub
execute if score #takt sniffa_deko matches 30 as @e[type=minecraft:item_display,tag=deko_blitz] run function sniffa:intern/deko/blitzen
