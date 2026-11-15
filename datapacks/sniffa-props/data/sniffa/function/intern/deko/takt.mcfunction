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
execute if score #takt sniffa_deko matches 26 if score #hub sniffa_deko matches 1 run function sniffa:intern/deko/funken
execute if score #takt sniffa_deko matches 0 as @e[type=minecraft:marker,tag=sniffa_deko,nbt={data:{art:"dampfkessel"}}] at @s rotated as @s run particle minecraft:cloud ^-0.625 ^4.1 ^ 0.06 0.05 0.06 0.02 6
