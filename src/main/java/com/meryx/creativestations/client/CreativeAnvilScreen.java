package com.meryx.creativestations.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
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
import java.util.function.Supplier;

/**
 * The Creative Anvil. Built from vanilla art: the anvil's frame, inventory and rename field, the creative
 * scroller and vanilla buttons. Pick any item (from your inventory or by searching every item) and edit its
 * enchantments (any level), name and lore; click the output slot to apply.
 * Uses the creative "set slot" packet, so it works on any server where you're in creative.
 */
public class CreativeAnvilScreen extends Screen {
    private static final Identifier ANVIL_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/anvil.png");
    private static final Identifier TEXT_FIELD = Identifier.withDefaultNamespace("container/anvil/text_field");
    private static final Identifier TEXT_FIELD_DISABLED = Identifier.withDefaultNamespace("container/anvil/text_field_disabled");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");

    // Window: wider than vanilla so enchantment names fit, inventory centred underneath
    private static final int PW = 230;
    private static final int TOP_H = 116;
    private static final int PH = TOP_H + 90;
    private static final int INV_X = (PW - 162) / 2;

    // The list on the right, and its scrollbar
    private static final int LX = 51;
    private static final int LY = 16;
    private static final int LW = PW - LX - 23;
    private static final int ROW_H = 18;
    private static final int ROWS = 5;
    private static final int LH = ROWS * ROW_H;
    private static final int TRACK_X = LX + LW + 3;
    private static final int SCROLLER_H = 15;

    // Enchantment row columns: x | name | - level +
    private static final int PLUS_X = LX + LW - 13;
    private static final int LEVEL_X = PLUS_X - 21;
    private static final int MINUS_X = LEVEL_X - 13;
    private static final int NAME_X = LX + 16;
    private static final int NAME_W = MINUS_X - NAME_X - 3;

    // Item search grid
    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 4;
    private static final int GRID_X = LX + (LW - GRID_COLS * 18) / 2;

    private static final int LORE_LINES = 4;
    private static final int MAX_LEVEL = 255;

    private static final int LABEL = 0xFF404040;
    private static final int ROW_A = 0xFF8B8B8B;
    private static final int ROW_B = 0xFF808080;
    private static final int ROW_ON = 0xFF9E9E9E;

    private enum Tab {
        ENCHANTS(Items.ENCHANTED_BOOK, "creativestations.editor.tab.enchants"),
        NAME(Items.NAME_TAG, "creativestations.editor.tab.name"),
        ITEMS(Items.COMPASS, "creativestations.editor.tab.items");

        final Item icon;
        final String key;

        Tab(Item icon, String key) {
            this.icon = icon;
            this.key = key;
        }
    }

    /** A clickable item slot. {@code x, y} is where the item is drawn; its 18x18 slot sits one pixel up and left. */
    private record Cell(int x, int y, Supplier<ItemStack> stack, boolean drawSlot) {
    }

    /** The widgets of one enchantment row; which enchantment they show depends on the scroll position. */
    private record Row(Button remove, Button name, Button minus, Button plus) {
    }

    private final Screen parent;
    private final List<Holder.Reference<Enchantment>> allEnchantments = new ArrayList<>();
    private final Map<Holder<Enchantment>, Integer> levels = new LinkedHashMap<>();
    private final String[] lore = new String[LORE_LINES];
    private final List<Cell> cells = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private List<ItemStack> searchResults = new ArrayList<>();

    private ItemStack working = ItemStack.EMPTY;
    /** Inventory-menu slot the item came from, or -1 if it was picked from search (goes to a free slot). */
    private int sourceSlot = -1;
    private String name = "";
    private String query = "";
    private Tab tab = Tab.ENCHANTS;
    private int enchantScroll;
    private int itemScroll;
    private boolean draggingScroller;
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
        if (stack.isEmpty()) {
            return;
        }
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
        refreshRows();
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

    // ---- Scrolling ----

    private int scrollTotal() {
        return switch (tab) {
            case ENCHANTS -> working.isEmpty() ? 0 : allEnchantments.size();
            case ITEMS -> (searchResults.size() + GRID_COLS - 1) / GRID_COLS;
            case NAME -> 0;
        };
    }

    private int scrollVisible() {
        return tab == Tab.ITEMS ? GRID_ROWS : ROWS;
    }

    private int scrollPos() {
        return tab == Tab.ITEMS ? itemScroll : enchantScroll;
    }

