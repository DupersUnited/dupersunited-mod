package wtf.dupers.dupersunited.features.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.cosmetics.CapeManager;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;

import java.nio.file.Path;
import java.util.List;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.features.screens.ui.Theme.*;

public class ChangeSkinScreen extends DuScreen {
    private static final String[] MODES = {
        "Auto",
        "Classic",
        "Slim"
    };

    private final Screen returnTo;
    private final AccountsScreen.AccountEntry entry;

    private EditBox sourceField;
    private Component statusMessage = Component.empty();
    private int mode = 0;// 0 = auto, 1 = classic, 2 = slim
    private boolean busy = false;

    //current skin preview
    private volatile @Nullable String currentSkinUrl;
    private volatile boolean currentSlim;
    private volatile boolean currentLoading = false;
    private volatile boolean currentFailed = false;

    private interface Job {
        String run() throws Exception;
    }

    public ChangeSkinScreen(Screen parent, AccountsScreen.AccountEntry entry) {
        super(Component.literal("Change Skin"), parent, "Skin");
        this.returnTo = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(330, this.width - 20);
        int left = this.width / 2 - fieldWidth / 2;

        String previous = sourceField != null ? sourceField.getValue() : "";
        sourceField = new EditBox(this.font, left, 80, fieldWidth - 65, 18, Component.literal("Skin source"));
        sourceField.setMaxLength(512);
        sourceField.setHint(Component.literal("File path, NameMC link or IGN.").withStyle(ChatFormatting.DARK_GRAY));
        sourceField.setValue(previous);
        this.addRenderableWidget(sourceField);

        this.addRenderableWidget(Button.builder(Component.literal("Upload"), _ -> browse())
            .bounds(left + fieldWidth - 60, 79, 60, 20)
            .tooltip(Tooltip.create(Component.literal("Pick a .png skin from your computer")))
            .build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Model: " + MODES[mode]),
                btn -> {
                    mode = (mode + 1) % MODES.length;
                    btn.setMessage(Component.literal("Model: " + MODES[mode]));
                }
            ).bounds(left, 104, 100, 20)
            .tooltip(Tooltip.create(Component.literal(
                "Auto = detect from the skin\nClassic = 4px arms (Steve)\nSlim = 3px arms (Alex)")))
            .build());

