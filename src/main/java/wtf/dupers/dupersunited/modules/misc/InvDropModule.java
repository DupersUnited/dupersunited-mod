package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class InvDropModule extends Module {

    private final IntSetting delay = register(new IntSetting("Delay (ms)", 500, 0, 1000));
    private final BooleanSetting dropArmour = register(new BooleanSetting("Drop Armour", false));
    private final BooleanSetting dropOffhand = register(new BooleanSetting("Drop Offhand", false));
    private final BooleanSetting hotbarOnly = register(new BooleanSetting("Hotbar Only", false));

    private boolean dropping = false;
    private int currentSlot = 0;
    private long lastDropTime = 0;

    public InvDropModule() {
        super("DropAll", "Drops every item in your inventory with a configurable delay.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onEnable() {
        dropping = true;
        currentSlot = 0;
        lastDropTime = 0;
    }

    @Override
    protected void onDisable() {
        dropping = false;
        currentSlot = 0;
    }

    @Override
    public void onTick() {
        if (!dropping) return;

        if (mc.player == null || mc.gameMode == null) return;

        long now = System.currentTimeMillis();
        if (now - lastDropTime < delay.getValue()) return;

        int maxSlot = hotbarOnly.getValue() ? 9 : 36;

        while (currentSlot < maxSlot && mc.player.getInventory().getItem(currentSlot).isEmpty()) {
            currentSlot++;
        }

        if (currentSlot >= maxSlot) {
            if (!hotbarOnly.getValue() && dropArmour.getValue()) {
                dropArmorSlots();
            }
            if (dropOffhand.getValue()) {
                dropOffhandSlot();
            }
            dropping = false;
            setEnabled(false);
            return;
        }

        int screenSlot = currentSlot < 9 ? 36 + currentSlot : currentSlot;

        mc.gameMode.handleContainerInput(
                mc.player.inventoryMenu.containerId,
                screenSlot,
                1,
                ContainerInput.THROW,
                mc.player
        );

        lastDropTime = now;
        currentSlot++;
    }

    private void dropArmorSlots() {
        for (int armorScreen = 5; armorScreen <= 8; armorScreen++) {
            if (!mc.player.inventoryMenu.getSlot(armorScreen).getItem().isEmpty()) {
                mc.gameMode.handleContainerInput(
                        mc.player.inventoryMenu.containerId,
                        armorScreen,
                        1,
                        ContainerInput.THROW,
                        mc.player
                );
            }
        }
    }

    private void dropOffhandSlot() {
        if (!mc.player.inventoryMenu.getSlot(45).getItem().isEmpty()) {
            mc.gameMode.handleContainerInput(
                    mc.player.inventoryMenu.containerId,
                    45,
                    1,
                    ContainerInput.THROW,
                    mc.player
            );
        }
    }
}