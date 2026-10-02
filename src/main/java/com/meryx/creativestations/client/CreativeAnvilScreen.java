package com.meryx.creativestations.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Creative Anvil, laid out like the vanilla anvil: input slot, arrow, output slot on the left, icon tabs,
 * a brown list on the right, and your inventory underneath. Pick any item (from your inventory or by searching
 * every item in the game) and edit its enchantments (any level), name and lore. Click the output slot to apply.
 * Uses the creative "set slot" packet, so it works on any server where you're in creative.
 */
public class CreativeAnvilScreen extends Screen {
    private static final int PW = 256;
    private static final int PH = 222;
    private static final int LORE_LINES = 4;
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_H = 20;
    private static final int MAX_LEVEL = 255;
    private static final int SEARCH_LIMIT = 36;

    // Colours taken from the vanilla anvil's look
    private static final int LIST_DARK = 0xFF2B2216;
    private static final int LIST_ROW_A = 0xFF5E4B32;
    private static final int LIST_ROW_B = 0xFF4F3F2A;
    private static final int TAN = 0xFFB39B6E;
    private static final int TAN_LIGHT = 0xFFD9C79A;
    private static final int RED = 0xFFD2603A;
    private static final int RED_LIGHT = 0xFFF08A63;
    private static final int GREY_BTN = 0xFF8B7B66;
    private static final int TAB_BLUE = 0xFF7A8FB5;
    private static final int TAB_BLUE_DARK = 0xFF4A5A78;
    private static final int TAB_ON = 0xFFA9BCDD;

    private enum Tab {
        NAME(Items.OAK_SIGN, "creativestations.editor.tab.name"),
        ENCHANTS(Items.ENCHANTED_BOOK, "creativestations.editor.tab.enchants"),
        ITEMS(Items.COMPASS, "creativestations.editor.tab.items");

        final Item icon;
        final String key;

        Tab(Item icon, String key) {
            this.icon = icon;
            this.key = key;
        }
    }

    /** A rectangle drawn in the background layer, with an optional centred label drawn on top. */
    private record Box(int x, int y, int w, int h, int fill, int light, String label, int textColor) {
    }

    private record Cell(int x, int y, ItemStack stack) {
    }

    private final Screen parent;
    private final List<Holder.Reference<Enchantment>> allEnchantments = new ArrayList<>();
    private final Map<Holder<Enchantment>, Integer> levels = new LinkedHashMap<>();
    private final String[] lore = new String[LORE_LINES];
    private final List<Box> boxes = new ArrayList<>();
    private final List<Cell> cells = new ArrayList<>();
    private final List<Button> hits = new ArrayList<>();
    private List<ItemStack> searchResults = new ArrayList<>();

    private ItemStack working = ItemStack.EMPTY;
    /** Inventory-menu slot the item came from, or -1 if it was picked from search (goes to a free slot). */
    private int sourceSlot = -1;
    private String name = "";
    private String query = "";
    private boolean refocusSearch;
    private Tab tab = Tab.ENCHANTS;
    private int scroll;
    private int px;
    private int py;

    public CreativeAnvilScreen(Screen parent) {
        super(Component.translatable("creativestations.editor.title"));
        this.parent = parent;
        Arrays.fill(lore, "");
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
        if (tab == Tab.ITEMS) {
            tab = Tab.ENCHANTS;
        }
        scroll = 0;
        rebuildWidgets();
    }

