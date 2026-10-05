package wtf.dupers.dupersunited.features.glitchutils;

import net.minecraft.world.item.ItemStack;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.mixin.accessor.EditBoxWidgetAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens .Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;

public class NBTEditor extends Screen {
    private static final int TOPTEXT = 0xFF82D1E3; // light aqua
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BOX_WIDTH = 360;
    private static final int BOX_HEIGHT = 210;

    private final ItemStack stack;
    private MultiLineEditBox nbtInput;

    public NBTEditor(ItemStack stack) {
        super(Component.literal("NBT Editor"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        super.init();

        ClientPacketListener networkHandler = getNetwork();
        if (networkHandler == null) return;

        try {
            var lookup = networkHandler.registryAccess();
            var nbt = (CompoundTag) DataComponentMap.CODEC.encodeStart(
                    lookup.createSerializationContext(NbtOps.INSTANCE),
                    stack.getComponents()
            ).getOrThrow();

            initializeWidgets(nbt);
        } catch (Exception e) {
            MainCommand.sendMessage("Failed to load NBT: " + e.getMessage(), true);
            MainClient.LOGGER.error("Failed to load NBT", e);
            this.onClose();
        }
    }

    private void initializeWidgets(CompoundTag nbt) {
        int x = this.width / 2 - (BOX_WIDTH / 2);
        int y = this.height / 2 - (BOX_HEIGHT / 2);

        this.nbtInput = EditBoxWidgetAccessor.create(
                this.font,
                x,
                y,
                BOX_WIDTH,
                BOX_HEIGHT,
                Component.literal("NBT"),
                Component.empty(),
                WHITE,
                true,
                WHITE,
                true,
                false
        );
        this.nbtInput.setCharacterLimit(32767);

        this.nbtInput.setValue(prettyPrintNBT(nbt.toString().replace("§", "&")));
        this.addRenderableWidget(this.nbtInput);

        this.addRenderableWidget(Button.builder(Component.literal("Save Changes").withStyle(ChatFormatting.GREEN), b -> saveNBT())
            .bounds(this.width / 2 - 100, y + BOX_HEIGHT + 10, 200, 20)
            .build());

        this.setInitialFocus(this.nbtInput);
    }

    private void saveNBT() {
        ClientPacketListener networkHandler = getNetwork();
        if (networkHandler == null) return;

        try {
            String rawText = this.nbtInput.getValue().replace("&", "§");
            CompoundTag newNbt = TagParser.parseCompoundFully(rawText.isBlank() ? "{}" : rawText);

            var ops = networkHandler.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            DataComponentMap map = DataComponentMap.CODEC.parse(ops, newNbt)
                    .getOrThrow(msg -> new RuntimeException("Invalid NBT: " + msg));

            stack.applyComponents(map);
            MainCommand.sendMessage("NBT Updated.", true);
            this.onClose();
        } catch (Exception e) {
            MainCommand.sendMessage(Component.empty()
                .append(Component.literal("Failed to save! ").withStyle(ChatFormatting.RED))
                .append(e.getMessage()), true);

            MainClient.LOGGER.error("Failed to save NBT", e);
        }
    }

    private ClientPacketListener getNetwork() {
        if (this.minecraft == null) return null;
        return this.minecraft.getConnection();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, "NBT EDITOR", this.width / 2, 20, TOPTEXT);
    }

    private String prettyPrintNBT(String snbt) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inQuotes = false;
        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);
            if (c == '"' && (i == 0 || snbt.charAt(i - 1) != '\\')) inQuotes = !inQuotes;
            if (!inQuotes) {
                if (c == '{' || c == '[') {
                    sb.append(c).append("\n").append("  ".repeat(++indent));
                    continue;
                }
                if (c == '}' || c == ']') {
                    indent = Math.max(0, indent - 1);
                    sb.append("\n").append("  ".repeat(indent)).append(c);
                    continue;
                }
                if (c == ',') {
                    sb.append(c).append("\n").append("  ".repeat(indent));
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return (this.nbtInput != null && this.nbtInput.keyPressed(input)) || super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        return (this.nbtInput != null && this.nbtInput.charTyped(input)) || super.charTyped(input);
    }
}