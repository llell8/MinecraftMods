package com.meryx.backrooms;

import com.meryx.backrooms.world.BackroomsChunkGenerator;
import com.meryx.backrooms.world.Field;
import com.meryx.backrooms.world.Layout;
import com.meryx.backrooms.world.Maze;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Getting into and out of the Backrooms. Suffocating inside a block in the overworld can glitch you through
 * reality into Level 0; an exit door takes you back to the surface where you fell through.
 */
public final class Noclip {
    /** Chance that a suffocation hurt in the overworld noclips you instead. */
    private static final float NOCLIP_CHANCE = 0.2F;
    /** The overworld position a player noclipped from, kept on the player as a scoreboard tag so it survives restarts. */
    private static final String RETURN_TAG = "backrooms_return:";

    private static final Set<UUID> PENDING = new HashSet<>();
    /** After the first night: the chance a noclip lands in The Field instead of Level 0. */
    private static final float FIELD_CHANCE = 0.35F;

    private Noclip() {
    }

    public static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity instanceof ServerPlayer player
                && source.is(DamageTypes.IN_WALL)
                && player.level().dimension() == Level.OVERWORLD
                && player.getRandom().nextFloat() < NOCLIP_CHANCE) {
            // Teleporting while the player is mid-tick is asking for trouble, so do it at the end of the tick
            PENDING.add(player.getUUID());
            return false;
        }
        return true;
    }

    /** Sends a player to Level 0 at the end of this tick. */
    public static void queue(ServerPlayer player) {
        PENDING.add(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        // Fell down a pitfall in Level 0: you come out somewhere deeper
        ServerLevel level0 = server.getLevel(Backrooms.LEVEL_0);
        if (level0 != null) {
            for (ServerPlayer player : List.copyOf(level0.players())) {
                if (player.getY() < Maze.PIT_BOTTOM_Y && player.isAlive()) {
                    deeper(player);
                }
            }
        }
        if (PENDING.isEmpty()) {
            return;
        }
        for (UUID id : PENDING) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && player.isAlive()) {
                // Once the overworld has seen its first night through, sometimes you fall somewhere else entirely
                if (server.overworld().getDayTime() >= 24000L && player.getRandom().nextFloat() < FIELD_CHANCE) {
                    enterField(player);
                } else {
                    enter(player);
                }
            }
        }
        PENDING.clear();
    }

    public static void enter(ServerPlayer player) {
        ServerLevel level0 = ((ServerLevel) player.level()).getServer().getLevel(Backrooms.LEVEL_0);
        if (level0 == null) {
            return;
        }
        clearReturnTag(player);
        player.addTag(RETURN_TAG + player.getBlockX() + "," + player.getBlockZ());

        // Every cell's centre is open floor
        int x = Math.floorDiv(player.getBlockX(), Maze.CELL) * Maze.CELL + Maze.CELL / 2;
        int z = Math.floorDiv(player.getBlockZ(), Maze.CELL) * Maze.CELL + Maze.CELL / 2;
        player.teleport(new TeleportTransition(level0, new Vec3(x + 0.5, Maze.STAND_Y, z + 0.5), Vec3.ZERO,
                player.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160));
        level0.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.NOCLIP, SoundSource.PLAYERS, 1.0F, 0.5F);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Level 0").withStyle(ChatFormatting.YELLOW)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("You noclipped out of reality.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        SmilerSpawner.entered(player);
    }

    /** The Field: always arriving at the same spot, facing the house far off to the north. */
    public static void enterField(ServerPlayer player) {
        ServerLevel field = ((ServerLevel) player.level()).getServer().getLevel(Backrooms.THE_FIELD);
        if (field == null) {
            enter(player);
            return;
        }
        clearReturnTag(player);
        player.addTag(RETURN_TAG + player.getBlockX() + "," + player.getBlockZ());
        player.teleport(new TeleportTransition(field, new Vec3(0.5, Field.GROUND_Y + 1, 0.5), Vec3.ZERO,
                180.0F, 0.0F, TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("You've been here before.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    /**
     * Through an exit door in Level 0, deeper in: Level 1 or Level 2 at random. The way back to the overworld is
     * remembered from where the player first fell in.
     */
    public static void deeper(ServerPlayer player) {
        boolean one = player.getRandom().nextBoolean();
        ServerLevel next = ((ServerLevel) player.level()).getServer().getLevel(one ? Backrooms.LEVEL_1 : Backrooms.LEVEL_2);
        if (next == null) {
            leave(player);
            return;
        }
        // The middle of every cell is open floor on every level
        Layout layout = BackroomsChunkGenerator.layout(next.dimension());
        int x = layout.cellCenter(player.getBlockX());
        int z = layout.cellCenter(player.getBlockZ());
        player.teleport(new TeleportTransition(next, new Vec3(x + 0.5, Maze.STAND_Y, z + 0.5), Vec3.ZERO,
                player.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80));
        next.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.NOCLIP, SoundSource.PLAYERS, 1.0F, 0.4F);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(one ? "Level 1" : "Level 2")
                .withStyle(one ? ChatFormatting.GRAY : ChatFormatting.DARK_RED)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(one ? "Habitable Zone" : "Pipe Dreams")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        SmilerSpawner.entered(player);
    }

    public static void leave(ServerPlayer player) {
        ServerLevel overworld = ((ServerLevel) player.level()).getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        int x = 0;
        int z = 0;
        for (String tag : player.getTags()) {
            if (tag.startsWith(RETURN_TAG)) {
                String[] parts = tag.substring(RETURN_TAG.length()).split(",");
                try {
                    x = Integer.parseInt(parts[0]);
                    z = Integer.parseInt(parts[1]);
                } catch (RuntimeException ignored) {
                    // A damaged tag: fall back to the world origin
                }
            }
        }
        clearReturnTag(player);
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        player.teleport(new TeleportTransition(overworld, new Vec3(x + 0.5, y, z + 0.5), Vec3.ZERO,
                player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        overworld.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.NOCLIP, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.sendSystemMessage(Component.literal("You made it out... this time.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private static void clearReturnTag(ServerPlayer player) {
        for (String tag : List.copyOf(player.getTags())) {
            if (tag.startsWith(RETURN_TAG)) {
                player.removeTag(tag);
            }
        }
    }
}
