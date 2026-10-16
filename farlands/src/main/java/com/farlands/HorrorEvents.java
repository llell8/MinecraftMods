package com.farlands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Picks a scary thing to happen to each player every few minutes. The longer
 * someone stays in the dark, the more "dread" they build and the worse it gets.
 */
public final class HorrorEvents {
    public static final List<String> EVENT_NAMES = List.of(
            "footsteps", "cave", "mining", "torch", "door", "whisper", "joined", "darkness", "watcher");

    private static final String[] WHISPERS = {
            "turn around",
            "you are not alone out here",
            "it saw you",
            "why did you come this far",
            "the edge is closer than you think",
            "don't sleep",
            "i can hear you breathing",
            "it's behind the wall",
            "go back",
            "he was here before you",
    };

    private static final String[] NAMES = {"Farlands", "null", "Herobrine", "nobody", "§kxxxxxx"};

    /** Per-player dread, in ticks. Grows faster in the dark. */
    private static final Map<UUID, Integer> DREAD = new HashMap<>();
    /** Ticks until the next event, per player. */
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    /** Small scheduler so events can play out over several seconds. */
    private static final List<Delayed> QUEUE = new ArrayList<>();

    private record Delayed(ServerPlayer player, int[] ticksLeft, Consumer<ServerPlayer> action) {}

    private HorrorEvents() {}

    public static void tick(ServerPlayer player) {
        if (player.isSpectator() || player.isCreative()) return;

        UUID id = player.getUUID();
        boolean dark = isDark(player);
        int dread = DREAD.merge(id, dark ? 2 : 1, Integer::sum);
        int cooldown = COOLDOWN.getOrDefault(id, 20 * 180) - (dark ? 2 : 1);

        if (cooldown <= 0) {
            randomEvent(player, dread);
            RandomSource random = player.getRandom();
            // 1.5 - 5 minutes between events, a bit less once dread is high
            int next = 20 * (90 + random.nextInt(210));
            if (dread > 20 * 60 * 30) next = next * 2 / 3;
            cooldown = next;
        }
        COOLDOWN.put(id, cooldown);
    }

    /** Runs queued steps of multi-part events; called once per server tick. */
    public static void tickQueue() {
        if (QUEUE.isEmpty()) return;
        List<Delayed> due = new ArrayList<>();
        for (Iterator<Delayed> it = QUEUE.iterator(); it.hasNext(); ) {
            Delayed d = it.next();
            if (d.player.isRemoved()) {
                it.remove();
            } else if (--d.ticksLeft[0] <= 0) {
                it.remove();
                due.add(d);
            }
        }
        for (Delayed d : due) d.action.accept(d.player);
    }

    public static void forget(ServerPlayer player) {
        DREAD.remove(player.getUUID());
        COOLDOWN.remove(player.getUUID());
        QUEUE.removeIf(d -> d.player == player);
    }

    public static boolean run(ServerPlayer player, String name) {
        switch (name) {
            case "footsteps" -> footsteps(player);
            case "cave" -> caveNoise(player);
            case "mining" -> mining(player);
            case "torch" -> { if (!snuffTorch(player)) footsteps(player); }
            case "door" -> { if (!slamDoor(player)) caveNoise(player); }
            case "whisper" -> whisper(player);
            case "joined" -> fakeJoin(player);
            case "darkness" -> darkness(player);
            case "watcher" -> { if (!Watcher.spawn(player)) footsteps(player); }
            default -> { return false; }
        }
        return true;
    }

    private static void randomEvent(ServerPlayer player, int dread) {
        RandomSource random = player.getRandom();
        int minutes = dread / (20 * 60);

        // Early on only subtle things happen; later the pool gets nastier.
        List<String> pool = new ArrayList<>(List.of("footsteps", "cave", "cave"));
        if (!player.level().canSeeSky(player.blockPosition())) pool.add("mining");
        if (minutes >= 5) pool.addAll(List.of("torch", "torch", "door", "whisper"));
        if (minutes >= 10) pool.addAll(List.of("watcher", "watcher", "darkness"));
        if (minutes >= 20) pool.addAll(List.of("joined", "watcher", "whisper"));

        String pick = pool.get(random.nextInt(pool.size()));
        Farlands.LOGGER.debug("Farlands event '{}' for {}", pick, player.getName().getString());
        run(player, pick);
    }

    // ---- events ----

    /** Something walks up behind you and stops. */
    private static void footsteps(ServerPlayer player) {
        Vec3 behind = horizontalLook(player).scale(-1);
        int steps = 4 + player.getRandom().nextInt(4);
        for (int i = 0; i < steps; i++) {
            double dist = 9.0 - i * (6.0 / steps);
            later(player, i * 9, p -> {
                Vec3 at = p.position().add(behind.scale(dist));
                BlockState below = p.level().getBlockState(BlockPos.containing(at).below());
                SoundEvent step = below.isAir() ? SoundEvents.STONE_STEP : below.getSoundType().getStepSound();
                sound(p, step, SoundSource.HOSTILE, at, 0.6F, 0.9F);
            });
        }
    }

