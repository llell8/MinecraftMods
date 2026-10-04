package com.hackclient.module.modules.player;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.lwjgl.glfw.GLFW;

/**
 * Longer reach for hitting entities and for breaking/placing blocks, by adding to your interaction range
 * attributes. Vanilla servers accept about +3 for entities and +1 for blocks beyond your normal range;
 * more than that only works in singleplayer or on servers that don't check.
 */
public class Reach extends Module {
	private static final Identifier MODIFIER = Identifier.fromNamespaceAndPath("hackclient", "reach");

	private final NumberSetting entityReach = number("Entity reach +", 3, 0, 10, 1);
	private final NumberSetting blockReach = number("Block reach +", 1, 0, 10, 1);

	public Reach() {
		super("Reach", "Hit entities and reach blocks from further away.", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		// Re-applied every tick, so it survives respawning and changing dimensions
		apply(Attributes.ENTITY_INTERACTION_RANGE, entityReach.get());
		apply(Attributes.BLOCK_INTERACTION_RANGE, blockReach.get());
	}

	@Override
	protected void onDisable() {
		if (mc.player == null) return;
		remove(Attributes.ENTITY_INTERACTION_RANGE);
		remove(Attributes.BLOCK_INTERACTION_RANGE);
	}

	private void apply(Holder<Attribute> attribute, double amount) {
		AttributeInstance instance = mc.player.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	private void remove(Holder<Attribute> attribute) {
		AttributeInstance instance = mc.player.getAttribute(attribute);
		if (instance != null) instance.removeModifier(MODIFIER);
	}
}
