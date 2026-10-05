package wtf.dupers.dupersunited.modules.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import wtf.dupers.dupersunited.features.screens.NoRenderScreen;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.ButtonSetting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

import static wtf.dupers.dupersunited.MainClient.mc;

public class NoRenderModule extends Module {

    public static final Set<EntityType<?>> selectedEntityIds = new ReferenceOpenHashSet<>();

    public final BooleanSetting particles = register(new BooleanSetting("Particles", false));

    public final BooleanSetting plainObfuscatedText = register(new BooleanSetting("Glyphs", false));

    private final ButtonSetting selectEntities = register(
            new ButtonSetting("SelectEntities", this::openScreen)
    );

    public NoRenderModule() {
        super("NoRender", "Hides entity, particles and glyphs.", Category.render);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    public void openScreen() {
        mc.gui.setScreen(new NoRenderScreen());
    }

    @Override
    public JsonElement writeJson() {
        JsonObject object = (JsonObject) super.writeJson();
        JsonArray noRenderEntities = new JsonArray();
        for (EntityType<?> type : selectedEntityIds) noRenderEntities.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        object.add("selected-entity-ids", noRenderEntities);
        return object;
    }

    @Override
    public void readJson(JsonElement element) {
        super.readJson(element);
        if (element instanceof JsonObject object && object.has("selected-entity-ids")) {
            selectedEntityIds.clear();
            for (JsonElement el : object.getAsJsonArray("selected-entity-ids")) {
                if (el.isJsonNull()) continue;
                Identifier id = Identifier.tryParse(el.getAsString());
                if (id == null) continue;
                BuiltInRegistries.ENTITY_TYPE.get(id).ifPresent(entry -> selectedEntityIds.add(entry.value()));
            }
        }
    }
}
