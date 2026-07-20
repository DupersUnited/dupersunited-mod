package wtf.dupers.dupersunited.events;

import wtf.dupers.dupersunited.commands.subcommands.WaitCommand;
import wtf.dupers.dupersunited.features.ClickSlotManager;
import wtf.dupers.dupersunited.features.PayAllManager;
import wtf.dupers.dupersunited.features.PluginScanner;
import wtf.dupers.dupersunited.features.auth.AuthManager;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacroManager;
import wtf.dupers.dupersunited.keybinds.KeybindManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

import static wtf.dupers.dupersunited.MainClient.MODULE_MANAGER;
import static wtf.dupers.dupersunited.SharedVariables.screenToOpen;

public class TickEvent {
    public static int pendingDisconnectTicks = -1;
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (pendingDisconnectTicks > 0) {
                pendingDisconnectTicks--;
            } else if (pendingDisconnectTicks == 0) {
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().getConnection()
                            .disconnect(Text.literal("Disconnected & sent packets"));
                }
                pendingDisconnectTicks = -1;
            }

            if (screenToOpen != null) {
                client.setScreen(screenToOpen);
                screenToOpen = null;
            }
            if (MODULE_MANAGER != null) {
                MODULE_MANAGER.onTick();
            }

            AuthManager.onTick();
            ChatMacroManager.onTick();
            ClickSlotManager.onTick();
            KeybindManager.onTick();
            PayAllManager.onTick();
            PluginScanner.onTick();
            WaitCommand.onTick();
        });
    }
}
