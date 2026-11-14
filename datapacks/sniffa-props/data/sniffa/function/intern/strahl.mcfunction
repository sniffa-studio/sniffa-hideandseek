execute unless block ~ ~ ~ #sniffa:durchlaessig run return run summon minecraft:marker ~ ~ ~ {Tags:["sniffa_ziel"]}
execute if entity @s[distance=..64] positioned ^ ^ ^0.2 run function sniffa:intern/strahl
