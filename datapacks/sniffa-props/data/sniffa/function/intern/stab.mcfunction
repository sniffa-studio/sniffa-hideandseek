scoreboard players reset @s sniffa_stab
execute unless items entity @s weapon.mainhand minecraft:carrot_on_a_stick[minecraft:custom_data~{sniffa_lichterkette:1b}] run return 0
execute if predicate {condition:"minecraft:entity_properties",entity:"this",predicate:{flags:{is_sneaking:true}}} run return run function sniffa:lichterkette_start
execute unless entity @e[type=minecraft:marker,tag=lichterkette_anfang] run return run function sniffa:lichterkette_start
function sniffa:lichterkette_ende
