package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import wtf.dupers.dupersunited.utils.ColorUtil;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class BetterTabModule extends Module {
    public final IntSetting tabSize = register(new IntSetting("TablistSize", 500, 1, 1000));
    public final IntSetting columnHeight = register(new IntSetting("ColumnHeight", 50, 1, 100));
    public final BooleanSetting scrollable = register(new BooleanSetting("Scrollable", false));
    public final BooleanSetting highlightSelf = register(new BooleanSetting("HighlightSelf", true));
    public final BooleanSetting showGamemode = register(new BooleanSetting("Gamemode", false));
    public final BooleanSetting showPing = register(new BooleanSetting("Ping", false));
    public final BooleanSetting showPingIcon = register(new BooleanSetting("PingIcon", true));

    public int scrollOffset = 0;

    public BetterTabModule() {
        super("BetterTab", "Allows you to configure your tab list.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    public void onMouseScroll(double amount) {
        if (!isEnabled() || !scrollable.getValue()) return;

        if (amount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else {
            scrollOffset++;
        }
    }

    @Override
    public void onDisable() {
        scrollOffset = 0;
        super.onDisable();
    }

    public Component getPlayerName(PlayerInfo entry) {
        Component name = entry.getTabListDisplayName();
        if (name == null) name = Component.literal(entry.getProfile().name());

        if (highlightSelf.getValue() && entry.getProfile().id().toString()
                .equals(mc.player.getGameProfile().id().toString())) {
            name = Component.literal(name.getString()).setStyle(name.getStyle().withColor(TextColor.fromRgb(ColorUtil.GREEN)));
        }

        if (showGamemode.getValue()) {
            GameType gm = entry.getGameMode();
            String gmText = gm == null ? "?" : switch (gm) {
                case SPECTATOR -> "Sp";
                case SURVIVAL -> "S";
                case CREATIVE -> "C";
                case ADVENTURE -> "A";
            };
            MutableComponent text = Component.empty();
            text.append(name);
            text.append(Component.literal(" [" + gmText + "]").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
            name = text;
        }

        return name;
    }
}