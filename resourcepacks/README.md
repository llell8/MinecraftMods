# Resource packs

## Void PvP

Minecraft Java 1.21.11 (pack format 75). Purple, white and black, black hole / void themed.

- `VoidPvP/` – pack source, `VoidPvP.zip` – ready to drop into `.minecraft/resourcepacks`
- Generator: `python3 tools/gen_void.py` (reuses `gen_textures.py` with void palettes)

- Netherite sword, axe, pickaxe, shovel, hoe and spear – galaxy heads: deep space with a drifting
  purple nebula and twinkling stars, a bright violet edge and a pulsing star gem (no particles)
- Diamond tools – void-rod handles and a star gem; armor icons are vanilla
- Mace – a faceted void crystal head with a light glint sweeping across it
- Easter egg: rename the netherite sword, axe or mace to `Crushing Swedes` for a Danish-flag version
- Bow and crossbow – purple wood and a white string that charges green → yellow → red while drawing
  (a loaded crossbow stays red)
- Shield – obsidian-purple planks, violet rim and a black hole emblem (normal size; banner shields
  keep their white face)
- Hunger – purple meat on a white bone (AppleSkin saturation outline recoloured); hearts are vanilla
- Totem – an angel: golden halo with a glint, white wings, purple heart-gem
- Pearl / wind charge orbited by a white-to-purple accretion disk; violet cobwebs and wind
- Elytra – violet wings (no particles on the icon)
- Smoky violet potion glass, violet arrows, violet glint, white/violet crit X, dark purple hit
  hearts, low fire, small first-person totem; golden apples are left vanilla

## Phoenix PvP

Minecraft Java 1.21.11 (pack format 75).

- `PhoenixPvP/` – pack source
- `PhoenixPvP.zip` – ready to drop into `.minecraft/resourcepacks`
- `tools/` – generators for the textures (`python3 tools/gen_textures.py`, needs Pillow)

Textures so far:
- Netherite sword, axe, pickaxe, shovel, hoe and spear (icon + in-hand) – vanilla
  textures with blaze rod handles, animated fire on the head and an orange gem where the
  head meets the handle
- Diamond sword, axe, pickaxe, shovel, hoe, spear and armor icons – same treatment as netherite
  (blaze rod handles, animated flames, gem)
- Elytra – phoenix wings on the fire palette (worn wings, item with flames, broken item)
- Bow – vanilla with slightly orange wood and a yellow string
- Crossbow (all pull/loaded states) – vanilla wood with blaze rod string and metal
  (real blaze rod colours with glowing banded segments)
- Arrows (item and stuck in things) – blaze rod shaft, glowing tip, flame-coloured fletching
- Mace – netherite head with a glowing molten crack, copper blaze rod handle, animated flames
- Totem of undying – 16x16 dragon-rune style phoenix in the tools' fire colours: serpent body,
  solid spread wings, legs with talons, fanned tail feathers
- Netherite armor – icons with animated flame tongues rising off the top, armor left visible (worn armor is vanilla)
- Ender pearl, cobweb, wind charge – recoloured orange; two flickering yellow-to-crimson fires slowly
  orbit the pearl and wind charge items a pixel away from them (pearl shrunk slightly to fit);
  orange wind charge projectile and burst particles
- Potions – amber glass bottles with dark netherite stoppers (drinkable, splash, lingering, empty)
- Golden apple – molten gold with a flame at the stem
- Enchanted golden apple – its own red-hot texture with a crown of flames
- Hearts – animated flaming hearts: fire gradient, flickering flame tongues and heat shimmer;
  hardcore + damage flash too
- Hunger bar – phoenix feathers instead of drumsticks (green when you have Hunger)
- Saturation (needs the AppleSkin mod) – gold outline traced around the feathers
- Hit particles – thin orange X for crits, thin blue soul-fire sparkle for enchanted crits
  (the game tints those blue, so they can't be orange), plain dark orange hearts when you hit something
- Low fire – the burning overlay (and fire blocks) squashed to 60% height
- Small totem and shield in first person (smaller and lower; blocking pose only slightly lower)
- Enchant glint – orange (items and armor)

`tools/vanilla/` holds the vanilla 1.21.11 textures the generator starts from
(`tools/vanilla/appleskin/` is AppleSkin's 1.21.11 `icons.png`).
