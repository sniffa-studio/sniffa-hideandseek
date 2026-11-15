execute unless data storage sniffa:deko laeufer.radius run data modify storage sniffa:deko laeufer.radius set value 4
$scoreboard players set #lm sniffa_deko $(nr)
scoreboard players operation #lm sniffa_deko %= #4 sniffa_deko
scoreboard players add #lm sniffa_deko 1
execute store result storage sniffa:deko laeufer.modell int 1 run scoreboard players get #lm sniffa_deko
execute store result score #ld sniffa_deko run random value 0..1
execute if score #ld sniffa_deko matches 0 run function sniffa:intern/deko/laeufer/rechts with storage sniffa:deko laeufer
execute if score #ld sniffa_deko matches 1 run function sniffa:intern/deko/laeufer/links with storage sniffa:deko laeufer
data modify storage sniffa:deko laeufer.radius set value 4
