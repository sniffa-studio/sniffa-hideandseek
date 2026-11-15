scoreboard players operation #m sniffa_deko = #k sniffa_deko
scoreboard players add #m sniffa_deko 60
scoreboard players operation #m sniffa_deko %= #6 sniffa_deko
scoreboard players add #m sniffa_deko 1
execute store result storage sniffa:deko paket.nr int 1 run scoreboard players get #m sniffa_deko
function sniffa:intern/deko/paket/neu_an with storage sniffa:deko paket
scoreboard players operation @e[type=minecraft:item_display,tag=paket_neu] sniffa_deko_weg = #k sniffa_deko
tag @e[type=minecraft:item_display,tag=paket_neu] remove paket_neu
