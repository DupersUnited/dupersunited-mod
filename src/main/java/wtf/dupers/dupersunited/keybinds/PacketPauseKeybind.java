package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.glitchutils.PacketPauseManager;
import wtf.dupers.dupersunited.modules.glitcha.PacketDelayModule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class PacketPauseKeybind extends Keybind {
    public static long blinkStartTime = 0;

    public PacketPauseKeybind() {
        super("Delay Packets", GLFW.GLFW_KEY_F7);
    }

    @Override
    public int getKeyCode() {
        return PacketDelayModule.blinkBind.getValue();
    }

    @Override
    public void setKeyCode(int keyCode) {
        PacketDelayModule.blinkBind.setValue(keyCode);
    }

    public static long getBlinkStartTime() {
        return blinkStartTime;
    }

    @Override
    public void onPress() {
        handleToggle();
    }

    public static void handleToggle() {
        if (mc.getConnection() == null) return;

        if (isShiftDown()) {
            handleCancel();
        } else {
            boolean wasPaused = PacketPauseManager.isPaused();
            int packetCount = PacketPauseManager.getPacketQueue().size();

            PacketPauseManager.toggle();

            if (mc.player != null) {
                mc.gui.setScreen(mc.gui.screen()); //prolly better way to do this but wtv bro
                if (wasPaused) {
                    blinkStartTime = 0;
                    MainCommand.sendMessage(Component.literal("Sent ")
                            .append(Component.literal(Integer.toString(packetCount)).withStyle(ChatFormatting.AQUA))
                            .append(" packets."), true);
                    MainCommand.sendMessage(Component.literal("Packets are now ")
                            .append(Component.literal("resumed").withStyle(ChatFormatting.GREEN))
                            .append("."), true);
                } else {
                    MainCommand.sendMessage(Component.literal("Packets are now ")
                            .append(Component.literal("paused").withStyle(ChatFormatting.RED))
                            .append("."), true);
                    blinkStartTime = System.currentTimeMillis();
                }
            }
        }
    }

    private static boolean isShiftDown() {
        long window = mc.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public static void handleCancel() {
        if (!PacketPauseManager.isPaused()) {
            if (mc.player != null) {
                MainCommand.sendMessage("Cannot cancel packets because blink is not active.", true);
            }
            return;
        }
        int packetCount = PacketPauseManager.getPacketQueue().size();

        PacketPauseManager.clear();
        PacketPauseManager.toggle();

        blinkStartTime = 0;

        if (mc.player != null) {
            MainCommand.sendMessage(Component.literal("Blink cancelled, cleared ")
                    .append(Component.literal(Integer.toString(packetCount)).withStyle(ChatFormatting.AQUA))
                    .append(" packets."), true);
        }
    }
}