$execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] align xyz positioned ~0.5 ~ ~0.5 rotated $(yaw) 0 positioned ^ ^ ^1 store result score #frei sniffa_deko run function sniffa:intern/deko/wand_frei
execute if score #frei sniffa_deko matches 0 run return run function sniffa:intern/kette_abgelehnt {grund:"Vor dieser Wand ist kein Platz."}
$execute at @e[type=minecraft:marker,tag=sniffa_ziel,limit=1] align xyz positioned ~0.5 ~ ~0.5 rotated $(yaw) 0 positioned ^ ^ ^1 run summon minecraft:marker ~ ~ ~ {Tags:["sniffa_deko","deko_neu"]}
execute as @e[type=minecraft:marker,tag=deko_neu] run data modify entity @s data set from storage sniffa:deko args
$execute as @e[type=minecraft:marker,tag=deko_neu] run rotate @s $(yaw) 0
execute as @e[type=minecraft:marker,tag=deko_neu] at @s rotated as @s run function sniffa:intern/deko/aufbauen
tag @e[type=minecraft:marker,tag=deko_neu] remove deko_neu
kill @e[type=minecraft:marker,tag=sniffa_ziel]
$tellraw @s [{"text":"$(name) hängt. ","color":"gold"},{"text":"Weg damit: anschauen und ","color":"gray"},{"text":"/function sniffa:deko_weg","color":"yellow","click_event":{"action":"suggest_command","command":"/function sniffa:deko_weg"}}]
