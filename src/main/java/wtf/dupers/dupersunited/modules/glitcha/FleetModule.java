package wtf.dupers.dupersunited.modules.glitcha;

import com.google.gson.JsonElement;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import wtf.dupers.dupersunited.api.module.settings.ModeSetting;
import wtf.dupers.dupersunited.features.fleet.FleetNet;

public class FleetModule extends Module {
    private final ModeSetting mode = register(new ModeSetting("Mode", "Host", "Host", "Worker"));
    private final IntSetting port = register(new IntSetting("Port", 8778, 1024, 65535));
    private final IntSetting delay = register(new IntSetting("Delay (ms)", 0, 0, 10000));

    public FleetModule() {
        super("Fleet", "Control multiple Minecraft clients with one client.", Category.glitcha);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
        delay.visible = () -> mode.getValue().equals("Host");
    }

    public int getDelay() {
        return delay.getValue();
    }

    @Override
    protected void onEnable() {
        if (mode.getValue().equals("Worker") ? !FleetNet.connect(port.getValue()) : !FleetNet.host(port.getValue())) setEnabled(false);
    }

    @Override
    protected void onDisable() {
        if (FleetNet.state() != FleetNet.State.IDLE) FleetNet.stop();
    }

    @Override
    public void readJson(JsonElement element) {
        super.readJson(element);
        // never wake up to fleet hosting or connected, should re-enable by hand each launch
        // unless someone thinks differently, then you can void this whole override
        if (isEnabled()) setEnabled(false);
    }
}