scoreboard players set #lang sniffa_deko 0
scoreboard players set #maschine sniffa_deko 0
execute positioned ^ ^ ^1 run function sniffa:intern/deko/paket/suchen
execute if score #lang sniffa_deko matches 0 run return 0
scoreboard players operation #start sniffa_deko = #lang sniffa_deko
execute if score #maschine sniffa_deko matches 1 run scoreboard players add #start sniffa_deko 3
execute store result entity @s data.start int 1 run scoreboard players get #start sniffa_deko
execute store result score #dreh sniffa_deko run data get entity @s data.yaw
scoreboard players add #dreh sniffa_deko 180
execute store result storage sniffa:deko paket.dreh int 1 run scoreboard players get #dreh sniffa_deko
scoreboard players set #k sniffa_deko 1
execute positioned ^ ^ ^1 run function sniffa:intern/deko/paket/reihe
scoreboard players set #k sniffa_deko 0
execute positioned ^ ^0.15625 ^ run function sniffa:intern/deko/paket/neu
scoreboard players set #k sniffa_deko -1
execute positioned ^ ^0.15625 ^-0.96875 run function sniffa:intern/deko/paket/neu
