package wtf.dupers.dupersunited.features;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import wtf.dupers.dupersunited.features.screens.ClickGui;

public class ModMenuIntegration implements ModMenuApi{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClickGui::new;
    }
}
