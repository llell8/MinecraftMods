package com.meryx.creativestations.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edits the item in the selected hotbar slot: its enchantments (any level), name and lore.
 * Changes are applied with the creative "set slot" packet, so no server-side mod is needed.
 * Use '&' for colour codes, e.g. &6Golden name.
 */
public class ItemEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 310;
    private static final int LORE_LINES = 4;
    private static final int ROWS_PER_PAGE = 7;
    private static final int MAX_LEVEL = 255;

    private final Screen parent;
    private final ItemStack original;
    private final int slot;
    private final DataComponentType<ItemEnchantments> enchantComponent;
    private final List<Holder.Reference<Enchantment>> allEnchantments = new ArrayList<>();
    private final Map<Holder<Enchantment>, Integer> levels = new LinkedHashMap<>();
    private final String[] lore = new String[LORE_LINES];
    private String name;
    private int page;

    public ItemEditorScreen(Screen parent) {
        super(Component.translatable("creativestations.editor.title"));
        this.parent = parent;
        Minecraft mc = Minecraft.getInstance();
        this.slot = 36 + mc.player.getInventory().getSelectedSlot();
        this.original = mc.player.getInventory().getSelectedItem().copy();
        this.enchantComponent = original.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;

        mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(allEnchantments::add);
        allEnchantments.sort(Comparator.comparing(h -> h.value().description().getString()));

        ItemEnchantments existing = original.getOrDefault(enchantComponent, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : existing.keySet()) {
            levels.put(holder, existing.getLevel(holder));
        }

        Component customName = original.get(DataComponents.CUSTOM_NAME);
        this.name = customName == null ? "" : customName.getString();
        ItemLore itemLore = original.get(DataComponents.LORE);
        for (int i = 0; i < LORE_LINES; i++) {
            lore[i] = itemLore != null && i < itemLore.lines().size() ? itemLore.lines().get(i).getString() : "";
        }
    }

    @Override
    protected void init() {
        if (original.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("creativestations.editor.empty"), b -> onClose())
                    .bounds(width / 2 - 100, height / 2 - 10, 200, 20).build());
            return;
        }
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(8, (height - 224) / 2);

        // Left column: name and lore
        EditBox nameBox = new EditBox(font, left, top + 34, 150, 18, Component.translatable("creativestations.editor.name"));
        nameBox.setMaxLength(100);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("creativestations.editor.name"));
        nameBox.setResponder(value -> name = value);
        addRenderableWidget(nameBox);

        for (int i = 0; i < LORE_LINES; i++) {
            final int line = i;
            EditBox loreBox = new EditBox(font, left, top + 70 + i * 22, 150, 18, Component.translatable("creativestations.editor.lore"));
            loreBox.setMaxLength(200);
            loreBox.setValue(lore[i]);
            loreBox.setHint(Component.translatable("creativestations.editor.lore"));
            loreBox.setResponder(value -> lore[line] = value);
            addRenderableWidget(loreBox);
        }

        // Right column: enchantments, one page at a time
        int rightX = left + 160;
        int pages = (allEnchantments.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        page = Math.max(0, Math.min(page, pages - 1));
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            int index = page * ROWS_PER_PAGE + row;
            if (index >= allEnchantments.size()) {
                break;
            }
            Holder.Reference<Enchantment> holder = allEnchantments.get(index);
            int level = levels.getOrDefault(holder, 0);
            int y = top + 20 + row * 20;

            Component label = holder.value().description().copy();
            if (level > 0) {
                label = Component.empty().append(label).append(" " + level).withStyle(ChatFormatting.GOLD);
            }
            addRenderableWidget(Button.builder(Component.literal("-"), b -> setLevel(holder, level - 1))
                    .bounds(rightX, y, 20, 18).build());
            addRenderableWidget(Button.builder(label, b -> setLevel(holder, level > 0 ? 0 : 1))
                    .bounds(rightX + 22, y, 106, 18).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> setLevel(holder, level + 1))
                    .bounds(rightX + 130, y, 20, 18).build());
        }
        Button prev = Button.builder(Component.literal("<"), b -> {
            page--;
            rebuildWidgets();
        }).bounds(rightX, top + 20 + ROWS_PER_PAGE * 20 + 2, 20, 18).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(rightX + 130, top + 20 + ROWS_PER_PAGE * 20 + 2, 20, 18).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);

        int bottom = top + 20 + ROWS_PER_PAGE * 20 + 28;
        addRenderableWidget(Button.builder(Component.translatable("creativestations.editor.apply"), b -> {
            apply();
            onClose();
        }).bounds(left, bottom, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("creativestations.editor.cancel"), b -> onClose())
                .bounds(left + 160, bottom, 150, 20).build());
    }

    private void setLevel(Holder<Enchantment> holder, int level) {
        level = Math.max(0, Math.min(MAX_LEVEL, level));
        if (level == 0) {
            levels.remove(holder);
        } else {
            levels.put(holder, level);
        }
        rebuildWidgets();
    }

    private void apply() {
        ItemStack result = original.copy();

        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        levels.forEach(enchants::set);
        result.set(enchantComponent, enchants.toImmutable());

        if (name.isBlank()) {
            result.remove(DataComponents.CUSTOM_NAME);
        } else {
            result.set(DataComponents.CUSTOM_NAME, styled(name));
        }

        List<Component> loreLines = new ArrayList<>();
        for (String line : lore) {
            if (!line.isBlank()) {
                loreLines.add(styled(line));
            }
        }
        if (loreLines.isEmpty()) {
            result.remove(DataComponents.LORE);
        } else {
            result.set(DataComponents.LORE, new ItemLore(loreLines));
        }

        minecraft.gameMode.handleCreativeModeItemAdd(result, slot);
    }

    /** '&' colour codes, and no forced italics. */
    private static Component styled(String text) {
        return Component.literal(text.replace('&', '§')).withStyle(style -> style.withItalic(false));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(8, (height - 224) / 2);
        graphics.drawString(font, title, left, top + 4, 0xFFFFFFFF);
        if (!original.isEmpty()) {
            graphics.renderItem(original, left + 130, top);
            graphics.drawString(font, Component.translatable("creativestations.editor.name"), left, top + 24, 0xFFAAAAAA);
            graphics.drawString(font, Component.translatable("creativestations.editor.lore"), left, top + 60, 0xFFAAAAAA);
            graphics.drawString(font, Component.translatable("creativestations.editor.enchantments"), left + 160, top + 8, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
