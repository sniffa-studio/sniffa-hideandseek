execute if entity @e[type=minecraft:marker,tag=sniffa_ziel,distance=..0.3] run return 1
scoreboard players remove #schritte sniffa_kette 1
execute if score #schritte sniffa_kette matches ..0 run return 1
execute unless block ~ ~ ~ #sniffa:durchlaessig run return 0
execute positioned ^ ^ ^0.2 run return run function sniffa:intern/frei_schritt
