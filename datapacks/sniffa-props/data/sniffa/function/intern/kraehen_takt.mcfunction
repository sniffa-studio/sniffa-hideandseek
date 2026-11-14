execute if entity @s[tag=kraehen_weg] run return run function sniffa:intern/kraehen_weg_takt
execute if entity @a[distance=..8,gamemode=!spectator,gamemode=!creative,predicate=sniffa:rennt] run return run function sniffa:intern/kraehen_auf
execute if entity @a[distance=..3.5,gamemode=!spectator,gamemode=!creative,predicate=sniffa:geht] run return run function sniffa:intern/kraehen_auf
