# Text effects (marker colours)

The mod overrides the vanilla text shader (`assets/minecraft/shaders/core/text.vsh` and `text.fsh`). Text whose
colour is exactly one of the marker colours below gets an effect. Every other colour renders exactly as in vanilla.
Players without the mod simply see the marker colour, so each one is picked to look right on its own.

The shader table lives in `assets/hideandseek/shaders/include/text_shine.glsl`. The same values are in
`ShineText` (mod) and `Shine` (plugin). Change all three together.

| Marker | Colour | Effect | Used for |
|---|---|---|---|
| `SHINE_GOLD` | `#F2C55B` | own colour, shine sweep, sparkles | ✦ and value in the voucher chat line, value under the voucher title, shop header |
| `SHINE_TEXT` | `#CFD7E3` | own colour, shine sweep | text of the voucher chat line |
| `SHINE_GREEN` | `#4FD08C` | own colour, shine sweep | finder name in the voucher chat line |
| `TITLE_BLUE` | `#5FA2F9` | blue to white gradient, shine, sparkles, blue glow | „Gutschein gefunden!“ for 5 and 10 € |
| `TITLE_RED` | `#FD3C4F` | red to peach gradient, shine, sparkles, red glow | „Gutschein gefunden!“ for 15 €, „DIE JAGD BEGINNT“, GEFUNDEN screen |
| `TITLE_XMAS` | `#F5C352` | warm gold letters, warm shine, snow sparkles, soft red glow | „VERSTECKEN“ at round start, „Runde beginnt“ bar |
| `TITLE_GOLD` | `#F7BE52` | same Christmas look as `TITLE_XMAS` | winner title, award ceremony |

- Shine, gradient and sparkles run in the fragment shader on screen position and game time. The glow on server text
  tints the vanilla text shadow (`floor(colour × 0.25)`), so only the four title markers touch their shadow.
- Text the mod draws itself (`ShineText`) also gets a soft eight-way glow and letters that drop in one after another,
  with an optional gentle wave.
- With an Iris shader pack, world text such as signs uses the pack's programs and shows the plain marker colour.
