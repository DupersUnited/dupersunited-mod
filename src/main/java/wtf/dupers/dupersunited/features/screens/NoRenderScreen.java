package wtf.dupers.dupersunited.features.screens;

import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.modules.render.NoRenderModule;
import net.minecraft.network.chat.Component;

public class NoRenderScreen extends EntitySelectionScreen {
    public NoRenderScreen() {
        super(Component.literal("No Render"), "No Render - Select Entities",
                NoRenderModule.selectedEntityIds, ConfigManager::save);
    }
}
