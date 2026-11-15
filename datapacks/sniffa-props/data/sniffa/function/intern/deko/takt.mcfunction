scoreboard players add #takt sniffa_deko 1
execute if score #takt sniffa_deko matches 40.. run scoreboard players set #takt sniffa_deko 0
execute if score #takt sniffa_deko matches 0 run function sniffa:intern/deko/takt_voll
execute if score #takt sniffa_deko matches 0 as @e[type=minecraft:item_display,tag=deko_paket] at @s run function sniffa:intern/deko/paket/schritt
execute if score #takt sniffa_deko matches 20..28 as @e[type=minecraft:item_display,tag=deko_paket,scores={sniffa_deko_weg=0}] at @s run function sniffa:intern/deko/paket/fallen
execute if score #takt sniffa_deko matches 39 as @e[type=minecraft:item_display,tag=deko_paket] run function sniffa:intern/deko/paket/dauer
execute if score #takt sniffa_deko matches 10 as @e[type=minecraft:item_display,tag=deko_blitz] run function sniffa:intern/deko/blitzen
execute if score #takt sniffa_deko matches 20 as @e[type=minecraft:item_display,tag=deko_blitz] run function sniffa:intern/deko/blitzen
execute if score #takt sniffa_deko matches 20 run function sniffa:intern/deko/hub
execute if score #takt sniffa_deko matches 30 as @e[type=minecraft:item_display,tag=deko_blitz] run function sniffa:intern/deko/blitzen
