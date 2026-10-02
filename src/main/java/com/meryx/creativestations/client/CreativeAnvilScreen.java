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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Creative Anvil: pick any item (from your inventory, or search every item in the game) and edit its
 * enchantments (any level), name and lore. Applied with the creative "set slot" packet, so it works on any
 * server where you're in creative, and you never need to hold the item.
 */
public class CreativeAnvilScreen extends Screen {
    private static final int PW = 304;
    private static final int PH = 186;
    private static final int LORE_LINES = 4;
    private static final int ROWS = 6;
    private static final int MAX_LEVEL = 255;
    private static final int GRID_COLS = 9;
    private static final int SEARCH_LIMIT = 36;

    private enum Tab { ENCHANTS, ITEMS, INVENTORY }

    private record Cell(int x, int y, ItemStack stack, Button button) {
    }

    private final Screen parent;
    private final List<Holder.Reference<Enchantment>> allEnchantments = new ArrayList<>();
    private final Map<Holder<Enchantment>, Integer> levels = new LinkedHashMap<>();
    private final String[] lore = new String[LORE_LINES];
    private final List<Cell> cells = new ArrayList<>();
    private List<ItemStack> searchResults = new ArrayList<>();

    private ItemStack working = ItemStack.EMPTY;
    /** Inventory-menu slot the item came from, or -1 if it was picked from search (goes to a free slot). */
    private int sourceSlot = -1;
    private String name = "";
    private String query = "";
    private boolean refocusSearch;
    private Tab tab = Tab.INVENTORY;
    private int page;
    private int px;
    private int py;

    public CreativeAnvilScreen(Screen parent) {
        super(Component.translatable("creativestations.editor.title"));
        this.parent = parent;
        java.util.Arrays.fill(lore, "");
        Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .listElements().forEach(allEnchantments::add);
        allEnchantments.sort(Comparator.comparing(h -> h.value().description().getString()));
        updateSearch();
    }

    private DataComponentType<ItemEnchantments> enchantComponent() {
        return working.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }

    /** Loads an item into the editor and resets the fields from its data. */
    private void select(ItemStack stack, int slot) {
        working = stack.copy();
        sourceSlot = slot;
        levels.clear();
        ItemEnchantments existing = working.getOrDefault(enchantComponent(), ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : existing.keySet()) {
            levels.put(holder, existing.getLevel(holder));
        }
        Component customName = working.get(DataComponents.CUSTOM_NAME);
        name = customName == null ? "" : customName.getString();
        ItemLore itemLore = working.get(DataComponents.LORE);
        for (int i = 0; i < LORE_LINES; i++) {
            lore[i] = itemLore != null && i < itemLore.lines().size() ? itemLore.lines().get(i).getString() : "";
        }
        tab = Tab.ENCHANTS;
        page = 0;
        rebuildWidgets();
    }

    private void updateSearch() {
        String q = query.toLowerCase(Locale.ROOT).trim();
        searchResults = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ItemStack stack = item.getDefaultInstance();
            if (q.isEmpty() || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                    || BuiltInRegistries.ITEM.getKey(item).getPath().contains(q.replace(' ', '_'))) {
                searchResults.add(stack);
                if (searchResults.size() >= SEARCH_LIMIT) {
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = Math.max(4, (height - PH) / 2);
        cells.clear();

        // Left: the selected item's name and lore
        boolean has = !working.isEmpty();
        EditBox nameBox = new EditBox(font, px + 30, py + 18, 92, 18, Component.translatable("creativestations.editor.name"));
        nameBox.setMaxLength(100);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("creativestations.editor.name"));
        nameBox.setResponder(value -> name = value);
        nameBox.active = has;
        addRenderableWidget(nameBox);
        for (int i = 0; i < LORE_LINES; i++) {
            final int line = i;
            EditBox loreBox = new EditBox(font, px + 8, py + 42 + i * 20, 114, 18, Component.translatable("creativestations.editor.lore"));
            loreBox.setMaxLength(200);
            loreBox.setValue(lore[i]);
            loreBox.setHint(Component.translatable("creativestations.editor.lore"));
            loreBox.setResponder(value -> lore[line] = value);
            loreBox.active = has;
            addRenderableWidget(loreBox);
        }
        Button apply = Button.builder(Component.translatable("creativestations.editor.apply"), b -> {
            apply();
            onClose();
        }).bounds(px + 8, py + 128, 55, 20).build();
        apply.active = has;
        addRenderableWidget(apply);
        addRenderableWidget(Button.builder(Component.translatable("creativestations.editor.cancel"), b -> onClose())
                .bounds(px + 67, py + 128, 55, 20).build());

        // Right: tabs
        addTab(Tab.ENCHANTS, 0, "creativestations.editor.tab.enchants");
        addTab(Tab.ITEMS, 1, "creativestations.editor.tab.items");
        addTab(Tab.INVENTORY, 2, "creativestations.editor.tab.inventory");
        switch (tab) {
            case ENCHANTS -> initEnchants(has);
            case ITEMS -> initItems();
            case INVENTORY -> initInventory();
        }
    }

    private void addTab(Tab target, int index, String key) {
        Button button = Button.builder(Component.translatable(key), b -> {
            tab = target;
            rebuildWidgets();
        }).bounds(px + 130 + index * 54, py + 16, 54, 20).build();
        button.active = tab != target;
        addRenderableWidget(button);
    }

    private void initEnchants(boolean has) {
        if (!has) {
            return;
        }
        int pages = (allEnchantments.size() + ROWS - 1) / ROWS;
        page = Math.max(0, Math.min(page, pages - 1));
        for (int row = 0; row < ROWS; row++) {
            int index = page * ROWS + row;
            if (index >= allEnchantments.size()) {
                break;
            }
            Holder.Reference<Enchantment> holder = allEnchantments.get(index);
            int level = levels.getOrDefault(holder, 0);
            int y = py + 40 + row * 20;
            Component label = holder.value().description().copy();
            if (level > 0) {
                label = Component.empty().append(label).append(" " + level).withStyle(ChatFormatting.GOLD);
            }
            addRenderableWidget(Button.builder(Component.literal("-"), b -> setLevel(holder, level - 1))
                    .bounds(px + 130, y, 16, 18).build());
            addRenderableWidget(Button.builder(label, b -> setLevel(holder, level > 0 ? 0 : 1))
                    .bounds(px + 148, y, 112, 18).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> setLevel(holder, level + 1))
                    .bounds(px + 262, y, 16, 18).build());
        }
        Button prev = Button.builder(Component.literal("<"), b -> {
            page--;
            rebuildWidgets();
        }).bounds(px + 130, py + 162, 20, 18).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(px + 258, py + 162, 20, 18).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
    }

