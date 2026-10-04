package com.hackclient.module;

import com.hackclient.module.modules.combat.AimAssist;
import com.hackclient.module.modules.combat.AttributeSwap;
import com.hackclient.module.modules.combat.AutoTotem;
import com.hackclient.module.modules.combat.MaceKill;
import com.hackclient.module.modules.combat.SpearKill;
import com.hackclient.module.modules.combat.StunSlam;
import com.hackclient.module.modules.combat.Criticals;
import com.hackclient.module.modules.combat.KillAura;
import com.hackclient.module.modules.combat.TriggerBot;
import com.hackclient.module.modules.grinding.AutoEat;
import com.hackclient.module.modules.grinding.AutoFish;
import com.hackclient.module.modules.grinding.AutoTrade;
import com.hackclient.module.modules.grinding.ToolSaver;
import com.hackclient.module.modules.grinding.TradePreview;
import com.hackclient.module.modules.misc.ClickGui;
import com.hackclient.module.modules.movement.AutoWalk;
import com.hackclient.module.modules.movement.Flight;
import com.hackclient.module.modules.movement.Speed;
import com.hackclient.module.modules.movement.Spider;
import com.hackclient.module.modules.movement.Step;
import com.hackclient.module.modules.movement.Velocity;
import com.hackclient.module.modules.player.AutoRespawn;
import com.hackclient.module.modules.player.AutoTool;
import com.hackclient.module.modules.player.FastPlace;
import com.hackclient.module.modules.player.NoFall;
import com.hackclient.module.modules.render.ESP;
import com.hackclient.module.modules.render.Fullbright;
import com.hackclient.module.modules.render.BlockESP;
import com.hackclient.module.modules.render.NoHurtCam;
import com.hackclient.module.modules.render.Tracers;
import com.hackclient.module.modules.render.PlayerTracker;
import com.hackclient.module.modules.render.Zoom;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private final Set<Integer> heldKeys = new HashSet<>();

	public ModuleManager() {
		// Combat
		modules.add(new KillAura());
		modules.add(new AimAssist());
		modules.add(new TriggerBot());
		modules.add(new Criticals());
		modules.add(new AutoTotem());
		modules.add(new AttributeSwap());
		modules.add(new MaceKill());
		modules.add(new SpearKill());
		modules.add(new StunSlam());
		// Player
		modules.add(new NoFall());
		modules.add(new FastPlace());
		modules.add(new AutoTool());
		modules.add(new AutoRespawn());
		// Movement
		modules.add(new Flight());
		modules.add(new Speed());
		modules.add(new Step());
		modules.add(new Spider());
		modules.add(new AutoWalk());
		modules.add(new Velocity());
		// Render
		modules.add(new Fullbright());
		modules.add(new ESP());
		modules.add(new PlayerTracker());
		modules.add(new BlockESP());
		modules.add(new Tracers());
		modules.add(new Zoom());
		modules.add(new NoHurtCam());
		// Grinding
		modules.add(new TradePreview());
		modules.add(new AutoTrade());
		modules.add(new AutoFish());
		modules.add(new AutoEat());
		modules.add(new ToolSaver());
		// Misc
		modules.add(new ClickGui());
	}

	public void onTick(Minecraft mc) {
		handleKeybinds(mc);

		if (mc.player == null || mc.level == null) return;
		for (Module module : modules) {
			if (module.isEnabled()) module.onTick();
		}
	}

	/** Toggles a module on the tick its key is first pressed (only when no screen is open). */
	private void handleKeybinds(Minecraft mc) {
		for (Module module : modules) {
			int key = module.getKey();
			if (key == GLFW.GLFW_KEY_UNKNOWN) continue;

			boolean down = mc.screen == null && InputConstants.isKeyDown(mc.getWindow(), key);
			if (down && heldKeys.add(key)) {
				module.toggle();
			} else if (!down) {
				heldKeys.remove(key);
			}
		}
	}

	public List<Module> getModules() {
		return modules;
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> type) {
		for (Module module : modules) {
			if (module.getClass() == type) return (T) module;
		}
		return null;
	}

	/** Shortcut for mixins: returns the module if it is enabled, otherwise null. */
	public <T extends Module> T getIfEnabled(Class<T> type) {
		T module = get(type);
		return module != null && module.isEnabled() ? module : null;
	}

	public Module getByName(String name) {
		for (Module module : modules) {
			if (module.getName().equalsIgnoreCase(name)) return module;
		}
		return null;
	}
}
