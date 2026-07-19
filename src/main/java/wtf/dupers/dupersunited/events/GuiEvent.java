package wtf.dupers.dupersunited.events;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.FabricatePackets;
import wtf.dupers.dupersunited.features.GuiPacketDelayManager;
import wtf.dupers.dupersunited.features.PacketPauseManager;
import wtf.dupers.dupersunited.features.SaveGuiManager;
import wtf.dupers.dupersunited.keybinds.PacketPauseKeybind;
import wtf.dupers.dupersunited.mixin.accessor.HandledScreenAccessor;
import wtf.dupers.dupersunited.mixin.accessor.ScreenAccessor;
import wtf.dupers.dupersunited.modules.exploit.BookBotModule;
import wtf.dupers.dupersunited.modules.glitcha.GuiUtilsModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class GuiEvent {
    private static final int MAUVE = 0xcba6f7;
    private static final int RED   = 0xf38ba8;

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
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                            ButtonWidget.builder(
                                            Text.literal("Close Without Packet").styled(s -> s.withColor(MAUVE)),
                                            button -> SaveGuiManager.saveAndCloseGui()
                                    )
                                    .dimensions(x, y, 110, 20)
                                    .tooltip(Tooltip.of(Text.of("Closes your GUI clientside and saves it to reopen later.")))
                                    .build()
                    );
                }

                if (mod.desyncSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                            ButtonWidget.builder(
                                            Text.literal("Desync").styled(s -> s.withColor(MAUVE)),
                                            btn -> {
                                                if (client.getNetworkHandler() != null && client.player != null) {
                                                    client.getNetworkHandler().sendPacket(
                                                            new net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket(
                                                                    client.player.currentScreenHandler.syncId
                                                            )
                                                    );
                                                }
                                            }
                                    )
                                    .dimensions(x + 115, y, 80, 20)
                                    .tooltip(Tooltip.of(Text.literal("Tells server GUI closed but keeps it open client-side.")))
                                    .build()
                    );
                }

                if (mod.clearGuiSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                            ButtonWidget.builder(
                                            Text.literal("Clear GUI Cache").styled(s -> s.withColor(MAUVE)),
                                            button -> {
                                                Screen previousScreen = SaveGuiManager.savedScreen;
                                                if (previousScreen != null) {
                                                    SaveGuiManager.savedScreen = null;
                                                    SaveGuiManager.deadGui = false;
                                                    MainCommand.sendMessage(Text.literal("Removed ")
                                                        .append(Text.literal(previousScreen.getTitle().getString()).formatted(Formatting.AQUA))
                                                        .append(" from saved screens."), true);
                                                } else {
                                                    MainCommand.sendMessage("You do not have a currently saved GUI!", true);
                                                }
                                            }
                                    )
                                    .dimensions(x, y + 25, 110, 20)
                                    .tooltip(Tooltip.of(Text.literal("Clears your saved GUI.")))
                                    .build()
                    );
                }

                if (mod.disconnectAndSendSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                            ButtonWidget.builder(
                                            Text.literal("DC & Send Packets").styled(s -> s.withColor(MAUVE)),
                                            btn -> {
                                                if (client.getNetworkHandler() == null) return;
                                                if (PacketPauseManager.isPaused()) PacketPauseKeybind.handleToggle();
                                                if (GuiPacketDelayManager.isPaused()) GuiPacketDelayManager.resume();
                                                TickEvent.pendingDisconnectTicks = 1;
                                            }
                                    )
                                    .dimensions(x, y + 50, 110, 20)
                                    .tooltip(Tooltip.of(Text.literal("Sends all currently queued packets (if there's any) and disconnects you from the server.")))
                                    .build()
                    );
                }

                if (mod.delayPackets.getValue()) {
                    boolean paused = GuiPacketDelayManager.isPaused();
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                            ButtonWidget.builder(
                                            Text.literal("Delay Packets: ").styled(s -> s.withColor(MAUVE))
                                                    .append(Text.literal(paused ? "ON" : "OFF")
                                                            .styled(s -> s.withColor(paused ? 0xa6e3a1 : 0xf38ba8))),
                                            btn -> {
                                                GuiPacketDelayManager.toggle();
                                                MinecraftClient.getInstance().setScreen(MinecraftClient.getInstance().currentScreen);
                                            }
                                    )
                                    .dimensions(x, y + 75, 110, 20)
                                    .tooltip(Tooltip.of(Text.literal("ONLY pauses GUI related packets.")))
                                    .build()
                    );
                }

                if (mod.saveGuiButtonSetting.getValue()) {
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                            ButtonWidget.builder(
                                            Text.literal("Save Gui").styled(s -> s.withColor(MAUVE)),
                                            btn -> SaveGuiManager.saveGui()
                                    )
                                    .dimensions(x, y + 100, 110, 20)
                                    .tooltip(Tooltip.of(Text.literal("Saves the current GUI.")))
                                    .build()
                    );
                }

                if (mod.commandBoxSetting.getValue()) {
                    int commandY = y + 125;
                    TextFieldWidget chatBox = new TextFieldWidget(
                            client.textRenderer,
                            x, commandY,
                            110, 18,
                            Text.literal("Chat")
                    ) {
                        @Override
                        public boolean keyPressed(KeyInput input) {
                            if (input.key() == 257 || input.key() == 335) {
                                String text = this.getText().trim();
                                if (text.isEmpty() || client.player == null) return false;
                                client.execute(() -> {
                                    if (text.startsWith("/"))
                                        client.player.networkHandler.sendChatCommand(text.substring(1));
                                    else
                                        client.player.networkHandler.sendChatMessage(text);
                                });
                                this.setText("");
                                return true;
                            }
                            return super.keyPressed(input);
                        }
                    };

                    chatBox.setMaxLength(256);
                    chatBox.setPlaceholder(Text.literal("Chat or Command").styled(s -> s.withColor(0x888888)));
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(chatBox);

                 /*   ((ScreenAccessor) screen).dupersunited$addDrawableChild(
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
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                            ButtonWidget.builder(
                                            Text.literal("Fabricate Packet").styled(s -> s.withColor(MAUVE)),
                                            btn -> FabricatePackets.open()
                                    )
                                    .dimensions(x, fabY, 110, 20)
                                    .tooltip(Tooltip.of(Text.literal("Fabricate and send a custom ClickSlot or ButtonClick packet")))
                                    .build()
                    );

                    if (client.player != null) {
                        ButtonWidget syncIdBtn = ButtonWidget.builder(
                                Text.literal("Sync Id: " + client.player.currentScreenHandler.syncId),
                                btn -> {})
                            .dimensions(x, y + 172, 110, 12)
                            .build();
                        syncIdBtn.active = false;
                        ((ScreenAccessor) screen).dupersunited$addDrawableChild(syncIdBtn);

                        ButtonWidget revisionBtn = ButtonWidget.builder(
                            Text.literal("Revision: " + client.player.currentScreenHandler.getRevision()),
                            btn -> {})
                            .dimensions(x, y + 184, 110, 12)
                            .build();
                        revisionBtn.active = false;
                        ((ScreenAccessor) screen).dupersunited$addDrawableChild(revisionBtn);
                    }
                }

                if (mod.copyGuiInfo.getValue() && screen instanceof HandledScreen<?> handledScreen) {
                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(ButtonWidget.builder(
                                            Text.literal("Copy GUI as JSON").styled(s -> s.withColor(MAUVE)),
                                            btn -> {
                                                JsonObject root = new JsonObject();
                                                root.addProperty("title", handledScreen.getTitle().getString());

                                                JsonArray slots = new JsonArray();
                                                for (Slot slot : handledScreen.getScreenHandler().slots) {
                                                    if (!slot.hasStack()) continue;
                                                    ItemStack stack = slot.getStack();

                                                    JsonObject slotObj = new JsonObject();
                                                    slotObj.addProperty("index", slot.getIndex());
                                                    slotObj.addProperty("id", Registries.ITEM.getId(stack.getItem()).toString());

                                                    ItemStack.CODEC.encodeStart(
                                                            MinecraftClient.getInstance().world.getRegistryManager().getOps(NbtOps.INSTANCE), stack
                                                    ).result().ifPresent(nbt -> slotObj.addProperty("nbt", nbt.toString()));

                                                    slots.add(slotObj);
                                                }

                                                root.add("slots", slots);

                                                String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
                                                MinecraftClient.getInstance().keyboard.setClipboard(json);

                                                MainCommand.sendMessage("Copied data to clipboard!", true);
                                            }
                                    ).dimensions(x, y + 200, 110, 20)
                                    .tooltip(Tooltip.of(Text.literal("Copies GUI NBT as JSON.")))
                                    .build()
                    );

                    if (mod.invTweaksSetting.getValue() && screen instanceof HandledScreen<?> hs && !(screen instanceof CreativeInventoryScreen)) {
                        HandledScreenAccessor hsa = (HandledScreenAccessor) hs;
                        int guiX = hsa.dupersunited$getGuiX();
                        int guiY = hsa.dupersunited$getGuiY();
                        int tweakY = guiY - 24;

                        ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                                ButtonWidget.builder(
                                                Text.literal("Steal").styled(s -> s.withColor(0xa6e3a1)),
                                                btn -> {
                                                    List<Slot> slots = hs.getScreenHandler().slots;
                                                    int containerSlotCount = slots.size() - 36;
                                                    for (int i = 0; i < containerSlotCount; i++) {
                                                        if (slots.get(i).hasStack()) {
                                                            client.interactionManager.clickSlot(
                                                                    client.player.currentScreenHandler.syncId,
                                                                    i, 0, SlotActionType.QUICK_MOVE, client.player
                                                            );
                                                        }
                                                    }
                                                }
                                        )
                                        .dimensions(guiX, tweakY, 53, 20)
                                        .tooltip(Tooltip.of(Text.literal("Steals all items in container.")))
                                        .build()
                        );

                        ((ScreenAccessor) screen).dupersunited$addDrawableChild(
                                ButtonWidget.builder(
                                                Text.literal("Dump").styled(s -> s.withColor(RED)),
                                                btn -> {
                                                    List<Slot> slots = hs.getScreenHandler().slots;
                                                    int total = slots.size();
                                                    for (int i = total - 36; i < total; i++) {
                                                        if (slots.get(i).hasStack()) {
                                                            client.interactionManager.clickSlot(
                                                                    client.player.currentScreenHandler.syncId,
                                                                    i, 0, SlotActionType.QUICK_MOVE, client.player
                                                            );
                                                        }
                                                    }
                                                }
                                        )
                                        .dimensions(guiX + 57, tweakY, 53, 20)
                                        .tooltip(Tooltip.of(Text.literal("Dumps all everything you have into a container.")))
                                        .build()
                        );
                    }
                }


                BookBotModule bookBot = MainClient.MODULE_MANAGER.getModule(BookBotModule.class);
                if (bookBot != null && bookBot.isEnabled() && (screen instanceof BookEditScreen || screen instanceof BookScreen)) {
                    if (client.player != null && client.player.getMainHandStack().isOf(Items.WRITABLE_BOOK)) {

                        int buttonWidth = 110;
                        int topRightX = scaledWidth - buttonWidth - 10;
                        int topRightY = 10;

                        ButtonWidget writeBookButton = ButtonWidget.builder(
                                        Text.literal("Start Auto Write").styled(s -> s.withColor(MAUVE)),
                                        btn -> {
                                            bookBot.startWriting();
                                            MainCommand.sendMessage("Starting book bot!", true);
                                            client.setScreen(null);
                                        }
                                )
                                .dimensions(topRightX, topRightY, buttonWidth, 20)
                                .tooltip(Tooltip.of(Text.literal("Click this to start the book bot macro!")))
                                .build();

                        ((ScreenAccessor) screen).dupersunited$addDrawableChild(writeBookButton);
                    }
                }

//                if (GuiMacro.isRecording) {
//                    ((ScreenAccessor) screen).dupersunited$addDrawableChild(
//                            ButtonWidget.builder(
//                                            Text.literal("End MacroGUI Recording").styled(s -> s.withColor(RED)),
//                                            btn -> {
//                                                GuiMacro.finalizeRecording();
//                                                MinecraftClient.getInstance().setScreen(client.currentScreen);
//                                            }
//                                    )
//                                    .dimensions(x, MinecraftClient.getInstance().getWindow().getScaledHeight() - 30, 170, 20)
//                                    .tooltip(Tooltip.of(Text.literal("Finalizes the macro you are currently recording.")))
//                                    .build()
//                    );
//                }
            }
        });
    }

    private static boolean shouldAttachToScreen(Screen screen) {
        return screen instanceof HandledScreen<?>
                || screen instanceof SignEditScreen
                || screen instanceof BookScreen
                || screen instanceof BookEditScreen
                || screen instanceof DeathScreen;
    }
}