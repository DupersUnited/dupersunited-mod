package wtf.dupers.dupersunited.modules.render;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.StringSetting;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class NickModule extends Module {
    private final StringSetting nickname = register(new StringSetting("Name", "Duper"));
    public final BooleanSetting censor = register(new BooleanSetting("Censor", false));
    public final BooleanSetting onlyIngame = register(new BooleanSetting("OnlyIngame", false));

    public String username;

    public NickModule() {
        super("NameChanger","Changes your displayed name", Category.render);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onEnable() {
        username = mc.getUser().getName();
    }

    public String replaceName(String string) {
        if (string != null && isEnabled()) {
            if (censor.getValue()) {
                if (username.length() <= 2) return string;
                String censored = username.substring(0, 2) + "*".repeat(username.length() - 2);
                return string.replace(username, censored);
            }
            String nick = nickname.getValue().replace("&", "§");
            return string.replace(username, nick);
        }

        return string;
    }

}
