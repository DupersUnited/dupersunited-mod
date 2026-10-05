package wtf.dupers.dupersunited.events;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.glitchutils.GhostBlock;
import wtf.dupers.dupersunited.features.glitchutils.GuiPacketDelayManager;
import wtf.dupers.dupersunited.features.glitchutils.PacketPauseManager;
import wtf.dupers.dupersunited.features.glitchutils.PayAllManager;
import wtf.dupers.dupersunited.modules.render.FreecamModule;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class WorldEvent {
    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (PacketPauseManager.isPaused()) PacketPauseManager.toggle();
            if (GuiPacketDelayManager.isPaused()) GuiPacketDelayManager.resume();
            GhostBlock.clearGhosts();
            FreecamModule freecam = MainClient.MODULE_MANAGER.getModule(FreecamModule.class);
            if (freecam != null && freecam.isEnabled()) freecam.setEnabled(false);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (PacketPauseManager.isPaused()) PacketPauseManager.toggle();
            if (GuiPacketDelayManager.isPaused()) GuiPacketDelayManager.resume();
            GhostBlock.clearGhosts();
            FreecamModule freecam = MainClient.MODULE_MANAGER.getModule(FreecamModule.class);
            if (freecam != null && freecam.isEnabled()) freecam.setEnabled(false);
            if (PayAllManager.isRunning()) PayAllManager.stopPayAll();
        });
    }
}