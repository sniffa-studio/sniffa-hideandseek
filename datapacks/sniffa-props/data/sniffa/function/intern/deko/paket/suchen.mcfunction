execute store success score #band sniffa_deko if entity @e[type=minecraft:marker,tag=sniffa_deko,distance=..1.6,nbt={data:{art:"fliessband"}}]
execute if score #band sniffa_deko matches 0 store success score #maschine sniffa_deko positioned ^ ^ ^2 if entity @e[type=minecraft:marker,tag=sniffa_deko,distance=..0.6,nbt={data:{art:"riesen_verpackung"}}]
execute if score #band sniffa_deko matches 0 run return 0
scoreboard players add #lang sniffa_deko 1
execute positioned ^ ^ ^1 run function sniffa:intern/deko/paket/suchen
