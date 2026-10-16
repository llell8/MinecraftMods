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
- Netherite armor – icons with animated flame tongues rising off the top, armor left visible (worn armor is vanilla)
- Ender pearl, cobweb, wind charge – recoloured orange; two flickering yellow-to-crimson fires slowly
  orbit the pearl and wind charge items a pixel away from them (pearl shrunk slightly to fit);
  orange wind charge projectile and burst particles
- Golden apple – molten gold with a flame at the stem
- Enchanted golden apple – its own red-hot texture with a crown of flames
- Hearts – animated flaming hearts: fire gradient, flickering flame tongues and heat shimmer;
  hardcore + damage flash too
- Hunger bar – phoenix feathers instead of drumsticks (green when you have Hunger)
- Saturation (needs the AppleSkin mod) – gold outline traced around the feathers
- Hit particles – thin orange X for crits, thin blue soul-fire sparkle for enchanted crits
  (the game tints those blue, so they can't be orange), plain dark orange hearts when you hit something
- Enchant glint – orange (items and armor)

`tools/vanilla/` holds the vanilla 1.21.11 textures the generator starts from
(`tools/vanilla/appleskin/` is AppleSkin's 1.21.11 `icons.png`).
