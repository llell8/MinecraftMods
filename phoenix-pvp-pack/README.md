# Phoenix PvP

A phoenix-themed PvP resource pack for Minecraft **1.21.11** (pack format 75).
It keeps vanilla's shapes and shading. Each texture is the vanilla one,
gradient-mapped onto phoenix fire palettes, so everything still reads like vanilla
at a glance.

| What | Look |
| --- | --- |
| Diamond swords, axes and armour | Phoenix flame (crimson to orange to gold) |
| Netherite | Ashen charcoal with ember highlights |
| Gold / mace / golden apple / XP bar | Solar gold |
| Iron | White-hot flame |
| Chainmail | Rust orange |
| Stone / wood tools | Ember stone / charred wood (vanilla stick handles) |
| Totem of Undying | Burning phoenix totem, also the pack icon |
| Elytra (item and wings) | Fire wings fading from crimson to gold |
| Ender pearl | Crimson orb |
| Bows and crossbows | Charred wood with glowing string |
| Arrows | Flame fletching |
| Shield (no banner) | Charred planks with a gold rim |
| End crystal | Phoenix fire |
| Enchantment glint | Orange flame instead of purple |
| Hotbar, buttons, inventory, chests, shulkers | Ember-dark hotbar and buttons, warm light containers (labels stay readable) |
| Hearts | Crimson |
| Low fire | The burning overlay sits 6px lower so it stays out of your view |

Leather armour stays vanilla because the game dyes it at runtime.

## Install

Grab `Phoenix-PvP.zip` from the **phoenix-pvp-latest** release, or zip the
contents of `pack/` yourself, and drop it into `.minecraft/resourcepacks`.

## Rebuild

```sh
pip install pillow
python3 generate.py --zip   # writes pack/ and build/Phoenix-PvP.zip
```

The script fetches the vanilla 1.21.11 textures from a mirror the first time it
runs and caches them in `.vanilla-cache/`. To retheme the pack, edit the palettes
at the top of `generate.py`.

Low fire changes `block/fire_1.png`, which also draws one of the flame layers on
fire blocks in the world. Those flames look a bit shorter too.
