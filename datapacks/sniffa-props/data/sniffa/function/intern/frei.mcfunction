scoreboard players set #schritte sniffa_kette 400
execute at @e[type=minecraft:marker,tag=lichterkette_anfang,limit=1] facing entity @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] feet positioned ^ ^ ^0.3 run return run function sniffa:intern/frei_schritt
return 1
