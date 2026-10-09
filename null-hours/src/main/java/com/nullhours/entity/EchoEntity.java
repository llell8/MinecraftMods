package com.nullhours.entity;

import com.nullhours.NullHoursConfig;
import com.nullhours.net.ScarePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Looks like a player with empty black eyes. It only moves while you are not looking at
 * it, and it talks to you in chat. Walk backwards while watching it, or get far enough
 * away, and it gives up.
 */
public class EchoEntity extends HauntEntity {
	public static final String NAME = "Echo";
	private static final String[] LINES = {
			"i can see you", "why are you running", "this is my world", "dont look away",
			"you left the door open", "i was here first", "closer", "turn around"
	};

	private int nextLine = 200;

	public EchoEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	public static void say(ServerPlayer player, String line) {
		if (NullHoursConfig.INSTANCE.fakeChat) player.sendSystemMessage(Component.literal("<" + NAME + "> " + line));
	}

	public static void joinMessage(ServerPlayer player) {
		if (NullHoursConfig.INSTANCE.fakeChat) {
			player.sendSystemMessage(Component.translatable("multiplayer.player.joined", NAME).withStyle(ChatFormatting.YELLOW));
		}
	}

	@Override
	protected int lifetime() {
		return 20 * 120;
	}

	@Override
	protected void hauntTick(ServerLevel level, ServerPlayer victim) {
		if (--nextLine <= 0) {
			say(victim, LINES[getRandom().nextInt(LINES.length)]);
			nextLine = 400 + getRandom().nextInt(600);
		}
		if (isSeenBy(victim, 0.5)) {
			// Frozen in place while watched
			getNavigation().stop();
			setDeltaMovement(0, getDeltaMovement().y, 0);
			lookAtVictim(victim);
			return;
		}
		if (distanceTo(victim) > 40.0) {
			say(victim, "next time");
			vanish(level);
			return;
		}
		if (distanceTo(victim) < 1.8) {
			jumpscare(level, victim, ScarePayload.FACE_ECHO);
			return;
		}
		getNavigation().moveTo(victim, 1.3);
	}

	@Override
	protected void onHit(ServerLevel level, ServerPlayer victim) {
		say(victim, "that wont work");
		vanish(level);
	}
}
