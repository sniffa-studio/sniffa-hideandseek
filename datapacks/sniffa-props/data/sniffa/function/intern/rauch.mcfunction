kill @e[type=minecraft:marker,tag=sniffa_ziel]
execute anchored eyes positioned ^ ^ ^ run function sniffa:intern/strahl
execute unless entity @e[type=minecraft:marker,tag=sniffa_ziel] run return run tellraw @s {"text":"Kein Block im Blick (bis 64 Blöcke).","color":"red"}
execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] align xyz positioned ~0.5 ~1 ~0.5 run kill @e[type=minecraft:item_display,tag=rauch,distance=..0.6]
$execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] align xyz positioned ~0.5 ~1 ~0.5 run summon minecraft:item_display ~ ~ ~ {Tags:["sniffa_prop","rauch"],view_range:4f,item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/rauch","minecraft:custom_model_data":{floats:[$(staerke)f]}}}}
kill @e[type=minecraft:marker,tag=sniffa_ziel]
$tellraw @s [{"text":"Rauch steigt auf (Stärke $(staerke)). ","color":"gold"},{"text":"Weg damit: Schornstein anschauen und ","color":"gray"},{"text":"/function sniffa:rauch_weg","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:rauch_weg"}}]
