execute if score @s sniffa_deko_weg matches -1 run return run function sniffa:intern/deko/paket/zurueck
execute if score @s sniffa_deko_weg matches 2.. run tp @s ^ ^ ^1
execute if score @s sniffa_deko_weg matches 1 run tp @s ^ ^ ^0.53125
execute if score @s sniffa_deko_weg matches 0 run tp @s ^ ^ ^0.96875
scoreboard players remove @s sniffa_deko_weg 1
