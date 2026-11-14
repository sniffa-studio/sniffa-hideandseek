kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
tag @e[type=minecraft:item_display,tag=lichterkette_letzte] remove lichterkette_letzte
$execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] align xz positioned ~0.5 ~ ~0.5 run summon minecraft:item_display ~ ~ ~ {Tags:["sniffa_prop","lichterkette","lichterkette_baum","lichterkette_letzte"],view_range:4f,item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/lichterkette_baum","minecraft:custom_model_data":{floats:[$(hoehe)f,$(radius)f,0f,0f,0f]}}}}
scoreboard objectives add sniffa_kette dummy
execute as @e[type=minecraft:item_display,tag=lichterkette_letzte,limit=1] store result entity @s item.components."minecraft:custom_model_data".floats[4] float 1 run scoreboard players get #farbe sniffa_kette
kill @e[type=minecraft:marker,tag=sniffa_ziel]
tellraw @s [{"text":"Baum geschmückt. ","color":"gold"},{"text":"Die Kette sucht sich Stamm, Höhe und Breite selbst. Feinschliff: ","color":"gray"},{"text":"lichterkette_runden {runden:6}","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:lichterkette_runden {runden:6}"}},{"text":", ","color":"gray"},{"text":"lichterkette_baum_mass {hoehe:8,radius:4}","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:lichterkette_baum_mass {hoehe:8,radius:4}"}}]
