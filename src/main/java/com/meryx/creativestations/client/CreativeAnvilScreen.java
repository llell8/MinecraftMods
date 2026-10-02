package com.meryx.creativestations.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
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
 * The Creative Anvil. Built from vanilla textures: the anvil's frame and inventory, the creative scroller,
 * and vanilla button sprites for the tabs. Pick any item (from your inventory or by searching every item)
 * and edit its enchantments (any level), name and lore; click the output slot to apply.
 * Uses the creative "set slot" packet, so it works on any server where you're in creative.
 */
public class CreativeAnvilScreen extends Screen {
    private static final Identifier ANVIL_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/anvil.png");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");

    // Overall size: wider than vanilla so enchantment names fit, inventory centred underneath
    private static final int PW = 230;
    private static final int TOP_H = 116;
    private static final int PH = TOP_H + 90;

    // The list on the right
    private static final int LX = 51;
    private static final int LY = 16;
    private static final int LW = PW - LX - 23;
    private static final int TRACK_X = LX + LW + 2;
    /** Left edge of the 9x4 inventory slots (the slot border, items sit one pixel in). */
    private static final int INV_X = (PW - 162) / 2;
    private static final int ROW_H = 18;
    private static final int ROWS = 5;
    private static final int LH = ROWS * ROW_H;
    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 4;

    // Enchantment row columns: x | name | - level +
    private static final int PLUS_X = LX + LW - 11;
    private static final int LEVEL_X = PLUS_X - 19;
    private static final int MINUS_X = LEVEL_X - 11;
    private static final int NAME_W = MINUS_X - (LX + 15) - 2;

    private static final int LORE_LINES = 4;
    private static final int MAX_LEVEL = 255;

    private static final int LIST_BORDER = 0xFF2B2216;
    private static final int ROW_A = 0xFF5E4B32;
    private static final int ROW_B = 0xFF52412B;
    private static final int TAN = 0xFFB39B6E;
    private static final int TAN_LIGHT = 0xFFD9C79A;
    private static final int TAN_DARK = 0xFF7F6C48;
    private static final int RED = 0xFFC8553A;
    private static final int RED_LIGHT = 0xFFEE8462;
    private static final int RED_DARK = 0xFF7E2E1D;
    private static final int OFF = 0xFF6E604D;
    private static final int LABEL = 0xFF404040;

    private enum Tab {
        ENCHANTS(Items.ENCHANTED_BOOK, "creativestations.editor.tab.enchants"),
        NAME(Items.OAK_SIGN, "creativestations.editor.tab.name"),
        ITEMS(Items.COMPASS, "creativestations.editor.tab.items");

        final Item icon;
        final String key;

        Tab(Item icon, String key) {
            this.icon = icon;
            this.key = key;
        }
    }

    private record Cell(int x, int y, ItemStack stack, boolean drawSlot) {
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
    private Tab tab = Tab.ENCHANTS;
    private int enchantScroll;
    private int itemScroll;
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

    // ---- Editing ----

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
        enchantScroll = 0;
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
            }
        }
        itemScroll = 0;
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

    // ---- Widgets ----

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = Math.max(2, (height - PH) / 2);
        cells.clear();
        boolean has = !working.isEmpty();

        // Input slot (click to clear) and output slot (click to apply)
        hit(7, 16, 18, 18, () -> {
            if (has) {
                clearSelection();
            }
        }, Component.translatable("creativestations.editor.input"));
        hit(7, 56, 18, 18, this::apply, null);

        // Tabs
        for (Tab t : Tab.values()) {
            hit(28, 16 + t.ordinal() * 24, 22, 22, () -> {
                tab = t;
                rebuildWidgets();
            }, Component.translatable(t.key));
        }

        switch (tab) {
            case ENCHANTS -> initEnchants(has);
            case NAME -> initName(has);
            case ITEMS -> initItems();
        }

        // Inventory, in the same places as the vanilla anvil's slots
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            int menuSlot = i < 9 ? 36 + i : i;
            int x = INV_X + 1 + (i % 9) * 18;
            int y = i < 9 ? TOP_H + 66 : TOP_H + 8 + ((i - 9) / 9) * 18;
            addCell(x, y, stack, () -> select(stack, menuSlot), false);
        }
    }

    private void initEnchants(boolean has) {
        if (!has) {
            return;
        }
        enchantScroll = clampScroll(enchantScroll, allEnchantments.size(), ROWS);
        for (int row = 0; row < ROWS && enchantScroll + row < allEnchantments.size(); row++) {
            Holder.Reference<Enchantment> holder = allEnchantments.get(enchantScroll + row);
            int level = levels.getOrDefault(holder, 0);
            int y = LY + row * ROW_H;
            Component fullName = holder.value().description();

            if (level > 0) {
                hit(LX + 1, y + 1, 12, 16, () -> setLevel(holder, 0),
                        Component.translatable("creativestations.editor.remove"));
            }
            hit(LX + 15, y + 1, NAME_W, 16, () -> setLevel(holder, level > 0 ? 0 : 1),
                    Component.empty().append(fullName).append(Component.translatable("creativestations.editor.max", holder.value().getMaxLevel())));
            addRenderableWidget(Button.builder(Component.literal("-"), b -> setLevel(holder, level - 1))
                    .bounds(px + MINUS_X, py + y + 1, 10, 16).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> setLevel(holder, level + 1))
                    .bounds(px + PLUS_X, py + y + 1, 10, 16).build());
        }
    }

    private void initName(boolean has) {
        for (int i = 0; i <= LORE_LINES; i++) {
            final int line = i - 1;
            boolean isName = i == 0;
            Component hint = Component.translatable(isName ? "creativestations.editor.name" : "creativestations.editor.lore");
            EditBox box = new EditBox(font, px + LX + 2, py + LY + 1 + i * ROW_H, LW - 4, 16, hint);
            box.setMaxLength(isName ? 100 : 200);
            box.setValue(isName ? name : lore[line]);
            box.setHint(hint);
            box.setResponder(value -> {
                if (isName) {
                    name = value;
                } else {
                    lore[line] = value;
                }
            });
            box.active = has;
            box.setEditable(has);
            addRenderableWidget(box);
        }
    }

    private void initItems() {
        EditBox search = new EditBox(font, px + LX + 2, py + LY + 1, LW - 4, 16, Component.translatable("creativestations.editor.search"));
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
        int totalRows = (searchResults.size() + GRID_COLS - 1) / GRID_COLS;
        itemScroll = clampScroll(itemScroll, totalRows, GRID_ROWS);
        int gridX = LX + (LW - GRID_COLS * 18) / 2 + 1;
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                int index = (itemScroll + r) * GRID_COLS + c;
                if (index >= searchResults.size()) {
                    break;
                }
                ItemStack stack = searchResults.get(index);
                addCell(gridX + c * 18, LY + ROW_H + r * 18, stack, () -> select(stack, -1), true);
            }
        }
    }

    private static int clampScroll(int scroll, int total, int visible) {
        return Math.max(0, Math.min(scroll, Math.max(0, total - visible)));
    }

    /** An invisible button over an area (relative to the window), so vanilla handles clicks and tooltips. */
    private void hit(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button button = Button.builder(Component.empty(), b -> action.run()).bounds(px + x, py + y, w, h).build();
        button.setAlpha(0.0F);
        if (tooltip != null) {
            button.setTooltip(Tooltip.create(tooltip));
        }
        addRenderableWidget(button);
    }

    /** A clickable item. {@code x, y} is where the item is drawn; the 18x18 slot sits one pixel up and left. */
    private void addCell(int x, int y, ItemStack stack, Runnable onClick, boolean drawSlot) {
        hit(x - 1, y - 1, 18, 18, () -> {
            if (!stack.isEmpty()) {
                onClick.run();
            }
        }, null);
        cells.add(new Cell(x, y, stack, drawSlot));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) -Math.signum(scrollY);
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            int next = clampScroll(enchantScroll + step, allEnchantments.size(), ROWS);
            if (next != enchantScroll) {
                enchantScroll = next;
                rebuildWidgets();
            }
            return true;
        }
        if (tab == Tab.ITEMS) {
            int totalRows = (searchResults.size() + GRID_COLS - 1) / GRID_COLS;
            int next = clampScroll(itemScroll + step, totalRows, GRID_ROWS);
            if (next != itemScroll) {
                itemScroll = next;
                refocusSearch = true;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---- Drawing ----

    private boolean over(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= px + x && mouseX < px + x + w && mouseY >= py + y && mouseY < py + y + h;
    }

    private void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(px + x, py + y, px + x + w, py + y + h, color);
    }

    /** A raised box with a light top-left edge and dark bottom-right edge. */
    private void bevel(GuiGraphics g, int x, int y, int w, int h, int color, int light, int dark) {
        fill(g, x, y, w, h, dark);
        fill(g, x, y, w - 1, h - 1, light);
        fill(g, x + 1, y + 1, w - 2, h - 2, color);
    }

    /** Draws the anvil texture's 4px border around a window of any size. */
    private void frame(GuiGraphics g) {
        int c = 4;
        int tw = 176;
        int th = 166;
        int x2 = px + PW - c;
        int y2 = py + PH - c;
        // Corners
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px, py, 0, 0, c, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, x2, py, tw - c, 0, c, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px, y2, 0, th - c, c, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, x2, y2, tw - c, th - c, c, c, 256, 256);
        // Edges, stretched from a single pixel row/column
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px + c, py, c, 0, PW - 2 * c, c, 1, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px + c, y2, c, th - c, PW - 2 * c, c, 1, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px, py + c, 0, c, c, PH - 2 * c, c, 1, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, x2, py + c, tw - c, c, c, PH - 2 * c, c, 1, 256, 256);
        // Body
        g.fill(px + c, py + c, x2, y2, 0xFFC6C6C6);
    }

    /** A vanilla inventory slot: dark top-left, white bottom-right. */
    private void slot(GuiGraphics g, int x, int y) {
        fill(g, x, y, 18, 18, 0xFF373737);
        fill(g, x + 1, y + 1, 17, 17, 0xFFFFFFFF);
        fill(g, x + 1, y + 1, 16, 16, 0xFF8B8B8B);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color, boolean shadow) {
        g.drawString(font, s, px + x, py + y, color, shadow);
    }

    private void centred(GuiGraphics g, String s, int x, int y, int w, int h, int color) {
        text(g, s, x + (w - font.width(s)) / 2, y + (h - 8) / 2 + 1, color, false);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);

        // Window: the vanilla anvil's frame (corners and stretched edges), then its inventory slots, centred
        frame(g);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px + INV_X, py + TOP_H + 7, 7, 83, 162, 76, 256, 256);

        // Input -> arrow -> output
        slot(g, 7, 16);
        int arrow = 0xFF8B8B8B;
        fill(g, 13, 37, 6, 9, arrow);
        for (int i = 0; i < 6; i++) {
            fill(g, 10 + i, 46 + i, 12 - 2 * i, 1, arrow);
        }
        slot(g, 7, 56);

        // Tabs, using the vanilla button sprite
        for (Tab t : Tab.values()) {
            int y = 16 + t.ordinal() * 24;
            Identifier sprite = t == tab ? BUTTON_DISABLED
                    : over(mouseX, mouseY, 28, y, 22, 22) ? BUTTON_HIGHLIGHTED : BUTTON;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, px + 28, py + y, 22, 22);
        }

        // The list
        fill(g, LX - 1, LY - 1, LW + 2, LH + 2, LIST_BORDER);
        for (int row = 0; row < ROWS; row++) {
            fill(g, LX, LY + row * ROW_H, LW, ROW_H, row % 2 == 0 ? ROW_A : ROW_B);
        }
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            for (int row = 0; row < ROWS && enchantScroll + row < allEnchantments.size(); row++) {
                Holder.Reference<Enchantment> holder = allEnchantments.get(enchantScroll + row);
                boolean on = levels.containsKey(holder);
                int y = LY + row * ROW_H;
                if (on) {
                    bevel(g, LX + 1, y + 1, 12, 16, RED, RED_LIGHT, RED_DARK);
                }
                bevel(g, LEVEL_X, y + 1, 18, 16, on ? TAN : TAN_DARK, on ? TAN_LIGHT : OFF, LIST_BORDER);
            }
        }
        for (Cell cell : cells) {
            if (cell.drawSlot()) {
                slot(g, cell.x() - 1, cell.y() - 1);
            }
        }

        // Scrollbar: an inset track with the creative inventory's scroller
        int trackX = TRACK_X;
        fill(g, trackX - 1, LY - 1, 14, LH + 2, 0xFF373737);
        fill(g, trackX, LY, 13, LH + 1, 0xFFFFFFFF);
        fill(g, trackX, LY, 12, LH, 0xFF8B8B8B);
        int total = 0;
        int visible = 1;
        int pos = 0;
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            total = allEnchantments.size();
            visible = ROWS;
            pos = enchantScroll;
        } else if (tab == Tab.ITEMS) {
            total = (searchResults.size() + GRID_COLS - 1) / GRID_COLS;
            visible = GRID_ROWS;
            pos = itemScroll;
        }
        boolean canScroll = total > visible;
        int thumbY = canScroll ? LY + (LH - 15) * pos / (total - visible) : LY;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, canScroll ? SCROLLER : SCROLLER_DISABLED, px + trackX, py + thumbY, 12, 15);

        text(g, title.getString(), 8, 6, LABEL, false);
        g.drawString(font, Component.translatable("container.inventory"), px + INV_X + 1, py + TOP_H - 4, LABEL, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        // Enchantment rows: X, name, level
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            for (int row = 0; row < ROWS && enchantScroll + row < allEnchantments.size(); row++) {
                Holder.Reference<Enchantment> holder = allEnchantments.get(enchantScroll + row);
                int level = levels.getOrDefault(holder, 0);
                int y = LY + row * ROW_H;
                if (level > 0) {
                    centred(g, "x", LX + 1, y + 1, 12, 16, 0xFFFFFFFF);
                }
                String full = holder.value().description().getString();
                String label = font.width(full) <= NAME_W ? full : font.plainSubstrByWidth(full, NAME_W - font.width("..")) + "..";
                text(g, label, LX + 15, y + 5, level > 0 ? 0xFFFFFFFF : 0xFFB0A590, true);
                centred(g, level > 0 ? String.valueOf(level) : "-", LEVEL_X, y + 1, 18, 16, level > 0 ? 0xFF3B2F20 : 0xFF8A7C62);
            }
        } else if (tab == Tab.ENCHANTS || (tab == Tab.NAME && working.isEmpty())) {
            Component hint = Component.translatable("creativestations.editor.pick");
            List<FormattedCharSequence> lines = font.split(hint, LW - 8);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), px + LX + 4, py + LY + 30 + i * 10, 0xFFE0D6C0, true);
            }
        }

        // Tab icons
        for (Tab t : Tab.values()) {
            g.renderItem(new ItemStack(t.icon), px + 31, py + 19 + t.ordinal() * 24);
        }

        // Input, output and every item cell
        ItemStack result = buildResult();
        if (!working.isEmpty()) {
            g.renderItem(working, px + 8, py + 17);
            g.renderItem(result, px + 8, py + 57);
        }
        ItemStack hovered = ItemStack.EMPTY;
        if (over(mouseX, mouseY, 7, 16, 18, 18)) {
            hovered = working;
        } else if (over(mouseX, mouseY, 7, 56, 18, 18)) {
            hovered = result;
        }
        for (Cell cell : cells) {
            if (!cell.stack().isEmpty()) {
                g.renderItem(cell.stack(), px + cell.x(), py + cell.y());
                g.renderItemDecorations(font, cell.stack(), px + cell.x(), py + cell.y());
            }
            if (over(mouseX, mouseY, cell.x() - 1, cell.y() - 1, 18, 18)) {
                fill(g, cell.x(), cell.y(), 16, 16, 0x80FFFFFF);
                if (!cell.stack().isEmpty()) {
                    hovered = cell.stack();
                }
            }
        }
        if (over(mouseX, mouseY, 7, 16, 18, 18) || over(mouseX, mouseY, 7, 56, 18, 18)) {
            int y = over(mouseX, mouseY, 7, 16, 18, 18) ? 17 : 57;
            fill(g, 8, y, 16, 16, 0x80FFFFFF);
        }
        if (!hovered.isEmpty()) {
            g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
