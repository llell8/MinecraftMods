package com.meryx.creativestations.client;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Creative Anvil. Click an item in your inventory to load it, edit its enchantments (any level) or its
 * name and lore, then click the output slot to apply. Uses the creative "set slot" packet, so it works on
 * any server where you're in creative.
 */
public class CreativeAnvilScreen extends Screen {
    private static final Identifier ANVIL_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/anvil.png");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    // Window
    private static final int PW = 196;
    private static final int TOP_H = 84;
    private static final int PH = TOP_H + 90;
    private static final int INV_X = (PW - 162) / 2;

    // Left column: input, arrow, output; then the tabs
    private static final int SLOT_X = 7;
    private static final int INPUT_Y = 16;
    private static final int OUTPUT_Y = 58;
    private static final int TAB_X = 30;
    private static final int TAB_SIZE = 20;

    // The panel and its scrollbar
    private static final int LX = 54;
    private static final int LY = 16;
    private static final int TRACK_X = PW - 7 - 12;
    private static final int LW = TRACK_X - 3 - LX;
    private static final int LH = 60;
    private static final int SCROLLER_H = 15;

    // Enchantment rows
    private static final int ROW_H = 20;
    private static final int ROWS = LH / ROW_H;
    private static final int LEVEL_W = 24;
    private static final int LEVEL_X = LX + LW - LEVEL_W - 2;
    private static final int NAME_X = LX + 20;
    private static final int NAME_W = LEVEL_X - NAME_X - 3;

    // Enchantment dropdown
    private static final int PICK_H = 12;
    private static final int PICK_ROWS = LH / PICK_H;

    // Name and lore text area
    private static final int LINE_H = 11;
    private static final int LORE_LINES = 4;

    private static final int MAX_LEVEL = 255;

    // Colours, matched to the look of the screenshots
    private static final int LABEL = 0xFF404040;
    private static final int PANEL = 0xFF57503F;
    private static final int PANEL_EDGE = 0xFF2B271F;
    private static final int DROPDOWN = 0xFF2E2A23;
    private static final int RED = 0xFFD8603E;
    private static final int RED_LIGHT = 0xFFF28B66;
    private static final int RED_DARK = 0xFF8E3A22;
    private static final int TAN = 0xFFC2A878;
    private static final int TAN_LIGHT = 0xFFE0CDA0;
    private static final int TAN_DARK = 0xFF8C744C;
    private static final int BLUE = 0xFF6E8BC0;
    private static final int BLUE_LIGHT = 0xFFA8C0E8;
    private static final int BLUE_DARK = 0xFF3B4E77;
    private static final int BLUE_ON = 0xFF9DB6E2;
    private static final int LORE_COLOR = 0xFFAA55FF;

    private enum Tab {
        NAME(Items.OAK_SIGN, "creativestations.editor.tab.name"),
        ENCHANTS(Items.ENCHANTED_BOOK, "creativestations.editor.tab.enchants");

        final Item icon;
        final String key;

        Tab(Item icon, String key) {
            this.icon = icon;
            this.key = key;
        }
    }

    /** A clickable inventory slot. {@code x, y} is where the item is drawn. */
    private record Cell(int x, int y, Supplier<ItemStack> stack) {
    }

    /** One enchantment row: remove button and level box, plus the add and choose buttons used on the last row. */
    private record Row(Button remove, EditBox level, Button add, Button choose) {
    }

    private final Screen parent;
    private final List<Holder.Reference<Enchantment>> allEnchantments = new ArrayList<>();
    private final Map<Holder<Enchantment>, Integer> levels = new LinkedHashMap<>();
    private final String[] lore = new String[LORE_LINES];
    private final List<Cell> cells = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> pickButtons = new ArrayList<>();
    private List<Holder.Reference<Enchantment>> candidates = new ArrayList<>();
    /** The enchantment chosen on the add row, and the level typed next to it. */
    private Holder.Reference<Enchantment> pending;
    private String pendingLevel = "1";

