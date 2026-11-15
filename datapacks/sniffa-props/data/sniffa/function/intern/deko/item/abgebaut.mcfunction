$execute as @p[distance=..12,gamemode=creative] run function sniffa:item/$(art)
$tellraw @p[distance=..12,gamemode=creative] [{"text":"$(name)","color":"gold"},{"text":" abgebaut und ins Inventar gelegt.","color":"gray"}]
function sniffa:intern/deko/abbauen
