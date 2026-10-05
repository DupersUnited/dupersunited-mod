package wtf.dupers.dupersunited.modules.glitcha;

import wtf.dupers.dupersunited.features.glitchutils.PacketPauseManager;
import wtf.dupers.dupersunited.features.screens.PacketDelayScreen;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.*;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

import static wtf.dupers.dupersunited.MainClient.mc;

public class PacketDelayModule extends Module {
    private final Map<BooleanSetting, Class<? extends Packet<?>>> packetSettings = new LinkedHashMap<>();
    private boolean lastMasterState = false;
    private boolean selectiveMode = false;

    public static final BindSetting blinkBind = new BindSetting("DelayPackets", GLFW.GLFW_KEY_F7);

    public PacketDelayModule() {
        super("DelayPackets", "Let's you customize which packets get disabled.", Category.glitcha);
        this.register(new BindSetting("GUI Key", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
        this.register(blinkBind);

        // movement packets
        addPacket("Position", ServerboundMovePlayerPacket.Pos.class);
        addPacket("Rotation", ServerboundMovePlayerPacket.Rot.class);
        addPacket("PositionLook", ServerboundMovePlayerPacket.PosRot.class);
        addPacket("GroundOnly", ServerboundMovePlayerPacket.StatusOnly.class);
        addPacket("VehicleMove", ServerboundMoveVehiclePacket.class);
        addPacket("PlayerInput", ServerboundPlayerInputPacket.class);
        addPacket("BoatPaddle", ServerboundPaddleBoatPacket.class);
        addPacket("TeleportConfirm", ServerboundAcceptTeleportationPacket.class);
        addPacket("TickEnd", ServerboundClientTickEndPacket.class);

        // interaction
        addPacket("PlayerAction", ServerboundPlayerActionPacket.class);
        addPacket("InteractBlock", ServerboundUseItemOnPacket.class);
        addPacket("InteractEntity", ServerboundInteractPacket.class);
        addPacket("InteractItem", ServerboundUseItemPacket.class);
        addPacket("HandSwing", ServerboundSwingPacket.class);
        addPacket("ClientCommand", ServerboundClientCommandPacket.class);
        addPacket("UpdateSlot", ServerboundSetCarriedItemPacket.class);

        // inventory
        addPacket("ClickSlot", ServerboundContainerClickPacket.class);
        addPacket("CloseScreen", ServerboundContainerClosePacket.class);
        addPacket("ButtonClick", ServerboundContainerButtonClickPacket.class);
        addPacket("CreativeAction", ServerboundSetCreativeModeSlotPacket.class);
        addPacket("CraftRequest", ServerboundPlaceRecipePacket.class);
        addPacket("RecipeBookData", ServerboundRecipeBookChangeSettingsPacket.class);
        addPacket("RecipeCategory", ServerboundRecipeBookSeenRecipePacket.class);

        // chat, command & data stuff
        addPacket("ChatMessage", ServerboundChatPacket.class);
        addPacket("CommandExec", ServerboundChatCommandPacket.class);
        addPacket("UpdateSign", ServerboundSignUpdatePacket.class);
        addPacket("UpdateCommandBlock", ServerboundSetCommandBlockPacket.class);
        addPacket("UpdateBeacon", ServerboundSetBeaconPacket.class);
        addPacket("BookUpdate", ServerboundEditBookPacket.class);
        addPacket("RenameItem", ServerboundRenameItemPacket.class);

        // technical packets
        addPacket("KeepAlive", ServerboundKeepAlivePacket.class);
        addPacket("ClientStatus", ServerboundClientInformationPacket.class);
        addPacket("CustomPayload", ServerboundCustomPayloadPacket.class);
        addPacket("AdvancementTab", ServerboundSeenAdvancementsPacket.class);
        addPacket("ResourcePackStatus", ServerboundResourcePackPacket.class);
        addPacket("QueryBlockNbt", ServerboundBlockEntityTagQueryPacket.class);
        addPacket("ChunkAck", ServerboundChunkBatchReceivedPacket.class);
        addPacket("ReconfigAck", ServerboundConfigurationAcknowledgedPacket.class);

        fillAll();
    }

    private void addPacket(String name, Class<? extends Packet<?>> clazz) {
        // None of these get registered to clickgui as that's a fucking cancer show it's too many things :(
        packetSettings.put(new BooleanSetting(name, true), clazz);
    }

    @Override
    public void toggle() {
        // Doing this so I can override the base toggle to avoid annoying fucking toggle message
        setEnabled(!isEnabled());
    }

    @Override
    protected void onEnable() {
        // open the screen immediately when the module is toggled
        Screen previous = mc.gui.screen();
        mc.execute(() -> mc.gui.setScreen(new PacketDelayScreen(previous, this)));
        // now we turn the module back off so it acts as a button & not spam shit with the module enable/disabled
        this.setEnabled(false);
    }

    @Override protected void onDisable() { fillAll(); }

    public void syncTargets() {
        PacketPauseManager.clearTargets();
        if (!selectiveMode) {
            packetSettings.values().forEach(PacketPauseManager::addTarget);
        } else {
            packetSettings.forEach((setting, clazz) -> {
                if (!setting.getValue()) PacketPauseManager.addTarget(clazz);
            });
        }
    }

    public void resetSettings() {
        packetSettings.keySet().forEach(s -> s.setValue(true));
    }

    private void fillAll() {
        PacketPauseManager.clearTargets();
        packetSettings.values().forEach(PacketPauseManager::addTarget);
    }

    public boolean isSelectiveMode() { return selectiveMode; }
    public void toggleSelectiveMode() { selectiveMode = !selectiveMode; }
    public Map<BooleanSetting, Class<? extends Packet<?>>> getPacketSettings() { return packetSettings; }
}