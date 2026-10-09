package com.nullhours.haunt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nullhours.NullHoursConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** /nullhours: check and tune the haunting, or set off an event to try it out. */
public final class HauntCommand {
	private HauntCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nullhours")
				.then(Commands.literal("status").executes(HauntCommand::status))
				.then(Commands.literal("on").executes(ctx -> setEnabled(ctx, true)))
				.then(Commands.literal("off").executes(ctx -> setEnabled(ctx, false)))
				.then(Commands.literal("stage")
						.then(Commands.argument("stage", IntegerArgumentType.integer(-1, 3)).executes(HauntCommand::setStage)))
				.then(Commands.literal("frequency")
						.then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.1, 20)).executes(HauntCommand::setFrequency)))
				.then(Commands.literal("event")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(HauntEvents.EVENTS.keySet(), builder))
								.executes(HauntCommand::event))));
	}

	private static void reply(CommandContext<CommandSourceStack> ctx, String message) {
		ctx.getSource().sendSuccess(() -> Component.literal(message), false);
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		NullHoursConfig config = NullHoursConfig.INSTANCE;
		int stage = HauntDirector.stage(ctx.getSource().getLevel());
		reply(ctx, "Null Hours is " + (config.enabled ? "on" : "off") + ", stage " + stage
				+ (config.stageOverride >= 0 ? " (forced)" : " (from days played)") + ", frequency x" + config.eventFrequency);
		return 1;
	}

	private static int setEnabled(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		NullHoursConfig.INSTANCE.enabled = enabled;
		NullHoursConfig.save();
		reply(ctx, "Null Hours " + (enabled ? "enabled" : "disabled"));
		return 1;
	}

	private static int setStage(CommandContext<CommandSourceStack> ctx) {
		int stage = IntegerArgumentType.getInteger(ctx, "stage");
		NullHoursConfig.INSTANCE.stageOverride = stage;
		NullHoursConfig.save();
		reply(ctx, stage < 0 ? "Stage now follows the days played" : "Stage forced to " + stage);
		return 1;
	}

	private static int setFrequency(CommandContext<CommandSourceStack> ctx) {
		NullHoursConfig.INSTANCE.eventFrequency = DoubleArgumentType.getDouble(ctx, "multiplier");
		NullHoursConfig.save();
		reply(ctx, "Event frequency set to x" + NullHoursConfig.INSTANCE.eventFrequency);
		return 1;
	}

	private static int event(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String name = StringArgumentType.getString(ctx, "name");
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (!HauntEvents.EVENTS.containsKey(name)) {
			ctx.getSource().sendFailure(Component.literal("Unknown event. Options: " + String.join(", ", HauntEvents.EVENTS.keySet())));
			return 0;
		}
		if (!HauntEvents.run(name, (ServerLevel) player.level(), player)) {
			ctx.getSource().sendFailure(Component.literal("'" + name + "' can't happen here right now (needs darkness, room, torches or a door nearby, or another entity is still around)"));
			return 0;
		}
		return 1;
	}
}
