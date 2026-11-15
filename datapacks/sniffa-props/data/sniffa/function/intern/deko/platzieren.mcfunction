kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
function sniffa:intern/laden
$data modify storage sniffa:deko args set value {art:"$(art)",name:"$(name)"}
execute store result score #dreh sniffa_deko run data get entity @s Rotation[0]
scoreboard players add #dreh sniffa_deko 405
scoreboard players operation #dreh sniffa_deko /= #90 sniffa_deko
scoreboard players operation #dreh sniffa_deko %= #4 sniffa_deko
scoreboard players operation #dreh sniffa_deko *= #90 sniffa_deko
scoreboard players add #dreh sniffa_deko 180
scoreboard players operation #dreh sniffa_deko %= #360 sniffa_deko
execute store result storage sniffa:deko args.yaw int 1 run scoreboard players get #dreh sniffa_deko
execute store result storage sniffa:deko args.nr int 1 run random value 0..5
function sniffa:intern/deko/farbe_name
function sniffa:intern/deko/anker with storage sniffa:deko args
