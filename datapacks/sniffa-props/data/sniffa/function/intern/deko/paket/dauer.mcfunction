execute if score @s sniffa_deko_weg matches 2.. run data merge entity @s {teleport_duration:40}
execute if score @s sniffa_deko_weg matches 1 run data merge entity @s {teleport_duration:21}
execute if score @s sniffa_deko_weg matches 0 run data merge entity @s {teleport_duration:30}
execute if score @s sniffa_deko_weg matches -1 run data merge entity @s {teleport_duration:0}
