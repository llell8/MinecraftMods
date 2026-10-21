# Vermilion PvP

A Vermilion Bird (Zhuque) themed PvP resource pack for Minecraft 1.21.x (made for 1.21.11).

- Orange enchant glint, on both items and armor
- Short swords for every tier: vermilion lacquer wood, stone, copper, iron, gold with a jade guard, imperial jade diamond, and a black netherite blade with a fire core. Every tier has a gold guard, a crimson grip and a red tassel.
- Low vermilion fire (both in the world and on screen)
- Phoenix-fire ender pearl

## Install
Download `Vermilion-PvP.zip` from the `vermilion-pack-latest` release, or build it yourself.
Put the zip in `.minecraft/resourcepacks` and enable it in Options → Resource Packs.

## Build
```
python3 vermilion-pack/generate.py
```
This needs Pillow. It regenerates `pack/` and writes `Vermilion-PvP.zip`. Every texture is drawn in code,
so to change colours, edit `SWORDS` or the fire/glint palettes in `generate.py`.
