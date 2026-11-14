kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
function sniffa:intern/laden
execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] align xyz positioned ~0.5 ~1 ~0.5 run kill @e[type=minecraft:item_display,tag=kraehen,distance=..0.6]
execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] align xyz positioned ~0.5 ~1 ~0.5 run summon minecraft:item_display ~ ~ ~ {Tags:["sniffa_prop","kraehen","kraehen_neu"],view_range:4f,item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/kraehen","minecraft:custom_model_data":{floats:[4f,0f,0f]}}}}
execute as @e[type=minecraft:item_display,tag=kraehen_neu] run function sniffa:intern/kraehen_uhr
tag @e[type=minecraft:item_display,tag=kraehen_neu] remove kraehen_neu
kill @e[type=minecraft:marker,tag=sniffa_ziel]
tellraw @s [{"text":"Krähen sitzen. ","color":"gold"},{"text":"Wer in der Nähe rennt, scheucht sie auf. Weg damit: Stelle anschauen und ","color":"gray"},{"text":"/function sniffa:kraehen_weg","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:kraehen_weg"}}]
