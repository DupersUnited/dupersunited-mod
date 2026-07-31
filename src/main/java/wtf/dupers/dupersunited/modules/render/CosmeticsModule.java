package wtf.dupers.dupersunited.modules.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.ButtonSetting;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import wtf.dupers.dupersunited.api.module.settings.ModeSetting;
import wtf.dupers.dupersunited.features.cosmetics.CosmeticCatalog;
import wtf.dupers.dupersunited.features.screens.CosmeticsPickerScreen;

public final class CosmeticsModule extends Module {
    private static final String[] HEADS = CosmeticCatalog.forSlot(CosmeticCatalog.Slot.HEAD).stream().map(CosmeticCatalog.Item::name).toArray(String[]::new);
    private static final String[] TAILS = CosmeticCatalog.forSlot(CosmeticCatalog.Slot.TAIL).stream().map(CosmeticCatalog.Item::name).toArray(String[]::new);
    public final ButtonSetting browse = register(new ButtonSetting("Browse Cosmetics", () -> MinecraftClient.getInstance().setScreen(new CosmeticsPickerScreen(this))));
    public final ModeSetting head = register(new ModeSetting("Head", "None", HEADS));
    public final ModeSetting tail = register(new ModeSetting("Tail", "None", TAILS));
    public final BooleanSetting animate = register(new BooleanSetting("Animate", true));
    public final IntSetting headX = register(new IntSetting("Head X", 0, -8, 8));
    public final IntSetting headY = register(new IntSetting("Head Height", 0, -8, 8));
    public final IntSetting headZ = register(new IntSetting("Head Z", 0, -8, 8));
    public final IntSetting tailX = register(new IntSetting("Tail X", 0, -8, 8));
    public final IntSetting tailY = register(new IntSetting("Tail Height", 0, -8, 8));
    public final IntSetting tailZ = register(new IntSetting("Tail Z", 0, -8, 8));
    public final IntSetting headSize = register(new IntSetting("Head Size", 100, 25, 200));
    public final IntSetting headWidth = register(new IntSetting("Head Width", 100, 25, 200));
    public final IntSetting headStretch = register(new IntSetting("Head Stretch", 100, 25, 200));
    public final IntSetting tailSize = register(new IntSetting("Tail Size", 100, 25, 200));
    public final IntSetting tailWidth = register(new IntSetting("Tail Width", 100, 25, 200));
    public final IntSetting tailStretch = register(new IntSetting("Tail Stretch", 100, 25, 200));
    public final IntSetting headPitch = register(new IntSetting("Head Pitch", 0, -180, 180));
    public final IntSetting headYaw = register(new IntSetting("Head Yaw", 0, -180, 180));
    public final IntSetting headRoll = register(new IntSetting("Head Roll", 0, -180, 180));
    public final IntSetting tailPitch = register(new IntSetting("Tail Pitch", 0, -180, 180));
    public final IntSetting tailYaw = register(new IntSetting("Tail Yaw", 0, -180, 180));
    public final IntSetting tailRoll = register(new IntSetting("Tail Roll", 0, -180, 180));

    public CosmeticsModule() {
        super("Cosmetics", "Blocky head and tail cosmetics with searchable selection and credits.", Category.render);
        head.visible = () -> false;
        tail.visible = () -> false;
        animate.visible = () -> false;
        headX.visible = () -> false;
        headY.visible = () -> false;
        headZ.visible = () -> false;
        tailX.visible = () -> false;
        tailY.visible = () -> false;
        tailZ.visible = () -> false;
        headSize.visible = () -> false;
        headWidth.visible = () -> false;
        headStretch.visible = () -> false;
        tailSize.visible = () -> false;
        tailWidth.visible = () -> false;
        tailStretch.visible = () -> false;
        headPitch.visible = () -> false;
        headYaw.visible = () -> false;
        headRoll.visible = () -> false;
        tailPitch.visible = () -> false;
        tailYaw.visible = () -> false;
        tailRoll.visible = () -> false;
        register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    public CosmeticCatalog.Item selectedHead() { return CosmeticCatalog.named(CosmeticCatalog.Slot.HEAD, head.getValue()); }
    public CosmeticCatalog.Item selectedTail() { return CosmeticCatalog.named(CosmeticCatalog.Slot.TAIL, tail.getValue()); }
}
