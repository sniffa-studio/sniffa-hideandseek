$summon minecraft:item_display ^-0.8125 ^1.1562 ^0.1875 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_8"}}}
$summon minecraft:item_display ^0.875 ^1.1562 ^-0.25 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_7"}}}
$summon minecraft:item_display ^-1.5 ^3.0386 ^1.75 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_6"}}}
$summon minecraft:item_display ^1.5 ^3.0386 ^1.75 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_5"}}}
$summon minecraft:item_display ^-1.5 ^0.0386 ^-1.25 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_4"}}}
$summon minecraft:item_display ^-1.5 ^0.0386 ^1.75 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_3"}}}
$summon minecraft:item_display ^1.5 ^0.0386 ^-1.25 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_2"}}}
$summon minecraft:item_display ^1.5 ^0.0386 ^1.75 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/rentiergehege_1"}}}
execute if block ^2 ^ ^1 #minecraft:air run setblock ^2 ^ ^1 minecraft:barrier strict
execute if block ^2 ^ ^ #minecraft:air run setblock ^2 ^ ^ minecraft:barrier strict
execute if block ^2 ^ ^-1 #minecraft:air run setblock ^2 ^ ^-1 minecraft:barrier strict
execute if block ^1 ^ ^1 #minecraft:air run setblock ^1 ^ ^1 minecraft:barrier strict
execute if block ^1 ^ ^-1 #minecraft:air run setblock ^1 ^ ^-1 minecraft:barrier strict
execute if block ^ ^ ^1 #minecraft:air run setblock ^ ^ ^1 minecraft:barrier strict
execute if block ^ ^ ^-1 #minecraft:air run setblock ^ ^ ^-1 minecraft:barrier strict
execute if block ^-1 ^ ^1 #minecraft:air run setblock ^-1 ^ ^1 minecraft:barrier strict
execute if block ^-1 ^ ^-1 #minecraft:air run setblock ^-1 ^ ^-1 minecraft:barrier strict
execute if block ^-2 ^ ^1 #minecraft:air run setblock ^-2 ^ ^1 minecraft:barrier strict
execute if block ^-2 ^ ^ #minecraft:air run setblock ^-2 ^ ^ minecraft:barrier strict
execute if block ^-2 ^ ^-1 #minecraft:air run setblock ^-2 ^ ^-1 minecraft:barrier strict
