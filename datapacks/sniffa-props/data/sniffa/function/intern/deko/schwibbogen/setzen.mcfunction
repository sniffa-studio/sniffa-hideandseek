$summon minecraft:item_display ^ ^1.5625 ^ {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/schwibbogen_3"}}}
$summon minecraft:item_display ^-1.5 ^0.9625 ^ {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/schwibbogen_2"}}}
$summon minecraft:item_display ^1.5 ^0.9625 ^ {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/schwibbogen_1"}}}
execute if block ^1 ^ ^ #minecraft:air run setblock ^1 ^ ^ minecraft:barrier strict
execute if block ^1 ^1 ^ #minecraft:air run setblock ^1 ^1 ^ minecraft:barrier strict
execute if block ^ ^ ^ #minecraft:air run setblock ^ ^ ^ minecraft:barrier strict
execute if block ^ ^1 ^ #minecraft:air run setblock ^ ^1 ^ minecraft:barrier strict
execute if block ^-1 ^ ^ #minecraft:air run setblock ^-1 ^ ^ minecraft:barrier strict
execute if block ^-1 ^1 ^ #minecraft:air run setblock ^-1 ^1 ^ minecraft:barrier strict