        this.addRenderableWidget(Button.builder(Component.literal("Apply").withStyle(ChatFormatting.GREEN), _ -> apply())
            .bounds(left + 105, 104, 60, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset").withStyle(ChatFormatting.RED), _ -> reset())
            .bounds(left + 170, 104, 60, 20)
            .tooltip(Tooltip.create(Component.literal("Removes the custom skin (back to default)")))
            .build());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), _ -> mc.gui.setScreen(returnTo))
            .bounds(left + fieldWidth - 60, 104, 60, 20).build());

        if (currentSkinUrl == null && !currentLoading) refreshCurrent();
    }

    //input
    @Override
    public void onFilesDrop(@NonNull List<Path> paths) {
        if (paths.isEmpty()) return;
        sourceField.setValue(paths.getFirst().toString());
        statusMessage = Component.literal("File selected, press Apply").withStyle(ChatFormatting.GRAY);
    }

    private void browse() {
        if (busy) return;
        Thread.ofPlatform().daemon().start(() -> {
            String chosen = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png")).flip();
                chosen = TinyFileDialogs.tinyfd_openFileDialog("Select a skin", "", filters, "PNG images", false);
            } catch (Throwable ignored) {}
            String result = chosen;
            if (result != null && !result.isEmpty()) {
                mc.execute(() -> {
                    sourceField.setValue(result);
                    statusMessage = Component.literal("File selected, press Apply").withStyle(ChatFormatting.GRAY);
                });
            }
        });
    }

    //actions
    private void apply() {
        if (busy) return;
        String input = sourceField.getValue().trim();
        if (input.isEmpty()) {
            statusMessage = Component.literal("Enter a file path, NameMC link or username").withStyle(ChatFormatting.RED);
            return;
        }
        int chosenMode = mode;
        run("Fetching skin...", () -> {
            SkinImporter.ImportedSkin skin = SkinImporter.resolve(input);
            String variant = switch (chosenMode) {
                case 1 -> "classic";
                case 2 -> "slim";
                default -> skin.variant();
            };
            int code = SessionAPI.uploadSkin(skin.png(), variant, entry.token());
            if (code < 200 || code >= 300) throw new Exception(describe(code));
            return "Skin changed! (" + variant + ")";
        });
    }

    private void reset() {
        if (busy) return;
        run("Resetting skin...", () -> {
            int code = SessionAPI.resetSkin(entry.token());
            if (code < 200 || code >= 300) throw new Exception(describe(code));
            return "Skin reset!";
        });
    }

    private void run(String pending, Job job) {
        busy = true;
        statusMessage = Component.literal(pending).withStyle(ChatFormatting.YELLOW);

        Thread.ofVirtual().start(() -> {
            String error = null;
            String success = null;
            try {
                success = job.run();
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : "Something went wrong";
            }

            String err = error;
            String ok = success;
            mc.execute(() -> {
                busy = false;
                if (err != null) {
                    statusMessage = Component.literal(err).withStyle(ChatFormatting.RED);
                    return;
                }
                statusMessage = Component.literal(ok).withStyle(ChatFormatting.GREEN);

                refreshCurrent();
            });
        });
    }

    private void refreshCurrent() {
        currentLoading = true;
        Thread.ofVirtual().start(() -> {
            String[] info = null;

            try {
                if (entry.token() != null && !entry.token().isEmpty()) {
                    String[] profileInfo = SessionAPI.getProfileInfo(entry.token());
                    if (profileInfo != null && profileInfo.length > 1) {
                        info = SessionAPI.getCurrentSkin(entry.token());
                    }
                }
            } catch (Exception ignored) {}

            //fallback
            if (info == null && entry.name() != null) {
                try {
                    String uuidStr = null;
                    JsonObject lookup = JsonParser.parseString(
                        SessionAPI.getJson("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + entry.name())).getAsJsonObject();
                    if (lookup.has("id")) {
                        uuidStr = lookup.get("id").getAsString();
                    }

                    if (uuidStr != null) {
                        info = SessionAPI.getSkinByUuid(uuidStr);
                    }
                } catch (Exception ignored) {}
            }

            String[] finalInfo = info;
            mc.execute(() -> {
                currentLoading = false;
                if (finalInfo == null) {
                    currentFailed = true;
                } else {
                    currentSkinUrl = finalInfo[0];
                    currentSlim = "slim".equals(finalInfo[1]);
                    currentFailed = false;
                }
            });
        });
    }

    private static String describe(int code) {
        return switch (code) {
            case 400 -> "Rejected: not a valid skin image";
            case 401 -> "Invalid or expired token";
            case 403 -> "Forbidden (Account can't change its skin right now!)";
            case 429 -> "Rate limited, try again in a bit";
            case -1 -> "Request failed (Network Error)";
            default -> "Failed with HTTP " + code;
        };
    }

    //rendering
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);

        int cx = this.width / 2;
        graphics.centeredText(this.font, Component.literal("Change Skin").withStyle(ChatFormatting.BOLD), cx, 30, secondary);
        graphics.centeredText(this.font, Component.literal(entry.name()), cx, 45, text);
        graphics.centeredText(this.font, Component.literal("File, NameMC link or username. You can also upload a .png."), cx, 62, dim);

        if (!statusMessage.getString().isEmpty()) {
            graphics.centeredText(this.font, statusMessage, cx, 132, text);
        }

        graphics.centeredText(this.font, Component.literal("Current skin"), cx, 150, dim);

        String url = currentSkinUrl;
        Identifier texture = url != null ? CapeManager.getTextureOrLoad(url) : null;
        int scale = Math.clamp((this.height - 180) / 32, 1, 5);
        int px = cx - 8 * scale;
        int py = 164;

        if (url == null) {
            graphics.fill(px, py, px + 16 * scale, py + 32 * scale, header);
            graphics.centeredText(this.font, Component.literal("Failed to load? Odd."), cx, py + 16 * scale - 4, dim);
        } else if (texture != null) {
            drawModel(graphics, texture, px, py, scale, currentSlim);
        } else {
            //still downloading in the background dont throw error instantly
            graphics.fill(px, py, px + 16 * scale, py + 32 * scale, header);
            graphics.centeredText(this.font, Component.literal("Loading..."), cx, py + 16 * scale - 4, dim);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private static void drawModel(GuiGraphicsExtractor g, Identifier t, int x, int y, int s, boolean slim) {
        int aw = slim ? 3 : 4;
        int rightArmX = slim ? 1 : 0;
        int leftArmX = 12;

        // legs
        part(g, t, x, y, s, 4, 20, 4, 20, 4, 12);
        part(g, t, x, y, s, 8, 20, 20, 52, 4, 12);
        part(g, t, x, y, s, 4, 20, 4, 36, 4, 12);
        part(g, t, x, y, s, 8, 20, 4, 52, 4, 12);
        // body
        part(g, t, x, y, s, 4, 8, 20, 20, 8, 12);
        part(g, t, x, y, s, 4, 8, 20, 36, 8, 12);
        // arms
        part(g, t, x, y, s, rightArmX, 8, 44, 20, aw, 12);
        part(g, t, x, y, s, rightArmX, 8, 44, 36, aw, 12);
        part(g, t, x, y, s, leftArmX, 8, 36, 52, aw, 12);
        part(g, t, x, y, s, leftArmX, 8, 52, 52, aw, 12);
        // head
        part(g, t, x, y, s, 4, 0, 8, 8, 8, 8);
        part(g, t, x, y, s, 4, 0, 40, 8, 8, 8);
    }

    private static void part(GuiGraphicsExtractor g, Identifier t, int x, int y, int s, int dx, int dy, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, t, x + dx * s, y + dy * s, u, v, w * s, h * s, w, h, 64, 64);
    }
}