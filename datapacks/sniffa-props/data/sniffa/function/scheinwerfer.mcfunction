summon minecraft:item_display ~ ~0.5 ~ {Tags:["sniffa_prop","scheinwerfer","sniffa_new"],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/scheinwerfer_sockel"}}}
summon minecraft:item_display ~ ~0.5 ~ {Tags:["sniffa_prop","scheinwerfer","scheinwerfer_kopf","sniffa_new"],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/scheinwerfer_kopf"}},transformation:{left_rotation:{angle:1.5707964f,axis:[1f,0f,0f]},right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]}}
execute as @e[type=minecraft:item_display,tag=sniffa_new,tag=!scheinwerfer_kopf,distance=..2] run tp @s ~ ~0.5 ~ ~ 0
execute as @e[type=minecraft:item_display,tag=sniffa_new,tag=scheinwerfer_kopf,distance=..2] run tp @s ~ ~0.5 ~ ~ ~
tag @e[type=minecraft:item_display,tag=sniffa_new,distance=..2] remove sniffa_new
