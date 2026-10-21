# Phoenix PvP

A phoenix-themed PvP resource pack for Minecraft **1.21.11** (pack format 75), built
one texture at a time on top of vanilla.

## So far

- **Netherite sword:** still reads as vanilla netherite. The grey blade is only
  warmed slightly, the purple trim becomes an ember red, and the blade's light edge
  and grip pick up a dull fire glow. The shape and shading are unchanged.

## Install

Zip the contents of `pack/` (or grab `Phoenix-PvP.zip` from the
**phoenix-pvp-latest** release) and drop it into `.minecraft/resourcepacks`.

## Rebuild

```sh
pip install pillow
python3 make_sword.py
```

The colors are the `SWAP` table at the top of `make_sword.py`.
