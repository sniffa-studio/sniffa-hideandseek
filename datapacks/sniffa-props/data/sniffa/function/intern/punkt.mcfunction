kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute as @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] run function sniffa:intern/raus
