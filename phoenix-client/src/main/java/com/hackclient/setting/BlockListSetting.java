package com.hackclient.setting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Any number of blocks from the game, stored by id (e.g. "minecraft:diamond_ore"). */
public class BlockListSetting extends Setting<Set<String>> {
	private Set<Block> cache;

	public BlockListSetting(String name, Collection<String> defaultIds) {
		super(name, Set.copyOf(defaultIds));
		this.value = new LinkedHashSet<>(defaultIds);
	}

	public static String id(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	/** Fast check used while scanning the world. */
	public boolean contains(Block block) {
		if (cache == null) {
			cache = new HashSet<>();
			for (Block b : BuiltInRegistries.BLOCK) {
				if (value.contains(id(b))) cache.add(b);
			}
		}
		return cache.contains(block);
	}

	public boolean isEmpty() {
		return value.isEmpty();
	}

	public void toggle(Block block) {
		String id = id(block);
		if (!value.remove(id)) value.add(id);
		cache = null;
	}

	public void setAll(List<Block> blocks, boolean enabled) {
		for (Block block : blocks) {
			if (enabled) value.add(id(block));
			else value.remove(id(block));
		}
		cache = null;
	}

	@Override
	public void set(Set<String> value) {
		this.value = new LinkedHashSet<>(value);
		cache = null;
	}
}
