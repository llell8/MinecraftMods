# The Backrooms

A Fabric mod for Minecraft 1.21.11 that adds Level 0 of the Backrooms.

- **Getting in:** reality is wearing thin. Once in a while (about every five minutes) two blocks one above the other near you start glitching: a see-through, flickering overlay of static, color-split lines and torn yellow wallpaper, and the blocks tear open into a 1x2 doorway of static. Step into it and you fall through into the Backrooms. The blocks come back after a minute or so. Suffocating inside a block in the overworld also has a 1-in-5 chance of noclipping you in.
- **Level 0:** an endless maze of mono-yellow wallpaper, moist carpet and humming, flickering fluorescent lights. It has open pillar halls, cramped corridors and dark zones full of dead lights. The blocks can't be broken in survival.
- **Smilers:** faces in the dark: two glowing eyes and a grin full of teeth, nothing else. There are two kinds that behave the same: the Smiler, with a fan of teeth that glow and split into red and blue at the edges, and the VHS Smiler, a fuzzy grey grin with round staring eyes. They never chase you. After about 20 seconds in Level 0 they start appearing where the lights are dead, always out of sight, and turn to watch you. Walk within 3 blocks of one and it lunges. If light reaches one, or you hit it, it slips away to another dark spot. Well-lit areas are safe, mostly: the first time you spot a Smiler, there's a chance every light around you dies at once. For about 12 seconds it comes for you through the dark, and if it reaches you, it bites. They don't appear on Peaceful.
- **The Bacteria:** three blocks of black wire, after the found footage: a flat blob of a head with a curl of wire around it, wide shoulders hooked at the ends, a spine like a kinked chain, crooked legs, one arm reaching to the floor like a crutch and the other bent across its body. It never stops shaking. After about a minute in the Backrooms one can turn up a few rooms away. It wanders, calling out in stolen human voices ("Hello?", "Help me... please.", "It's okay. Come closer.") to draw you in, screams when it spots you, and runs you down. Unlike a Smiler, it can be killed, but it takes a lot of hitting.
- **Deeper in:** exit doors (green EXIT sign) are rare, hidden in the walls of Level 0. They don't lead out: they lead deeper, to Level 1 or Level 2 at random.
  - **Level 1, the Habitable Zone:** an endless concrete warehouse with seven blocks of headroom, pillars striped with hazard paint at the foot, the odd concrete wall, damp floors with puddles and oil stains, hanging lights with whole stretches dark, and stacks of crates.
  - **Level 2, Pipe Dreams:** one low basement after another: painted cinderblock walls with doorways between them, stained concrete floors with drains and puddles, and pipes everywhere. Big rusty mains run along the ceilings for rooms at a time, straight through the walls, with copper pipes beside them; more pipes hug the walls at head height and risers climb the corners. Only a few lights, flickering fluorescents and red warning lamps, so Smilers love it.
- **Getting out:** the exit doors on Levels 1 and 2 take you back to the surface where you first fell through.
- **Screams from nowhere:** every few minutes in the Backrooms you hear a Bacteria scream right in your ears, even when there's no Bacteria anywhere near.

All blocks are in the Building Blocks creative tab. The Smilers and the Bacteria have spawn eggs in the Spawn Eggs tab. Glitched Blocks are in the Building Blocks tab: stack two and walk in to test the way in. A Smiler placed somewhere bright moves to a dark spot nearby, or vanishes if there is none. They don't attack players in creative.

Build with `gradle build`. The jar ends up in `build/libs/`.

## Credits

The Bacteria's voices were made with [Piper](https://github.com/rhasspy/piper) text-to-speech (MIT) using its LibriTTS voice, trained on [LibriTTS](http://www.openslr.org/60/) (CC BY 4.0). The screams are synthesized from a model of the human voice. Everything was processed with ffmpeg; the other sounds are synthesized or reuse vanilla Minecraft sounds.
