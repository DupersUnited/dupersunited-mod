package wtf.dupers.dupersunited.events;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.glitchutils.FabricatePackets;
import wtf.dupers.dupersunited.features.glitchutils.GuiPacketDelayManager;
import wtf.dupers.dupersunited.features.glitchutils.PacketPauseManager;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.keybinds.PacketPauseKeybind;
import wtf.dupers.dupersunited.mixin.accessor.HandledScreenAccessor;
import wtf.dupers.dupersunited.mixin.accessor.ScreenAccessor;
import wtf.dupers.dupersunited.modules.exploit.BookBotModule;
import wtf.dupers.dupersunited.modules.glitcha.GuiUtilsModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.nbt.NbtOps;
import wtf.dupers.dupersunited.utils.ColorUtil;


import java.util.List;

import static wtf.dupers.dupersunited.MainClient.mc;

public class GuiEvent {
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (MainClient.MODULE_MANAGER == null) return;
            GuiUtilsModule mod = MainClient.MODULE_MANAGER.getModule(GuiUtilsModule.class);
            if (mod != null && mod.isEnabled()) {
                if (!shouldAttachToScreen(screen))
                    return;

                int x = 10;
                int y = 10;

                if (mod.saveGuiSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            Button.builder(
                                            Component.literal("Close Without Packet").withStyle(s -> s.withColor(ColorUtil.MAUVE2)),
                                            button -> SaveGuiManager.saveAndCloseGui()
                                    )
                                    .bounds(x, y, 110, 20)
                                    .tooltip(Tooltip.create(Component.literal("Closes your GUI clientside and saves it to reopen later.")))
                                    .build()
                    );
                }

