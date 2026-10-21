# Phoenix PvP

A phoenix-themed PvP resource pack for Minecraft **1.21.11** (pack format 75), built
one texture at a time on top of vanilla.

## So far

- **Diamond sword:** the vanilla sword with each of its seven teal shades swapped
  for a fire color, from a dark ember outline to a pale gold edge. The shape and
  the stick handle are unchanged.

## Install

Zip the contents of `pack/` (or grab `Phoenix-PvP.zip` from the
**phoenix-pvp-latest** release) and drop it into `.minecraft/resourcepacks`.

## Rebuild

```sh
pip install pillow
python3 make_sword.py
```

The colors are the `SWAP` table at the top of `make_sword.py`.