    private static void caveNoise(ServerPlayer player) {
        RandomSource r = player.getRandom();
        Vec3 at = player.position().add(r.nextGaussian() * 8, r.nextGaussian() * 3, r.nextGaussian() * 8);
        sound(player, SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, at, 1.0F, 0.6F + r.nextFloat() * 0.3F);
    }

    /** Someone is digging toward you through the stone. */
    private static void mining(ServerPlayer player) {
        RandomSource r = player.getRandom();
        Vec3 dir = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.3, r.nextGaussian()).normalize();
        for (int i = 0; i < 6; i++) {
            double dist = 14 - i;
            later(player, i * 14 + r.nextInt(6), p -> {
                Vec3 at = p.position().add(dir.scale(dist));
                sound(p, SoundEvents.STONE_HIT, SoundSource.BLOCKS, at, 0.8F, 0.7F);
            });
        }
        later(player, 100, p -> sound(p, SoundEvents.STONE_BREAK, SoundSource.BLOCKS,
                p.position().add(dir.scale(8)), 1.0F, 0.6F));
    }

    /** A torch you can't see goes out. Gone. */
    private static boolean snuffTorch(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = horizontalLook(player);
        BlockPos center = player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-12, -4, -12), center.offset(12, 4, 12))) {
            BlockState state = level.getBlockState(pos);
            if (!isTorch(state)) continue;
            Vec3 to = Vec3.atCenterOf(pos).subtract(player.getEyePosition());
            if (to.horizontalDistanceSqr() < 9) continue;
            if (look.dot(to.normalize()) > 0.2) continue; // must be out of view
            found.add(pos.immutable());
        }
        if (found.isEmpty()) return false;

        BlockPos pos = found.get(player.getRandom().nextInt(found.size()));
        level.removeBlock(pos, false);
        Vec3 c = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 12, 0.1, 0.2, 0.1, 0.01);
        sound(player, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, c, 0.5F, 0.8F);
        return true;
    }

    /** A nearby door swings, then slams. */
    private static boolean slamDoor(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-10, -3, -10), center.offset(10, 3, 10))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door
                    && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && pos.distSqr(center) > 9) {
                BlockPos at = pos.immutable();
                boolean open = state.getValue(DoorBlock.OPEN);
                door.setOpen(null, level, state, at, !open);
                later(player, 25, p -> {
                    BlockState now = level.getBlockState(at);
                    if (now.getBlock() instanceof DoorBlock d) d.setOpen(null, level, now, at, open);
                });
                return true;
            }
        }
        return false;
    }

    private static void whisper(ServerPlayer player) {
        String text = WHISPERS[player.getRandom().nextInt(WHISPERS.length)];
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        sound(player, SoundEvents.ENDERMAN_AMBIENT, SoundSource.HOSTILE,
                player.position().add(horizontalLook(player).scale(-2)), 0.25F, 0.5F);
    }

    /** "Farlands joined the game" ... and leaves just as fast. Only you see it. */
    private static void fakeJoin(ServerPlayer player) {
        String name = NAMES[player.getRandom().nextInt(NAMES.length)];
        player.sendSystemMessage(Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW));
        later(player, 60 + player.getRandom().nextInt(60), p ->
                p.sendSystemMessage(Component.translatable("multiplayer.player.left", name).withStyle(ChatFormatting.YELLOW)));
    }

    /** The lights die for a few seconds and your heart is very loud. */
    private static void darkness(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 6, 0, false, false));
        for (int i = 0; i < 6; i++) {
            later(player, i * 18, p -> sound(p, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE,
                    p.position(), 1.0F, 1.0F));
        }
    }

    // ---- helpers ----

    static boolean isDark(ServerPlayer player) {
        return player.level().getMaxLocalRawBrightness(player.blockPosition()) < 7;
    }

    static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }

    static void later(ServerPlayer player, int ticks, Consumer<ServerPlayer> action) {
        QUEUE.add(new Delayed(player, new int[]{Math.max(1, ticks)}, action));
    }

    /** Plays a sound that only this player can hear. */
    static void sound(ServerPlayer player, SoundEvent sound, SoundSource source, Vec3 at, float volume, float pitch) {
        sound(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, at, volume, pitch);
    }

    static void sound(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source, Vec3 at, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(sound, source, at.x, at.y, at.z, volume, pitch,
                player.getRandom().nextLong()));
    }
}
