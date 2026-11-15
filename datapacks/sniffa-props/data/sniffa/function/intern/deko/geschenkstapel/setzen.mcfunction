$summon minecraft:item_display ^-0.5 ^1 ^-0.5 {Tags:["sniffa_prop","sniffa_deko_teil","deko_teil_neu"],Rotation:[$(yaw)f,0f],item:{id:"minecraft:stick",count:1,components:{"minecraft:item_model":"hideandseek:props/deko/geschenkstapel"}}}
execute if block ^ ^ ^ #minecraft:air run setblock ^ ^ ^ minecraft:barrier strict
execute if block ^ ^1 ^ #minecraft:air run setblock ^ ^1 ^ minecraft:barrier strict
execute if block ^ ^2 ^ #minecraft:air run setblock ^ ^2 ^ minecraft:barrier strict
execute if block ^ ^ ^-1 #minecraft:air run setblock ^ ^ ^-1 minecraft:barrier strict
execute if block ^ ^1 ^-1 #minecraft:air run setblock ^ ^1 ^-1 minecraft:barrier strict
execute if block ^ ^2 ^-1 #minecraft:air run setblock ^ ^2 ^-1 minecraft:barrier strict
execute if block ^-1 ^ ^ #minecraft:air run setblock ^-1 ^ ^ minecraft:barrier strict
execute if block ^-1 ^1 ^ #minecraft:air run setblock ^-1 ^1 ^ minecraft:barrier strict
execute if block ^-1 ^2 ^ #minecraft:air run setblock ^-1 ^2 ^ minecraft:barrier strict
execute if block ^-1 ^ ^-1 #minecraft:air run setblock ^-1 ^ ^-1 minecraft:barrier strict
execute if block ^-1 ^1 ^-1 #minecraft:air run setblock ^-1 ^1 ^-1 minecraft:barrier strict
execute if block ^-1 ^2 ^-1 #minecraft:air run setblock ^-1 ^2 ^-1 minecraft:barrier strict