    private void initItems() {
        EditBox search = new EditBox(font, px + 130, py + 40, 164, 18, Component.translatable("creativestations.editor.search"));
        search.setMaxLength(50);
        search.setValue(query);
        search.setHint(Component.translatable("creativestations.editor.search"));
        search.setResponder(value -> {
            query = value;
            updateSearch();
            refocusSearch = true;
            rebuildWidgets();
        });
        addRenderableWidget(search);
        if (refocusSearch) {
            refocusSearch = false;
            setInitialFocus(search);
        }
        for (int i = 0; i < searchResults.size(); i++) {
            ItemStack stack = searchResults.get(i);
            addCell(px + 130 + (i % GRID_COLS) * 18, py + 62 + (i / GRID_COLS) * 18, stack, -1);
        }
    }

    private void initInventory() {
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            // Inventory index to creative-menu slot: hotbar 0-8 is 36-44, the rest keep their number
            int menuSlot = i < 9 ? 36 + i : i;
            int col = i % 9;
            int y = i < 9 ? py + 104 : py + 40 + ((i - 9) / 9) * 18;
            addCell(px + 130 + col * 18, y, stack, menuSlot);
        }
    }

    /** A clickable slot: an invisible button over a drawn slot, so clicks and focus just work. */
    private void addCell(int x, int y, ItemStack stack, int menuSlot) {
        Button button = Button.builder(Component.empty(), b -> {
            if (!stack.isEmpty()) {
                select(stack, menuSlot);
            }
        }).bounds(x, y, 18, 18).build();
        button.setAlpha(0.0F);
        addRenderableWidget(button);
        cells.add(new Cell(x, y, stack, button));
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
        ItemStack result = working.copy();

        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        levels.forEach(enchants::set);
        result.set(enchantComponent(), enchants.toImmutable());

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

        int target = sourceSlot;
        if (target < 0) {
            Inventory inv = minecraft.player.getInventory();
            int free = inv.getFreeSlot();
            int index = free >= 0 ? free : inv.getSelectedSlot();
            target = index < 9 ? 36 + index : index;
        }
        minecraft.gameMode.handleCreativeModeItemAdd(result, target);
    }

    /** '&' colour codes, and no forced italics. */
    private static Component styled(String text) {
        return Component.literal(text.replace('&', '§')).withStyle(style -> style.withItalic(false));
    }

    // ---- Drawing: vanilla-style grey panel with inset slots ----

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        panel(graphics, px, py, PW, PH);
        graphics.drawString(font, title, px + 8, py + 6, 0xFF404040, false);
        slot(graphics, px + 8, py + 18);
        for (Cell cell : cells) {
            slot(graphics, cell.x(), cell.y());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        if (!working.isEmpty()) {
            graphics.renderItem(working, px + 9, py + 19);
        } else {
            graphics.drawString(font, Component.translatable("creativestations.editor.pick"), px + 8, py + 154, 0xFF404040, false);
        }
        graphics.drawString(font, Component.translatable("creativestations.editor.colors"), px + 8, py + 172, 0xFF707070, false);

        ItemStack hovered = ItemStack.EMPTY;
        for (Cell cell : cells) {
            if (!cell.stack().isEmpty()) {
                graphics.renderItem(cell.stack(), cell.x() + 1, cell.y() + 1);
                graphics.renderItemDecorations(font, cell.stack(), cell.x() + 1, cell.y() + 1);
            }
            if (mouseX >= cell.x() && mouseX < cell.x() + 18 && mouseY >= cell.y() && mouseY < cell.y() + 18) {
                graphics.fill(cell.x() + 1, cell.y() + 1, cell.x() + 17, cell.y() + 17, 0x80FFFFFF);
                hovered = cell.stack();
            }
        }
        if (!hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
