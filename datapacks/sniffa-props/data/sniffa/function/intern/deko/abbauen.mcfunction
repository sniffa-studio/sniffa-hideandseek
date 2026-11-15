scoreboard players operation #id sniffa_deko = @s sniffa_deko
execute as @e[type=minecraft:item_display,tag=sniffa_deko_teil] if score @s sniffa_deko = #id sniffa_deko run kill @s
function sniffa:intern/deko/raeumen with entity @s data
kill @s
