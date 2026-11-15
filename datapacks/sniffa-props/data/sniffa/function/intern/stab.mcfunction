scoreboard players reset @s sniffa_stab
execute if items entity @s weapon.mainhand minecraft:carrot_on_a_stick[minecraft:custom_data~{sniffa_deko:{}}] run return run function sniffa:intern/deko/item/benutzen
execute if items entity @s weapon.mainhand minecraft:carrot_on_a_stick[minecraft:custom_data~{sniffa_lichterkette:1b}] run return run function sniffa:intern/stab_klick {art:"lichterkette"}
execute if items entity @s weapon.mainhand minecraft:carrot_on_a_stick[minecraft:custom_data~{sniffa_eiszapfen:1b}] run return run function sniffa:intern/stab_klick {art:"eiszapfen"}