    private ItemStack working = ItemStack.EMPTY;
    /** Inventory-menu slot the item came from. */
    private int sourceSlot = -1;
    private String name = "";
    private Tab tab = Tab.ENCHANTS;
    private boolean pickerOpen;
    private int rowScroll;
    private int pickScroll;
    private boolean draggingScroller;
    private boolean updating;
    private int px;
    private int py;

    public CreativeAnvilScreen(Screen parent) {
        super(Component.translatable("creativestations.editor.title"));
        this.parent = parent;
        Arrays.fill(lore, "");
        Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .listElements().forEach(allEnchantments::add);
        allEnchantments.sort(Comparator.comparing(h -> h.value().description().getString()));
    }

    // ---- Editing ----

    private DataComponentType<ItemEnchantments> enchantComponent() {
        return working.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }

    /** The item's name without any custom name, e.g. "Netherite Sword". */
    private String defaultName() {
        ItemStack plain = working.copy();
        plain.remove(DataComponents.CUSTOM_NAME);
        return plain.getHoverName().getString();
    }

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
        name = customName == null ? defaultName() : customName.getString();
        ItemLore itemLore = working.get(DataComponents.LORE);
        for (int i = 0; i < LORE_LINES; i++) {
            lore[i] = itemLore != null && i < itemLore.lines().size() ? itemLore.lines().get(i).getString() : "";
        }
        pickerOpen = false;
        rowScroll = 0;
        pending = null;
        ensurePending();
        rebuildWidgets();
    }

    private void clearSelection() {
        working = ItemStack.EMPTY;
        sourceSlot = -1;
        levels.clear();
        name = "";
        Arrays.fill(lore, "");
        pickerOpen = false;
        rebuildWidgets();
    }

    private List<Holder<Enchantment>> applied() {
        return new ArrayList<>(levels.keySet());
    }

    private void updateCandidates() {
        candidates = new ArrayList<>();
        for (Holder.Reference<Enchantment> holder : allEnchantments) {
            if (!levels.containsKey(holder)) {
                candidates.add(holder);
            }
        }
    }

    /** Makes sure the add row has a sensible enchantment chosen. */
    private void ensurePending() {
        updateCandidates();
        if (pending == null || levels.containsKey(pending)) {
            pending = candidates.isEmpty() ? null : candidates.get(0);
            pendingLevel = pending == null ? "1" : String.valueOf(pending.value().getMaxLevel());
        }
    }

    private void openPicker() {
        updateCandidates();
        if (candidates.isEmpty()) {
            return;
        }
        int index = candidates.indexOf(pending);
        pickScroll = Math.max(0, Math.min(Math.max(0, candidates.size() - PICK_ROWS), index));
        pickerOpen = true;
        refresh();
    }

    private void choose(Holder.Reference<Enchantment> holder) {
        pending = holder;
        pendingLevel = String.valueOf(holder.value().getMaxLevel());
        pickerOpen = false;
        refresh();
    }

    private void addPending() {
        if (pending == null) {
            return;
        }
        int level = pendingLevel.isEmpty() ? 1 : Integer.parseInt(pendingLevel);
        levels.put(pending, Math.max(1, Math.min(MAX_LEVEL, level)));
        ensurePending();
        // Scroll so the add row stays visible
        rowScroll = Math.max(0, levels.size() + 1 - ROWS);
        refresh();
    }

    private ItemStack buildResult() {
        if (working.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = working.copy();
        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        levels.forEach(enchants::set);
        result.set(enchantComponent(), enchants.toImmutable());

        if (name.isBlank() || name.equals(defaultName())) {
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
        if (result.isEmpty() || sourceSlot < 0) {
            return;
        }
        // Tell the server, and update our own copy (the server doesn't echo creative slot changes back)
        minecraft.gameMode.handleCreativeModeItemAdd(result, sourceSlot);
        minecraft.player.inventoryMenu.getSlot(sourceSlot).set(result);
        // Like a real anvil: taking the output empties the input, and the menu stays open
        clearSelection();
    }

    /** '&' colour codes, and no forced italics. */
    private static Component styled(String text) {
        return Component.literal(text.replace('&', '§')).withStyle(style -> style.withItalic(false));
    }

    // ---- Scrolling ----

    private int scrollTotal() {
        if (tab != Tab.ENCHANTS || working.isEmpty()) {
            return 0;
        }
        return pickerOpen ? candidates.size() : levels.size() + 1;
    }

    private int scrollVisible() {
        return pickerOpen ? PICK_ROWS : ROWS;
    }

    private int scrollPos() {
        return pickerOpen ? pickScroll : rowScroll;
    }

    private void setScroll(int value) {
        value = Math.max(0, Math.min(Math.max(0, scrollTotal() - scrollVisible()), value));
        if (pickerOpen) {
            pickScroll = value;
        } else {
            rowScroll = value;
        }
        refresh();
    }

    private boolean canScroll() {
        return scrollTotal() > scrollVisible();
    }

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
        // Clicking outside the dropdown closes it
        if (pickerOpen && !over(click.x(), click.y(), LX, LY, LW, LH)) {
            pickerOpen = false;
            refresh();
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
        pickButtons.clear();
        boolean has = !working.isEmpty();

        hit(SLOT_X, INPUT_Y, 18, 18, () -> {
            if (!working.isEmpty()) {
                clearSelection();
            }
        }, Component.translatable("creativestations.editor.input"));
        hit(SLOT_X, OUTPUT_Y, 18, 18, this::apply, null);

        for (Tab t : Tab.values()) {
            hit(TAB_X, LY + t.ordinal() * (TAB_SIZE + 2), TAB_SIZE, TAB_SIZE, () -> {
                tab = t;
                pickerOpen = false;
                rebuildWidgets();
            }, Component.translatable(t.key));
        }

        if (has && tab == Tab.ENCHANTS) {
            initEnchants();
        } else if (has && tab == Tab.NAME) {
            initName();
        }

        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < 36; i++) {
            final int index = i;
            int menuSlot = i < 9 ? 36 + i : i;
            int x = INV_X + 1 + (i % 9) * 18;
            int y = i < 9 ? TOP_H + 66 : TOP_H + 8 + ((i - 9) / 9) * 18;
            hit(x - 1, y - 1, 18, 18, () -> select(inv.getItem(index), menuSlot), null);
            cells.add(new Cell(x, y, () -> inv.getItem(index)));
        }
        refresh();
    }

    private void initEnchants() {
        for (int r = 0; r < ROWS; r++) {
            final int row = r;
            int y = LY + r * ROW_H;
            Button remove = hit(LX + 3, y + 3, 14, 14, () -> {
                Holder<Enchantment> holder = appliedAt(row);
                if (holder != null) {
                    levels.remove(holder);
                    rowScroll = Math.max(0, Math.min(rowScroll, levels.size() + 1 - ROWS));
                    refresh();
                }
            }, Component.translatable("creativestations.editor.remove"));

            EditBox level = new EditBox(font, px + LEVEL_X + 3, py + y + 6, LEVEL_W - 4, 10, Component.empty());
            level.setBordered(false);
            level.setTextColor(0xFFFFFFFF);
            level.setMaxLength(3);
            level.setFilter(s -> s.matches("\\d*"));
            level.setResponder(value -> {
                if (updating) {
                    return;
                }
                Holder<Enchantment> holder = appliedAt(row);
                if (holder != null && !value.isEmpty()) {
                    levels.put(holder, Math.max(1, Math.min(MAX_LEVEL, Integer.parseInt(value))));
                } else if (holder == null && isAddRow(row)) {
                    pendingLevel = value;
                }
            });
            addRenderableWidget(level);

            Button add = hit(LX + 3, y + 3, 14, 14, this::addPending, Component.translatable("creativestations.editor.add"));
            Button choose = hit(NAME_X - 2, y + 3, LEVEL_X - NAME_X - 1, 14, this::openPicker,
                    Component.translatable("creativestations.editor.choose"));
            rows.add(new Row(remove, level, add, choose));
        }
        for (int i = 0; i < PICK_ROWS; i++) {
            final int row = i;
            pickButtons.add(hit(LX, LY + i * PICK_H, LW, PICK_H, () -> {
                int index = pickScroll + row;
                if (index < candidates.size()) {
                    choose(candidates.get(index));
                }
            }, null));
        }
    }

    private void initName() {
        for (int i = 0; i <= LORE_LINES; i++) {
            final int line = i - 1;
            boolean isName = i == 0;
            EditBox box = new EditBox(font, px + LX + 4, py + LY + 4 + i * LINE_H, LW - 8, 10, Component.empty());
            box.setBordered(false);
            box.setTextColor(isName ? 0xFFFFFFFF : LORE_COLOR);
            box.setMaxLength(isName ? 100 : 200);
            box.setValue(isName ? name : lore[line]);
            if (i == 1) {
                box.setHint(Component.translatable("creativestations.editor.lore"));
            }
            box.setResponder(value -> {
                if (isName) {
                    name = value;
                } else {
                    lore[line] = value;
                }
            });
            addRenderableWidget(box);
        }
    }

    private Holder<Enchantment> appliedAt(int row) {
        List<Holder<Enchantment>> applied = applied();
        int index = rowScroll + row;
        return index < applied.size() ? applied.get(index) : null;
    }

    private boolean isAddRow(int row) {
        return rowScroll + row == levels.size();
    }

    /** Shows and fills the right widgets for the current scroll position, without recreating them. */
    private void refresh() {
        updating = true;
        for (int r = 0; r < rows.size(); r++) {
            Row row = rows.get(r);
            Holder<Enchantment> holder = appliedAt(r);
            boolean shown = holder != null && !pickerOpen;
            boolean addRow = !pickerOpen && isAddRow(r);
            row.remove().visible = shown;
            row.level().visible = shown || (addRow && pending != null);
            String value = shown ? String.valueOf(levels.get(holder)) : addRow ? pendingLevel : "";
            if (!row.level().getValue().equals(value)) {
                row.level().setValue(value);
            }
            row.add().visible = addRow && pending != null;
            row.choose().visible = addRow && pending != null;
        }
        for (int i = 0; i < pickButtons.size(); i++) {
            pickButtons.get(i).visible = pickerOpen && pickScroll + i < candidates.size();
        }
        updating = false;
    }

    /** An invisible button over an area (relative to the window), so vanilla handles clicks and tooltips. */
    private Button hit(int x, int y, int w, int h, Runnable action, Component tooltip) {
        Button button = Button.builder(Component.empty(), b -> action.run()).bounds(px + x, py + y, w, h).build();
        button.setAlpha(0.0F);
        if (tooltip != null) {
            button.setTooltip(Tooltip.create(tooltip));
        }
        addRenderableWidget(button);
        return button;
    }

    @Override
    public void onClose() {
        if (pickerOpen) {
            pickerOpen = false;
            refresh();
            return;
        }
        minecraft.setScreen(parent);
    }

    // ---- Drawing ----

    private boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= px + x && mouseX < px + x + w && mouseY >= py + y && mouseY < py + y + h;
    }

    private void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(px + x, py + y, px + x + w, py + y + h, color);
    }

    /** A raised box: light top-left edge, dark bottom-right edge. */
    private void bevel(GuiGraphics g, int x, int y, int w, int h, int color, int light, int dark) {
        fill(g, x, y, w, h, dark);
        fill(g, x, y, w - 1, h - 1, light);
        fill(g, x + 1, y + 1, w - 2, h - 2, color);
    }

    /** A vanilla inventory slot: dark top-left edge, white bottom-right edge. */
    private void slot(GuiGraphics g, int x, int y) {
        fill(g, x, y, 18, 18, 0xFF373737);
        fill(g, x + 1, y + 1, 17, 17, 0xFFFFFFFF);
        fill(g, x + 1, y + 1, 16, 16, 0xFF8B8B8B);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, s, px + x, py + y, color, true);
    }

    private String fit(String s, int width) {
        return font.width(s) <= width ? s : font.plainSubstrByWidth(s, width - font.width("..")) + "..";
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
        g.drawString(font, title, px + 8, py + 5, LABEL, false);
        g.drawString(font, Component.translatable("container.inventory"), px + INV_X + 1, py + TOP_H - 4, LABEL, false);

        // Input -> arrow -> output
        slot(g, SLOT_X, INPUT_Y);
        int arrow = 0xFF8B8B8B;
        fill(g, SLOT_X + 6, INPUT_Y + 21, 6, 11, arrow);
        for (int i = 0; i < 6; i++) {
            fill(g, SLOT_X + 3 + i, INPUT_Y + 32 + i, 12 - 2 * i, 1, arrow);
        }
        slot(g, SLOT_X, OUTPUT_Y);

        // Tabs: blue squares, the selected one lighter
        for (Tab t : Tab.values()) {
            int y = LY + t.ordinal() * (TAB_SIZE + 2);
            boolean on = t == tab;
            boolean hovered = over(mouseX, mouseY, TAB_X, y, TAB_SIZE, TAB_SIZE);
            bevel(g, TAB_X, y, TAB_SIZE, TAB_SIZE, on || hovered ? BLUE_ON : BLUE, on ? 0xFFFFFFFF : BLUE_LIGHT, BLUE_DARK);
        }

        // Panel
        fill(g, LX - 1, LY - 1, LW + 2, LH + 2, PANEL_EDGE);
        boolean has = !working.isEmpty();
        if (has && tab == Tab.NAME) {
            fill(g, LX - 1, LY - 1, LW + 2, LH + 2, 0xFFA0A0A0);
            fill(g, LX, LY, LW, LH, 0xFF000000);
        } else if (has && pickerOpen) {
            fill(g, LX, LY, LW, LH, DROPDOWN);
        } else {
            fill(g, LX, LY, LW, LH, PANEL);
            if (has && tab == Tab.ENCHANTS) {
                for (int r = 0; r < ROWS; r++) {
                    int y = LY + r * ROW_H;
                    if (appliedAt(r) != null) {
                        bevel(g, LX + 3, y + 3, 14, 14, RED, RED_LIGHT, RED_DARK);
                        bevel(g, LEVEL_X, y + 3, LEVEL_W, 14, TAN, TAN_LIGHT, TAN_DARK);
                    } else if (isAddRow(r) && pending != null) {
                        bevel(g, LX + 3, y + 3, 14, 14, BLUE, BLUE_LIGHT, BLUE_DARK);
                        // The dropdown box showing which enchantment will be added
                        int bx = NAME_X - 2;
                        int bw = LEVEL_X - NAME_X - 1;
                        boolean hover = over(mouseX, mouseY, bx, y + 3, bw, 14);
                        fill(g, bx, y + 3, bw, 14, hover ? 0xFFD0D0D0 : PANEL_EDGE);
                        fill(g, bx + 1, y + 4, bw - 2, 12, 0xFF3A352B);
                        int ax = bx + bw - 9;
                        for (int i = 0; i < 4; i++) {
                            fill(g, ax + i, y + 8 + i, 7 - 2 * i, 1, 0xFFC0B8A4);
                        }
                        bevel(g, LEVEL_X, y + 3, LEVEL_W, 14, TAN, TAN_LIGHT, TAN_DARK);
                    }
                }
            }
        }

        // Scrollbar
        fill(g, TRACK_X - 1, LY - 1, 14, LH + 2, 0xFF373737);
        fill(g, TRACK_X, LY, 13, LH + 1, 0xFFFFFFFF);
        fill(g, TRACK_X, LY, 12, LH, 0xFF8B8B8B);
        int total = scrollTotal();
        int visible = scrollVisible();
        boolean scrollable = total > visible;
        int thumbY = scrollable ? LY + (LH - SCROLLER_H) * scrollPos() / (total - visible) : LY;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, scrollable ? SCROLLER : SCROLLER_DISABLED, px + TRACK_X, py + thumbY, 12, SCROLLER_H);

        // "?" help, next to the name editor
        if (tab == Tab.NAME) {
            bevel(g, TRACK_X - 13, 3, 11, 11, TAN, TAN_LIGHT, TAN_DARK);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        boolean has = !working.isEmpty();

        if (has && tab == Tab.ENCHANTS && !pickerOpen) {
            for (int r = 0; r < ROWS; r++) {
                int y = LY + r * ROW_H;
                Holder<Enchantment> holder = appliedAt(r);
                if (holder != null) {
                    text(g, "x", LX + 8, y + 6, 0xFFFFFFFF);
                    text(g, fit(holder.value().description().getString(), NAME_W), NAME_X, y + 6, 0xFFFFFFFF);
                } else if (isAddRow(r)) {
                    if (pending != null) {
                        text(g, "+", LX + 8, y + 6, 0xFFFFFFFF);
                        text(g, fit(pending.value().description().getString(), LEVEL_X - NAME_X - 14), NAME_X + 1, y + 6, 0xFF9C9686);
                    } else {
                        text(g, Component.translatable("creativestations.editor.all").getString(), LX + 4, y + 6, 0xFF9C9686);
                    }
                }
            }
        } else if (has && pickerOpen) {
            for (int i = 0; i < PICK_ROWS && pickScroll + i < candidates.size(); i++) {
                int y = LY + i * PICK_H;
                if (over(mouseX, mouseY, LX, y, LW, PICK_H)) {
                    fill(g, LX + 1, y, LW - 2, PICK_H, 0xFFD0D0D0);
                    fill(g, LX + 2, y + 1, LW - 4, PICK_H - 2, 0xFF000000);
                }
                Holder.Reference<Enchantment> holder = candidates.get(pickScroll + i);
                text(g, fit(holder.value().description().getString(), LW - 8), LX + 4, y + 2, holder == pending ? 0xFFFFFF55 : 0xFFFFFFFF);
            }
        } else if (!has) {
            int y = LY + 6;
            for (var line : font.split(Component.translatable("creativestations.editor.pick"), LW - 8)) {
                g.drawString(font, line, px + LX + 4, py + y, 0xFFE0DAC8, true);
                y += 10;
            }
        }

        for (Tab t : Tab.values()) {
            g.renderItem(new ItemStack(t.icon), px + TAB_X + 2, py + LY + 2 + t.ordinal() * (TAB_SIZE + 2));
        }

        if (tab == Tab.NAME) {
            text(g, "?", TRACK_X - 10, 5, 0xFFFFFFFF);
            if (over(mouseX, mouseY, TRACK_X - 13, 3, 11, 11)) {
                g.setTooltipForNextFrame(font, font.split(Component.translatable("creativestations.editor.help"), 200), mouseX, mouseY);
            }
        }

        // Items: input, output, inventory
        ItemStack result = buildResult();
        if (has) {
            g.renderItem(working, px + SLOT_X + 1, py + INPUT_Y + 1);
            g.renderItem(result, px + SLOT_X + 1, py + OUTPUT_Y + 1);
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int y : new int[]{INPUT_Y, OUTPUT_Y}) {
            if (over(mouseX, mouseY, SLOT_X, y, 18, 18)) {
                fill(g, SLOT_X + 1, y + 1, 16, 16, 0x80FFFFFF);
                hovered = y == INPUT_Y ? working : result;
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
}
