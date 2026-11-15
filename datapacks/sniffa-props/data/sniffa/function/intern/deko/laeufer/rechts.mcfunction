$execute rotated ~-7.5 0 positioned ^$(radius) ^1 ^ run summon minecraft:item_display ~ ~ ~ {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu","deko_laeufer","r$(radius)","laeufer_neu"],teleport_duration:10,item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/eislaeufer_$(modell)"}}}
execute as @e[type=minecraft:item_display,tag=laeufer_neu] positioned as @s run tp @s ~ ~ ~ ~ 0
tag @e[type=minecraft:item_display,tag=laeufer_neu] remove laeufer_neu
