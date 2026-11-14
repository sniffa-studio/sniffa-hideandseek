$execute if predicate {condition:"minecraft:entity_properties",entity:"this",predicate:{flags:{is_sneaking:true}}} run return run function sniffa:$(art)_start
$execute unless entity @e[type=minecraft:marker,tag=$(art)_anfang] run return run function sniffa:$(art)_start
$function sniffa:$(art)_ende
