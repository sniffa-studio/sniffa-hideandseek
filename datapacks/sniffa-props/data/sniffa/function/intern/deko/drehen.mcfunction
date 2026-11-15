scoreboard players add @s sniffa_deko_dreh 1
execute if score @s sniffa_deko_dreh matches 4.. run scoreboard players set @s sniffa_deko_dreh 0
execute if score @s sniffa_deko_dreh matches 0 run data merge entity @s {start_interpolation:0,interpolation_duration:40,transformation:{left_rotation:{angle:0f,axis:[0f,1f,0f]},right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
execute if score @s sniffa_deko_dreh matches 1 run data merge entity @s {start_interpolation:0,interpolation_duration:40,transformation:{left_rotation:{angle:1.5707964f,axis:[0f,1f,0f]},right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
execute if score @s sniffa_deko_dreh matches 2 run data merge entity @s {start_interpolation:0,interpolation_duration:40,transformation:{left_rotation:{angle:3.1415927f,axis:[0f,1f,0f]},right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
execute if score @s sniffa_deko_dreh matches 3 run data merge entity @s {start_interpolation:0,interpolation_duration:40,transformation:{left_rotation:{angle:4.712389f,axis:[0f,1f,0f]},right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
