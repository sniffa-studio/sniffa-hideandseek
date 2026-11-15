execute as @e[type=minecraft:item_display,tag=deko_dreh] run function sniffa:intern/deko/drehen
execute as @e[type=minecraft:item_display,tag=deko_dreh_z] run function sniffa:intern/deko/drehen_z
execute as @e[type=minecraft:item_display,tag=deko_blitz] run function sniffa:intern/deko/blitzen
execute as @e[type=minecraft:item_display,tag=deko_band] run data merge entity @s {start_interpolation:0,interpolation_duration:0,transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[1f,1f,1f]}}
function sniffa:intern/deko/hub
