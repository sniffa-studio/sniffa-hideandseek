execute positioned ~ ~0.525 ~ run function sniffa:intern/deko/paket/neu
scoreboard players add #k sniffa_deko 1
execute if score #k sniffa_deko <= #start sniffa_deko positioned ^ ^ ^1 run function sniffa:intern/deko/paket/reihe
