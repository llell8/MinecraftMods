# Farlands

A Fabric horror mod for Minecraft 1.21.11. Works in singleplayer and on servers
(only the server needs it; vanilla clients are fine).

The longer you play, and especially the longer you spend in the dark, the more
the world starts acting wrong around you:

- footsteps walking up behind you
- cave noises and someone mining toward you through the stone
- torches you aren't looking at going out
- doors swinging open and shut on their own
- whispers in chat, and players who "join the game" and vanish again
- your lights dying while your heartbeat gets loud
- **the Watcher**: a still figure far away that faces you, and is gone when you stare at it or get close

Creative and spectator players are left alone. Use `/farlands <event>` to trigger
an event on yourself (`footsteps`, `cave`, `mining`, `torch`, `door`, `whisper`,
`joined`, `darkness`, `watcher`).

## Opening in IntelliJ IDEA

1. **File → Open…** and pick this `farlands` folder (not the repo root).
2. Trust the project and let Gradle sync (needs JDK 21+).
3. Run the `Minecraft Client` run configuration that Loom generates, or the
   Gradle task `runClient`.

## Building

```
./gradlew build
```

The mod jar is `build/libs/Farlands.jar`. Drop it in your `mods` folder along with Fabric API.
