# Phoenix PvP resource pack

Minecraft Java 1.21.11 (pack format 75).

- `PhoenixPvP/` – pack source
- `PhoenixPvP.zip` – ready to drop into `.minecraft/resourcepacks`
- `tools/` – generators for the textures (`python3 tools/gen_textures.py`, needs Pillow)

Textures so far:
- Netherite sword, axe, pickaxe, shovel, hoe and spear (icon + in-hand) – vanilla
  textures with blaze rod handles, animated fire on the head and an orange gem where the
  head meets the handle
- Mace – netherite head with a glowing molten crack, copper blaze rod handle, animated flames
- Totem of undying – 16x16 dragon-rune style phoenix in the tools' fire colours: serpent body,
  solid spread wings, legs with talons, fanned tail feathers
- Netherite armor – icons wrapped in light animated flames that leave the armor visible (worn armor is vanilla)
- Golden apple – molten gold with a flame at the stem
- Enchanted golden apple – its own red-hot texture with a crown of flames
- Hearts – animated flaming hearts: fire gradient, flickering flame tongues and heat shimmer;
  hardcore + damage flash too
- Hunger bar – phoenix feathers instead of drumsticks (green when you have Hunger)
- Enchant glint – orange (items and armor)

`tools/vanilla/` holds the vanilla 1.21.11 textures the generator starts from.