    private void clearSelection() {
        working = ItemStack.EMPTY;
        sourceSlot = -1;
        levels.clear();
        name = "";
        Arrays.fill(lore, "");
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

    /** What the item will look like once applied. */
    private ItemStack buildResult() {
        if (working.isEmpty()) {
            return ItemStack.EMPTY;
        }
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
        return result;
    }

    private void apply() {
        ItemStack result = buildResult();
        if (result.isEmpty()) {
            return;
        }
        int target = sourceSlot;
        if (target < 0) {
            Inventory inv = minecraft.player.getInventory();
            int free = inv.getFreeSlot();
            int index = free >= 0 ? free : inv.getSelectedSlot();
            target = index < 9 ? 36 + index : index;
        }
        minecraft.gameMode.handleCreativeModeItemAdd(result, target);
        onClose();
    }

    /** '&' colour codes, and no forced italics. */
    private static Component styled(String text) {
        return Component.literal(text.replace('&', '§')).withStyle(style -> style.withItalic(false));
    }

    // ---- Layout ----

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = Math.max(4, (height - PH) / 2);
        boxes.clear();
        cells.clear();
        hits.clear();
        boolean has = !working.isEmpty();

        // Left: input slot (click to clear) and output slot (click to apply)
        hit(px + 8, py + 18, 18, 18, () -> {
            if (has) {
                clearSelection();
            }
        }, "creativestations.editor.input");
        hit(px + 8, py + 62, 18, 18, this::apply, "creativestations.editor.output");

        // Tabs, drawn like the vanilla anvil's icon tabs
        for (Tab t : Tab.values()) {
            int y = py + 18 + t.ordinal() * 24;
            boolean on = t == tab;
            boxes.add(new Box(px + 32, y, 24, 22, on ? TAB_ON : TAB_BLUE, on ? 0xFFFFFFFF : TAB_BLUE_DARK, null, 0));
            hit(px + 32, y, 24, 22, () -> {
                tab = t;
                scroll = 0;
                rebuildWidgets();
            }, t.key);
        }

        // The brown list
        boxes.add(new Box(px + 60, py + 18, 184, VISIBLE_ROWS * ROW_H, LIST_ROW_A, LIST_DARK, null, 0));
        switch (tab) {
            case NAME -> initName(has);
            case ENCHANTS -> initEnchants(has);
            case ITEMS -> initItems();
        }

        // Inventory, always shown underneath, like the vanilla screen
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            int menuSlot = i < 9 ? 36 + i : i;
            int col = i % 9;
            int y = i < 9 ? py + 196 : py + 138 + ((i - 9) / 9) * 18;
            addCell(px + 47 + col * 18, y, stack, () -> select(stack, menuSlot), false);
        }
    }

    private void initName(boolean has) {
        EditBox nameBox = new EditBox(font, px + 64, py + 21, 176, 16, Component.translatable("creativestations.editor.name"));
        nameBox.setMaxLength(100);
        nameBox.setValue(name);
        nameBox.setHint(Component.translatable("creativestations.editor.name"));
        nameBox.setResponder(value -> name = value);
        nameBox.active = has;
        addRenderableWidget(nameBox);
        for (int i = 0; i < LORE_LINES; i++) {
            final int line = i;
            EditBox loreBox = new EditBox(font, px + 64, py + 21 + (i + 1) * ROW_H, 176, 16, Component.translatable("creativestations.editor.lore"));
            loreBox.setMaxLength(200);
            loreBox.setValue(lore[i]);
            loreBox.setHint(Component.translatable("creativestations.editor.lore"));
            loreBox.setResponder(value -> lore[line] = value);
            loreBox.active = has;
            addRenderableWidget(loreBox);
        }
    }

    private void initEnchants(boolean has) {
        if (!has) {
            return;
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, allEnchantments.size() - VISIBLE_ROWS)));
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= allEnchantments.size()) {
                break;
            }
            Holder.Reference<Enchantment> holder = allEnchantments.get(index);
            int level = levels.getOrDefault(holder, 0);
            int y = py + 18 + row * ROW_H;
            boxes.add(new Box(px + 60, y, 184, ROW_H, row % 2 == 0 ? LIST_ROW_A : LIST_ROW_B, 0, null, 0));
            // Red X removes it
            boxes.add(new Box(px + 62, y + 1, 18, 18, level > 0 ? RED : GREY_BTN, level > 0 ? RED_LIGHT : TAN_LIGHT, "X", 0xFFFFFFFF));
            hit(px + 62, y + 1, 18, 18, () -> setLevel(holder, 0), null);
            // Click the name to toggle it on at level 1
            hit(px + 82, y + 1, 98, 18, () -> setLevel(holder, level > 0 ? 0 : 1), null);
            // - level +
            boxes.add(new Box(px + 182, y + 1, 14, 18, TAN, TAN_LIGHT, "-", 0xFF3B2F20));
            hit(px + 182, y + 1, 14, 18, () -> setLevel(holder, level - 1), null);
            boxes.add(new Box(px + 198, y + 1, 22, 18, TAN, TAN_LIGHT, String.valueOf(level), 0xFFFFFFFF));
            boxes.add(new Box(px + 222, y + 1, 14, 18, TAN, TAN_LIGHT, "+", 0xFF3B2F20));
            hit(px + 222, y + 1, 14, 18, () -> setLevel(holder, level + 1), null);
        }
    }

    private void initItems() {
        EditBox search = new EditBox(font, px + 64, py + 21, 176, 16, Component.translatable("creativestations.editor.search"));
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
            addCell(px + 64 + (i % 9) * 18, py + 42 + (i / 9) * 18, stack, () -> select(stack, -1), true);
        }
    }

    /** An invisible button over an area, so clicks and focus are handled by vanilla. */
    private Button hit(int x, int y, int w, int h, Runnable action, String tooltipKey) {
        Button button = Button.builder(Component.empty(), b -> action.run()).bounds(x, y, w, h).build();
        button.setAlpha(0.0F);
        if (tooltipKey != null) {
            button.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        }
        addRenderableWidget(button);
        hits.add(button);
        return button;
    }

    private void addCell(int x, int y, ItemStack stack, Runnable onClick, boolean inList) {
        hit(x, y, 18, 18, () -> {
            if (!stack.isEmpty()) {
                onClick.run();
            }
        }, null);
        cells.add(new Cell(x, y, stack));
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            int max = Math.max(0, allEnchantments.size() - VISIBLE_ROWS);
            int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
            if (next != scroll) {
                scroll = next;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---- Drawing ----

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

    private static void arrow(GuiGraphics g, int x, int y) {
        int color = 0xFF8B8B8B;
        g.fill(x + 4, y, x + 10, y + 10, color);
        for (int i = 0; i < 7; i++) {
            g.fill(x + i, y + 10 + i, x + 14 - i, y + 11 + i, color);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        panel(graphics, px, py, PW, PH);
        graphics.drawString(font, title, px + 8, py + 6, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("container.inventory"), px + 47, py + 127, 0xFF404040, false);

        slot(graphics, px + 8, py + 18);
        arrow(graphics, px + 10, py + 40);
        slot(graphics, px + 8, py + 62);

        for (Box box : boxes) {
            graphics.fill(box.x(), box.y(), box.x() + box.w(), box.y() + box.h(), box.fill());
            if (box.light() != 0) {
                graphics.fill(box.x(), box.y(), box.x() + box.w(), box.y() + 1, box.light());
                graphics.fill(box.x(), box.y(), box.x() + 1, box.y() + box.h(), box.light());
            }
        }
        // List border, scrollbar
        graphics.fill(px + 59, py + 17, px + 245, py + 18, LIST_DARK);
        graphics.fill(px + 59, py + 18 + VISIBLE_ROWS * ROW_H, px + 245, py + 19 + VISIBLE_ROWS * ROW_H, LIST_DARK);
        int trackX = px + 246;
        int trackH = VISIBLE_ROWS * ROW_H;
        graphics.fill(trackX, py + 18, trackX + 8, py + 18 + trackH, 0xFF8B8B8B);
        int total = Math.max(VISIBLE_ROWS, allEnchantments.size());
        int thumbH = tab == Tab.ENCHANTS ? Math.max(12, trackH * VISIBLE_ROWS / total) : trackH;
        int thumbY = tab == Tab.ENCHANTS && total > VISIBLE_ROWS
                ? py + 18 + (trackH - thumbH) * scroll / (total - VISIBLE_ROWS) : py + 18;
        graphics.fill(trackX, thumbY, trackX + 8, thumbY + thumbH, 0xFFFFFFFF);
        graphics.fill(trackX + 1, thumbY + 1, trackX + 8, thumbY + thumbH, 0xFF555555);
        graphics.fill(trackX + 1, thumbY + 1, trackX + 7, thumbY + thumbH - 1, 0xFFC6C6C6);

        for (Cell cell : cells) {
            slot(graphics, cell.x(), cell.y());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        // Box labels (X, -, level, +)
        for (Box box : boxes) {
            if (box.label() != null) {
                int tx = box.x() + (box.w() - font.width(box.label())) / 2;
                int ty = box.y() + (box.h() - 8) / 2;
                graphics.drawString(font, box.label(), tx, ty, box.textColor(), false);
            }
        }
        // Enchantment names in the list
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            for (int row = 0; row < VISIBLE_ROWS && scroll + row < allEnchantments.size(); row++) {
                Holder.Reference<Enchantment> holder = allEnchantments.get(scroll + row);
                String text = font.plainSubstrByWidth(holder.value().description().getString(), 94);
                int color = levels.containsKey(holder) ? 0xFFFFFFFF : 0xFFA0A0A0;
                graphics.drawString(font, text, px + 84, py + 18 + row * ROW_H + 6, color, true);
            }
        } else if (tab == Tab.ENCHANTS) {
            graphics.drawString(font, Component.translatable("creativestations.editor.pick"), px + 66, py + 62, 0xFFFFFFFF, true);
        }
        for (Tab t : Tab.values()) {
            graphics.renderItem(new ItemStack(t.icon), px + 36, py + 21 + t.ordinal() * 24);
        }

        // Input, output and the grid items
        ItemStack result = buildResult();
        if (!working.isEmpty()) {
            graphics.renderItem(working, px + 9, py + 19);
            graphics.renderItem(result, px + 9, py + 63);
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (Cell cell : cells) {
            if (!cell.stack().isEmpty()) {
                graphics.renderItem(cell.stack(), cell.x() + 1, cell.y() + 1);
                graphics.renderItemDecorations(font, cell.stack(), cell.x() + 1, cell.y() + 1);
            }
        }
        for (Button hit : hits) {
            if (!hit.isHovered()) {
                continue;
            }
            int x = hit.getX();
            int y = hit.getY();
            if (hit.getWidth() == 18 && hit.getHeight() == 18) {
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
            }
            if (x == px + 8 && y == py + 18) {
                hovered = working;
            } else if (x == px + 8 && y == py + 62) {
                hovered = result;
            }
        }
        for (Cell cell : cells) {
            if (mouseX >= cell.x() && mouseX < cell.x() + 18 && mouseY >= cell.y() && mouseY < cell.y() + 18
                    && !cell.stack().isEmpty()) {
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