    private void setScroll(int value) {
        int max = Math.max(0, scrollTotal() - scrollVisible());
        value = Math.max(0, Math.min(max, value));
        if (tab == Tab.ITEMS) {
            itemScroll = value;
        } else {
            enchantScroll = value;
        }
        refreshRows();
    }

    private boolean canScroll() {
        return scrollTotal() > scrollVisible();
    }

    /** Moves the scroller so its middle follows the mouse. */
    private void scrollToMouse(double mouseY) {
        double fraction = (mouseY - (py + LY) - SCROLLER_H / 2.0) / (LH - SCROLLER_H);
        fraction = Math.max(0, Math.min(1, fraction));
        setScroll((int) Math.round(fraction * (scrollTotal() - scrollVisible())));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (canScroll()) {
            setScroll(scrollPos() - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (canScroll() && over(click.x(), click.y(), TRACK_X, LY, 12, LH)) {
            draggingScroller = true;
            scrollToMouse(click.y());
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (draggingScroller) {
            scrollToMouse(click.y());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingScroller = false;
        return super.mouseReleased(click);
    }

    // ---- Widgets ----

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = Math.max(2, (height - PH) / 2);
        cells.clear();
        rows.clear();
        boolean has = !working.isEmpty();

        // Input slot (click to clear) and output slot (click to apply)
        hit(7, 16, 18, 18, () -> {
            if (!working.isEmpty()) {
                clearSelection();
            }
        }, Component.translatable("creativestations.editor.input"));
        hit(7, 56, 18, 18, this::apply, null);

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

        // Inventory, in the vanilla slot positions
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < 36; i++) {
            final int index = i;
            int menuSlot = i < 9 ? 36 + i : i;
            int x = INV_X + 1 + (i % 9) * 18;
            int y = i < 9 ? TOP_H + 66 : TOP_H + 8 + ((i - 9) / 9) * 18;
            addCell(x, y, () -> inv.getItem(index), stack -> select(stack, menuSlot), false);
        }
        refreshRows();
    }

    private void initEnchants(boolean has) {
        if (!has) {
            return;
        }
        for (int r = 0; r < ROWS; r++) {
            final int row = r;
            int y = py + LY + r * ROW_H + 1;
            Button remove = Button.builder(Component.literal("x").withStyle(ChatFormatting.RED),
                    b -> holderAt(row, h -> setLevel(h, 0))).bounds(px + LX + 2, y, 12, 16).build();
            remove.setTooltip(Tooltip.create(Component.translatable("creativestations.editor.remove")));
            Button nameHit = Button.builder(Component.empty(),
                    b -> holderAt(row, h -> setLevel(h, levels.containsKey(h) ? 0 : 1))).bounds(px + NAME_X - 1, y, NAME_W + 2, 16).build();
            nameHit.setAlpha(0.0F);
            Button minus = Button.builder(Component.literal("-"),
                    b -> holderAt(row, h -> setLevel(h, levels.getOrDefault(h, 0) - 1))).bounds(px + MINUS_X, y, 12, 16).build();
            Button plus = Button.builder(Component.literal("+"),
                    b -> holderAt(row, h -> setLevel(h, levels.getOrDefault(h, 0) + 1))).bounds(px + PLUS_X, y, 12, 16).build();
            addRenderableWidget(remove);
            addRenderableWidget(nameHit);
            addRenderableWidget(minus);
            addRenderableWidget(plus);
            rows.add(new Row(remove, nameHit, minus, plus));
        }
    }

    private Holder.Reference<Enchantment> holderAt(int row) {
        int index = enchantScroll + row;
        return index < allEnchantments.size() ? allEnchantments.get(index) : null;
    }

    private void holderAt(int row, java.util.function.Consumer<Holder<Enchantment>> action) {
        Holder.Reference<Enchantment> holder = holderAt(row);
        if (holder != null) {
            action.accept(holder);
        }
    }

    /** Updates the row widgets after scrolling or a level change, without recreating them. */
    private void refreshRows() {
        for (int r = 0; r < rows.size(); r++) {
            Row row = rows.get(r);
            Holder.Reference<Enchantment> holder = holderAt(r);
            boolean shown = holder != null;
            int level = shown ? levels.getOrDefault(holder, 0) : 0;
            row.remove().visible = shown && level > 0;
            row.name().visible = shown;
            row.minus().visible = shown;
            row.plus().visible = shown;
            row.minus().active = level > 0;
            row.plus().active = level < MAX_LEVEL;
            if (shown) {
                row.name().setTooltip(Tooltip.create(Component.empty().append(holder.value().description())
                        .append(Component.translatable("creativestations.editor.max", holder.value().getMaxLevel()))));
            }
        }
    }

    /** A borderless text box sitting on the vanilla anvil's rename field, like the real anvil. */
    private EditBox textField(int row, String value, Component hint, int maxLength, boolean editable) {
        EditBox box = new EditBox(font, px + LX + 5, py + LY + row * ROW_H + 5, LW - 10, 12, hint);
        box.setBordered(false);
        box.setTextColor(0xFFFFFFFF);
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setHint(hint);
        box.setEditable(editable);
        box.active = editable;
        addRenderableWidget(box);
        return box;
    }

    private void initName(boolean has) {
        textField(0, name, Component.translatable("creativestations.editor.name"), 100, has)
                .setResponder(value -> name = value);
        for (int i = 0; i < LORE_LINES; i++) {
            final int line = i;
            textField(i + 1, lore[i], Component.translatable("creativestations.editor.lore"), 200, has)
                    .setResponder(value -> lore[line] = value);
        }
    }

    private void initItems() {
        EditBox search = textField(0, query, Component.translatable("creativestations.editor.search"), 50, true);
        search.setResponder(value -> {
            query = value;
            updateSearch();
        });
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                final int offset = r * GRID_COLS + c;
                Supplier<ItemStack> stack = () -> {
                    int index = itemScroll * GRID_COLS + offset;
                    return index < searchResults.size() ? searchResults.get(index) : ItemStack.EMPTY;
                };
                addCell(GRID_X + 1 + c * 18, LY + ROW_H + 1 + r * 18, stack, s -> select(s, -1), true);
            }
        }
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

    private void addCell(int x, int y, Supplier<ItemStack> stack, java.util.function.Consumer<ItemStack> onClick, boolean drawSlot) {
        hit(x - 1, y - 1, 18, 18, () -> onClick.accept(stack.get()), null);
        cells.add(new Cell(x, y, stack, drawSlot));
    }

    // ---- Drawing ----

    private boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= px + x && mouseX < px + x + w && mouseY >= py + y && mouseY < py + y + h;
    }