                if (mod.desyncSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            Button.builder(
                                            Component.literal("Desync").withStyle(s -> s.withColor(ColorUtil.MAUVE)),
                                            btn -> {
                                                if (client.player == null || client.getConnection() == null) return;
                                                if (client.player.containerMenu == client.player.inventoryMenu) {
                                                    MainCommand.sendMessage("No screen open to desync.", true);
                                                    return;
                                                }
                                                client.getConnection().send(
                                                        new ServerboundContainerClosePacket(client.player.containerMenu.containerId)
                                                );
                                                MainCommand.sendMessage("Desynced screen.", true);
                                            }
                                    )
                                    .bounds(x, y + 225, 80, 20)
                                    .tooltip(Tooltip.create(Component.literal("Tells server GUI closed but keeps it open client-side.")))
                                    .build()
                    );
                }

                if (mod.clearGuiSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            Button.builder(
                                            Component.literal("Clear GUI Cache").withStyle(s -> s.withColor(ColorUtil.MAUVE)),
                                            button -> {
                                                Screen previousScreen = SaveGuiManager.savedScreen;
                                                if (previousScreen != null) {
                                                    SaveGuiManager.savedScreen = null;
                                                    SaveGuiManager.deadGui = false;
                                                    MainCommand.sendMessage(Component.literal("Removed ")
                                                        .append(Component.literal(previousScreen.getTitle().getString()).withStyle(ChatFormatting.AQUA))
                                                        .append(" from saved screens."), true);
                                                } else {
                                                    MainCommand.sendMessage("You do not have a currently saved GUI!", true);
                                                }
                                            }
                                    )
                                    .bounds(x, y + 25, 110, 20)
                                    .tooltip(Tooltip.create(Component.literal("Clears your saved GUI.")))
                                    .build()
                    );
                }

                if (mod.disconnectAndSendSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            Button.builder(
                                            Component.literal("DC & Send Packets").withStyle(s -> s.withColor(ColorUtil.MAUVE)),
                                            btn -> {
                                                if (client.getConnection() == null) return;
                                                if (PacketPauseManager.isPaused()) PacketPauseKeybind.handleToggle();
                                                if (GuiPacketDelayManager.isPaused()) GuiPacketDelayManager.resume();
                                                TickEvent.pendingDisconnectTicks = 1;
                                            }
                                    )
                                    .bounds(x, y + 50, 110, 20)
                                    .tooltip(Tooltip.create(Component.literal("Sends all currently queued packets (if there's any) and disconnects you from the server.")))
                                    .build()
                    );
                }

                if (mod.delayPackets.getValue()) {
                    boolean paused = GuiPacketDelayManager.isPaused();
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            Button.builder(
                                            Component.literal("Delay Packets: ").withStyle(s -> s.withColor(ColorUtil.MAUVE))
                                                    .append(Component.literal(paused ? "ON" : "OFF")
                                                            .withStyle(s -> s.withColor(paused ? 0xa6e3a1 : 0xf38ba8))),
                                            btn -> {
                                                GuiPacketDelayManager.toggle();
                                                Minecraft.getInstance().gui.setScreen(Minecraft.getInstance().gui.screen());
                                            }
                                    )
                                    .bounds(x, y + 75, 110, 20)
                                    .tooltip(Tooltip.create(Component.literal("ONLY pauses GUI related packets.")))
                                    .build()
                    );
                }

                if (mod.saveGuiButtonSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            Button.builder(
                                            Component.literal("Save Gui").withStyle(s -> s.withColor(ColorUtil.MAUVE)),
                                            btn -> SaveGuiManager.saveGui()
                                    )
                                    .bounds(x, y + 100, 110, 20)
                                    .tooltip(Tooltip.create(Component.literal("Saves the current GUI.")))
                                    .build()
                    );
                }

                if (mod.commandBoxSetting.getValue()) {
                    int commandY = y + 125;
                    EditBox chatBox = new EditBox(
                            client.font,
                            x, commandY,
                            110, 18,
                            Component.literal("Chat")
                    ) {
                        @Override
                        public boolean keyPressed(KeyEvent input) {
                            if (input.key() == 257 || input.key() == 335) {
                                String text = this.getValue().trim();
                                if (text.isEmpty() || client.player == null) return false;
                                client.execute(() -> {
                                    if (text.startsWith("/"))
                                        client.player.connection.sendCommand(text.substring(1));
                                    else
                                        client.player.connection.sendChat(text);
                                });
                                this.setValue("");
                                return true;
                            }
                            return super.keyPressed(input);
                        }
                    };

                    chatBox.setMaxLength(256);
                    chatBox.setHint(Component.literal("Chat or Command").withStyle(s -> s.withColor(0x888888)));
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(chatBox);

                 /*   ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            ButtonWidget.builder(
                                            Text.literal("▶").styled(s -> s.withColor(0xa6e3a1)),
                                            btn -> {
                                                String input = chatBox.getText().trim();
                                                if (input.isEmpty() || client.player == null) return;
                                                String cmd = input.startsWith("/") ? input.substring(1) : input;
                                                client.execute(() -> client.player.networkHandler.sendChatCommand(cmd));
                                                chatBox.setText("");
                                            }
                                    )
                                    .dimensions(x + 112, commandY, 20, 18)
                                    .tooltip(Tooltip.of(Text.literal("Sends command while you are in a GUI")))
                                    .build()
                    );*/
                }

                if (mod.ShowFabricatePackets.getValue()) {
                    int fabY = y + 150;
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                            Button.builder(
                                            Component.literal("Fabricate Packet").withStyle(s -> s.withColor(ColorUtil.MAUVE)),
                                            btn -> FabricatePackets.open()
                                    )
                                    .bounds(x, fabY, 110, 20)
                                    .tooltip(Tooltip.create(Component.literal("Fabricate and send a custom ClickSlot or ButtonClick packet")))
                                    .build()
                    );

                    if (client.player != null) {
                        Button syncIdBtn = Button.builder(
                                Component.literal("Sync Id: " + client.player.containerMenu.containerId),
                                btn -> {})
                            .bounds(x, y + 172, 110, 12)
                            .build();
                        syncIdBtn.active = false;
                        ((ScreenAccessor) screen).dupersunited$addRenderableWidget(syncIdBtn);

                        Button revisionBtn = Button.builder(
                            Component.literal("Revision: " + client.player.containerMenu.getStateId()),
                            btn -> {})
                            .bounds(x, y + 184, 110, 12)
                            .build();
                        revisionBtn.active = false;
                        ((ScreenAccessor) screen).dupersunited$addRenderableWidget(revisionBtn);
                    }
                }

                if (mod.copyGuiInfo.getValue() && screen instanceof AbstractContainerScreen<?> handledScreen) {
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(Button.builder(
                                            Component.literal("Copy GUI as JSON").withStyle(s -> s.withColor(ColorUtil.MAUVE2)),
                                            btn -> {
                                                JsonObject root = new JsonObject();
                                                root.addProperty("title", handledScreen.getTitle().getString());

                                                JsonArray slots = new JsonArray();
                                                for (Slot slot : handledScreen.getMenu().slots) {
                                                    if (!slot.hasItem()) continue;
                                                    ItemStack stack = slot.getItem();

                                                    JsonObject slotObj = new JsonObject();
                                                    slotObj.addProperty("index", slot.getContainerSlot());
                                                    slotObj.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());

                                                    ItemStack.CODEC.encodeStart(
                                                            Minecraft.getInstance().level.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack
                                                    ).result().ifPresent(nbt -> slotObj.addProperty("nbt", nbt.toString()));

                                                    slots.add(slotObj);
                                                }

                                                root.add("slots", slots);

                                                String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
                                                mc.keyboardHandler.setClipboard(json);

                                                MainCommand.sendMessage("Copied data to clipboard!", true);
                                            }
                                    ).bounds(x, y + 200, 110, 20)
                                    .tooltip(Tooltip.create(Component.literal("Copies GUI NBT as JSON.")))
                                    .build()
                    );

                    if (mod.invTweaksSetting.getValue() && screen instanceof AbstractContainerScreen<?> hs && !(screen instanceof CreativeModeInventoryScreen)) {
                        HandledScreenAccessor hsa = (HandledScreenAccessor) hs;
                        int guiX = hsa.dupersunited$getGuiX();
                        int guiY = hsa.dupersunited$getGuiY();
                        int tweakY = guiY - 24;

                        ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                                Button.builder(
                                                Component.literal("Steal").withStyle(s -> s.withColor(0xa6e3a1)),
                                                btn -> {
                                                    List<Slot> slots = hs.getMenu().slots;
                                                    int containerSlotCount = slots.size() - 36;
                                                    for (int i = 0; i < containerSlotCount; i++) {
                                                        if (slots.get(i).hasItem()) {
                                                            client.gameMode.handleContainerInput(
                                                                    client.player.containerMenu.containerId,
                                                                    i, 0, ContainerInput.QUICK_MOVE, client.player
                                                            );
                                                        }
                                                    }
                                                }
                                        )
                                        .bounds(guiX, tweakY, 53, 20)
                                        .tooltip(Tooltip.create(Component.literal("Steals all items in container.")))
                                        .build()
                        );

                        ((ScreenAccessor) screen).dupersunited$addRenderableWidget(
                                Button.builder(
                                                Component.literal("Dump").withStyle(s -> s.withColor(ColorUtil.RED2)),
                                                btn -> {
                                                    List<Slot> slots = hs.getMenu().slots;
                                                    int total = slots.size();
                                                    for (int i = total - 36; i < total; i++) {
                                                        if (slots.get(i).hasItem()) {
                                                            client.gameMode.handleContainerInput(
                                                                    client.player.containerMenu.containerId,
                                                                    i, 0, ContainerInput.QUICK_MOVE, client.player
                                                            );
                                                        }
                                                    }
                                                }
                                        )
                                        .bounds(guiX + 57, tweakY, 53, 20)
                                        .tooltip(Tooltip.create(Component.literal("Dumps all everything you have into a container.")))
                                        .build()
                        );
                    }
                }


                BookBotModule bookBot = MainClient.MODULE_MANAGER.getModule(BookBotModule.class);
                if (bookBot != null && bookBot.isEnabled() && (screen instanceof BookEditScreen || screen instanceof BookViewScreen)) {
                    if (client.player != null && client.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WRITABLE_BOOK)) {

                        int buttonWidth = 110;
                        int topRightX = scaledWidth - buttonWidth - 10;
                        int topRightY = 10;

                        Button writeBookButton = Button.builder(
                                        Component.literal("Start Auto Write").withStyle(s -> s.withColor(ColorUtil.MAUVE2)),
                                        btn -> {
                                            bookBot.startWriting();
                                            MainCommand.sendMessage("Starting book bot!", true);
                                            client.gui.setScreen(null);
                                        }
                                )
                                .bounds(topRightX, topRightY, buttonWidth, 20)
                                .tooltip(Tooltip.create(Component.literal("Click this to start the book bot macro!")))
                                .build();

                        ((ScreenAccessor) screen).dupersunited$addRenderableWidget(writeBookButton);
                    }
                }

                if (GuiMacro.getInstance().isRecording) {
                    Button endMacroButton = Button.builder(
                            Component.literal("End MacroGUI Recording").withStyle(s -> s.withColor(ColorUtil.RED2)),
                            btn -> {
                                GuiMacro.getInstance().finalizeRecording();
                                Minecraft.getInstance().gui.setScreen(client.gui.screen());
                            }
                        ).bounds(x, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 30, 170, 20)
                        .tooltip(Tooltip.create(Component.literal("Finalizes the macro you are currently recording.")))
                        .build();
                    ((ScreenAccessor) screen).dupersunited$addRenderableWidget(endMacroButton);
                }
            }
        });
    }

    private static boolean shouldAttachToScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>
                || screen instanceof SignEditScreen
                || screen instanceof BookViewScreen
                || screen instanceof BookEditScreen
                || screen instanceof DeathScreen;
    }
}