package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import wtf.dupers.dupersunited.api.module.settings.StringSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static wtf.dupers.dupersunited.MainClient.mc;

public class SpamModule extends Module {

    private static final int MAX_MESSAGES = 3;

    private final StringSetting[] messages = new StringSetting[MAX_MESSAGES];
    private final IntSetting delay = register(new IntSetting("Delay (ms)", 1000, 50, 10000));
    private final BooleanSetting limited = register(new BooleanSetting("Limit", false));
    private final StringSetting count = register(new StringSetting("Count", "10"));
    private final BooleanSetting bypass = register(new BooleanSetting("Bypass AntiSpam", false));
    private final IntSetting suffixLen = register(new IntSetting("Suffix Length", 3, 1, 8));

    private final Random random = new Random();

    private long lastSendTime = 0;
    private int sentCount = 0;
    private int messageIndex = 0;

    public SpamModule() {
        super("Spam", "Repeatedly sends chat messages with a configurable delay.", Category.misc);

        // defualt messages
        for (int i = 0; i < MAX_MESSAGES; i++) {
            String defaultVal = i == 0 ? "DU ON TOP" : "JOIN GG/DUPES";
            messages[i] = register(new StringSetting("MSG " + (i + 1), defaultVal, 256));
        }

        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));

        count.visible = () -> limited.getValue();
        suffixLen.visible = () -> bypass.getValue();
    }

    @Override
    protected void onEnable() {
        lastSendTime = 0;
        sentCount = 0;
        messageIndex = 0;
    }

    @Override
    protected void onDisable() {
        sentCount = 0;
        messageIndex = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (limited.getValue() && sentCount >= getCountValue()) {
            setEnabled(false);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSendTime < delay.getValue()) return;

        List<String> active = getActiveMessages();
        if (active.isEmpty()) return;

        if (messageIndex >= active.size()) messageIndex = 0;

        String msg = active.get(messageIndex);
        if (bypass.getValue()) msg = msg + " " + randomSuffix();

        mc.player.connection.sendChat(msg);

        messageIndex = (messageIndex + 1) % active.size();
        lastSendTime = now;
        sentCount++;
    }

    //anti-spam bypassert
    private String randomSuffix() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < suffixLen.getValue(); i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // returning only non empty message slots
    private List<String> getActiveMessages() {
        List<String> result = new ArrayList<>();
        for (StringSetting m : messages) {
            String val = m.getValue();
            if (val != null && !val.isEmpty()) result.add(val);
        }
        return result;
    }

    private int getCountValue() {
        try {
            int val = Integer.parseInt(count.getValue().trim());
            return Math.max(1, val);
        } catch (NumberFormatException e) {
            return 10;
        }
    }
}