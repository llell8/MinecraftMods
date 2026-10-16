# Phoenix PvP resource pack

Minecraft Java 1.21.11 (pack format 75).

- `PhoenixPvP/` – pack source
- `PhoenixPvP.zip` – ready to drop into `.minecraft/resourcepacks`
- `tools/` – generators for the textures (`python3 tools/gen_textures.py`, needs Pillow)

Textures so far:
- Netherite sword, axe, pickaxe, shovel, hoe and spear (icon + in-hand) – vanilla
  textures with blaze rod handles, animated fire on the head and an orange gem where the
  head meets the handle
- Totem of undying – 16x16 dragon-rune style phoenix in the tools' fire colours: serpent body,
  solid spread wings, legs with talons, fanned tail feathers
- Netherite armor – animated flames on the icons (worn armor is vanilla)
- Golden apple – molten gold with a flame at the stem
- Enchanted golden apple – its own red-hot texture with a crown of flames
- Enchant glint – orange (items and armor)

`tools/vanilla/` holds the vanilla 1.21.11 textures the generator starts from.
