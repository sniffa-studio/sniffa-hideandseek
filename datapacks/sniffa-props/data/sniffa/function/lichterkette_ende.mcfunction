execute unless entity @e[type=minecraft:marker,tag=lichterkette_anfang] run return run tellraw @s [{"text":"Erst den Anfang setzen: ","color":"red"},{"text":"/function sniffa:lichterkette_start","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:lichterkette_start"}}]
kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
scoreboard objectives add sniffa_kette dummy
execute store result score #ax sniffa_kette run data get entity @e[type=minecraft:marker,tag=lichterkette_anfang,limit=1] Pos[0] 1000
execute store result score #ay sniffa_kette run data get entity @e[type=minecraft:marker,tag=lichterkette_anfang,limit=1] Pos[1] 1000
execute store result score #az sniffa_kette run data get entity @e[type=minecraft:marker,tag=lichterkette_anfang,limit=1] Pos[2] 1000
execute store result score #bx sniffa_kette run data get entity @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] Pos[0] 1000
execute store result score #by sniffa_kette run data get entity @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] Pos[1] 1000
execute store result score #bz sniffa_kette run data get entity @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] Pos[2] 1000
scoreboard players operation #bx sniffa_kette -= #ax sniffa_kette
scoreboard players operation #by sniffa_kette -= #ay sniffa_kette
scoreboard players operation #bz sniffa_kette -= #az sniffa_kette
tag @e[type=minecraft:item_display,tag=lichterkette_letzte] remove lichterkette_letzte
execute at @e[type=minecraft:marker,tag=lichterkette_anfang,limit=1] run summon minecraft:item_display ~ ~ ~ {Tags:["sniffa_prop","lichterkette","lichterkette_letzte"],view_range:4f,item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/lichterkette","minecraft:custom_model_data":{floats:[0f,0f,0f,0f,0f]}}}}
execute as @e[type=minecraft:item_display,tag=lichterkette_letzte,limit=1] run function sniffa:intern/kette_spannen
tp @e[type=minecraft:marker,tag=lichterkette_anfang] @e[type=minecraft:marker,tag=sniffa_ziel,limit=1]
kill @e[type=minecraft:marker,tag=sniffa_ziel]
tellraw @s [{"text":"Lichterkette hängt. ","color":"gold"},{"text":"Weiter geht's vom Ende aus: nächsten Punkt anschauen und ","color":"gray"},{"text":"/function sniffa:lichterkette_ende","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:lichterkette_ende"}}]
