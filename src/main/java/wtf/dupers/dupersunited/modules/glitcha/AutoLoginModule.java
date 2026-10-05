package wtf.dupers.dupersunited.modules.glitcha;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.autologin.AutoLoginManager;
import org.lwjgl.glfw.GLFW;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class AutoLoginModule extends Module {

    private static final Pattern REGISTER = Pattern.compile("(?i)/reg(?:ister)?\\b");
    private static final Pattern LOGIN = Pattern.compile("(?i)/log(?:in)?\\b");
    private static final Pattern PLACEHOLDER = Pattern.compile("<[^>]{1,32}>");

    private final BooleanSetting autoRegister = new BooleanSetting("Auto Register", true);
    private final BooleanSetting autoLogin = new BooleanSetting("Auto Login", true);

    private final Minecraft mc = Minecraft.getInstance();
    private static final String RANDOMCHARACTERSTHINGS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public AutoLoginModule() {
        super("AutoLogin", "Automatically registers and logs into cracked servers.", Category.glitcha);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
        this.register(autoRegister);
        this.register(autoLogin);

        AutoLoginManager.load();
    }

    @Override
    public void onPacketReceive(Packet<?> packet) {
        if (!isEnabled() || mc.player == null) return;

        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            String message = chatPacket.content().getString();
            String username = mc.player.getGameProfile().name();

            //handle registering
            if (autoRegister.getValue() && REGISTER.matcher(message).find()) {
                String existingPassword = AutoLoginManager.getPassword(username);
                String password = (existingPassword != null) ? existingPassword : generateRandomPassword();

                AutoLoginManager.saveAccount(username, password);

                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.connection.sendChat("/register " + password + " " + password);
                        MainCommand.sendMessage(Component.literal("Registered account ")
                            .append(Component.literal(username).withStyle(ChatFormatting.GREEN))
                            .append(" with saved password."), true);
                    }
                });
                return;
            }

            //handling auto login
            if (autoLogin.getValue() && LOGIN.matcher(message).find()) {
                String password = AutoLoginManager.getPassword(username);

                mc.execute(() -> {
                    if (mc.player != null) {
                        if (password != null) {
                            mc.player.connection.sendChat("/login " + password);
                            MainCommand.sendMessage(Component.literal("Logging in as ")
                                .append(Component.literal(username).withStyle(ChatFormatting.GREEN)), true);
                        } else {
                            MainCommand.sendMessage(Component.literal("Weird? No saved password found for ")
                                .append(Component.literal(username).withStyle(ChatFormatting.RED)), true);
                        }
                    }
                });
            }
        }
    }

    public static String cleanPlaceholders(String text) {
        return PLACEHOLDER.matcher(text).replaceAll("");
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(RANDOMCHARACTERSTHINGS.charAt(RANDOM.nextInt(RANDOMCHARACTERSTHINGS.length())));
        }
        return sb.toString();
    }
}