    private void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(px + x, py + y, px + x + w, py + y + h, color);
    }

    /** A vanilla-style inset: dark top-left edge, white bottom-right edge. */
    private void inset(GuiGraphics g, int x, int y, int w, int h, int color) {
        fill(g, x, y, w, h, 0xFF373737);
        fill(g, x + 1, y + 1, w - 1, h - 1, 0xFFFFFFFF);
        fill(g, x + 1, y + 1, w - 2, h - 2, color);
    }

    /** Draws the anvil texture's 4px border around a window of any size. */
    private void frame(GuiGraphics g) {
        int c = 4;
        int tw = 176;
        int th = 166;
        int x2 = px + PW - c;
        int y2 = py + PH - c;
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px, py, 0, 0, c, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, x2, py, tw - c, 0, c, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px, y2, 0, th - c, c, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, x2, y2, tw - c, th - c, c, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px + c, py, c, 0, PW - 2 * c, c, 1, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px + c, y2, c, th - c, PW - 2 * c, c, 1, c, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px, py + c, 0, c, c, PH - 2 * c, c, 1, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, x2, py + c, tw - c, c, c, PH - 2 * c, c, 1, 256, 256);
        g.fill(px + c, py + c, x2, y2, 0xFFC6C6C6);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);
        frame(g);
        g.blit(RenderPipelines.GUI_TEXTURED, ANVIL_TEXTURE, px + INV_X, py + TOP_H + 7, 7, 83, 162, 76, 256, 256);
        g.drawString(font, title, px + 8, py + 6, LABEL, false);
        g.drawString(font, Component.translatable("container.inventory"), px + INV_X + 1, py + TOP_H - 4, LABEL, false);

        // Input -> arrow -> output
        inset(g, 7, 16, 18, 18, 0xFF8B8B8B);
        int arrow = 0xFF8B8B8B;
        fill(g, 13, 37, 6, 9, arrow);
        for (int i = 0; i < 6; i++) {
            fill(g, 10 + i, 46 + i, 12 - 2 * i, 1, arrow);
        }
        inset(g, 7, 56, 18, 18, 0xFF8B8B8B);

        // Tabs on vanilla button sprites
        for (Tab t : Tab.values()) {
            int y = 16 + t.ordinal() * 24;
            Identifier sprite = t == tab ? BUTTON_DISABLED : over(mouseX, mouseY, 28, y, 22, 22) ? BUTTON_HIGHLIGHTED : BUTTON;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, px + 28, py + y, 22, 22);
        }

        // The list: an inset panel with grey rows
        inset(g, LX - 1, LY - 1, LW + 2, LH + 2, ROW_A);
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            for (int r = 0; r < ROWS; r++) {
                Holder.Reference<Enchantment> holder = holderAt(r);
                if (holder == null) {
                    break;
                }
                boolean on = levels.containsKey(holder);
                fill(g, LX, LY + r * ROW_H, LW, ROW_H, on ? ROW_ON : r % 2 == 0 ? ROW_A : ROW_B);
                inset(g, LEVEL_X, LY + r * ROW_H + 1, 20, 16, 0xFF000000);
            }
        } else if (tab == Tab.NAME || tab == Tab.ITEMS) {
            int fields = tab == Tab.NAME ? LORE_LINES + 1 : 1;
            boolean editable = tab == Tab.ITEMS || !working.isEmpty();
            for (int i = 0; i < fields; i++) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, editable ? TEXT_FIELD : TEXT_FIELD_DISABLED,
                        px + LX + 1, py + LY + i * ROW_H + 1, LW - 2, 16);
            }
        }
        for (Cell cell : cells) {
            if (cell.drawSlot()) {
                inset(g, cell.x() - 1, cell.y() - 1, 18, 18, 0xFF8B8B8B);
            }
        }

        // Scrollbar track and the creative inventory's scroller
        inset(g, TRACK_X - 1, LY - 1, 14, LH + 2, 0xFF8B8B8B);
        int total = scrollTotal();
        int visible = scrollVisible();
        boolean scrollable = total > visible;
        int thumbY = scrollable ? LY + (LH - SCROLLER_H) * scrollPos() / (total - visible) : LY;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, scrollable ? SCROLLER : SCROLLER_DISABLED, px + TRACK_X, py + thumbY, 12, SCROLLER_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        // Enchantment names and levels
        if (tab == Tab.ENCHANTS && !working.isEmpty()) {
            for (int r = 0; r < ROWS; r++) {
                Holder.Reference<Enchantment> holder = holderAt(r);
                if (holder == null) {
                    break;
                }
                int level = levels.getOrDefault(holder, 0);
                int y = py + LY + r * ROW_H;
                String full = holder.value().description().getString();
                String label = font.width(full) <= NAME_W ? full : font.plainSubstrByWidth(full, NAME_W - font.width("..")) + "..";
                g.drawString(font, label, px + NAME_X, y + 5, level > 0 ? 0xFFFFFF55 : 0xFFFFFFFF, true);
                String lvl = level > 0 ? String.valueOf(level) : "-";
                g.drawString(font, lvl, px + LEVEL_X + (20 - font.width(lvl)) / 2, y + 5, level > 0 ? 0xFFFFFFFF : 0xFF808080, false);
            }
        } else if (working.isEmpty() && tab != Tab.ITEMS) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("creativestations.editor.pick"), LW - 10);
            int y = py + LY + (tab == Tab.NAME ? ROW_H * 5 - 10 * lines.size() - 2 : 30);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), px + LX + 5, y + i * 10, 0xFFFFFFFF, true);
            }
        }

        for (Tab t : Tab.values()) {
            g.renderItem(new ItemStack(t.icon), px + 31, py + 19 + t.ordinal() * 24);
        }

        // Input, output and every item slot
        ItemStack result = buildResult();
        if (!working.isEmpty()) {
            g.renderItem(working, px + 8, py + 17);
            g.renderItem(result, px + 8, py + 57);
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int y : new int[]{16, 56}) {
            if (over(mouseX, mouseY, 7, y, 18, 18)) {
                fill(g, 8, y + 1, 16, 16, 0x80FFFFFF);
                hovered = y == 16 ? working : result;
            }
        }
        for (Cell cell : cells) {
            ItemStack stack = cell.stack().get();
            if (!stack.isEmpty()) {
                g.renderItem(stack, px + cell.x(), py + cell.y());
                g.renderItemDecorations(font, stack, px + cell.x(), py + cell.y());
            }
            if (over(mouseX, mouseY, cell.x() - 1, cell.y() - 1, 18, 18)) {
                fill(g, cell.x(), cell.y(), 16, 16, 0x80FFFFFF);
                if (!stack.isEmpty()) {
                    hovered = stack;
                }
            }
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
