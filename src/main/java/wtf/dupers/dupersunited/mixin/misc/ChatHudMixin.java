package wtf.dupers.dupersunited.mixin.misc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.misc.ChatStackerModule;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ChatComponent.class)
public class ChatHudMixin {
    @Shadow @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Unique private static final int STACK_RECENCY_LINES = 12;
    @Unique private final Map<Component, ChatStackerModule.RepeatingMessage> messages = new HashMap<>();
    @Unique private volatile boolean wasLastMessageBlank = false; // ChatHud isn't thread-safe but mods dont give a fuck

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
    public void dupersunited$addVisibleMessage(GuiMessage message, CallbackInfo ci, @Share("message") LocalRef<MutableComponent> messageRef, @Share("messageData") LocalRef<ChatStackerModule.RepeatingMessage> messageDataRef) {
        if (MainClient.MODULE_MANAGER.isEnabled(ChatStackerModule.class)) {
            String plainText = message.content().getString().strip();

            if (plainText.matches("[\\-=+*_~]+")) { // avoid touching separators
                return;
            }

            if (plainText.isBlank()) {
                // wipe duplicate blank lines
                if (wasLastMessageBlank) {
                    ci.cancel();
                }
                wasLastMessageBlank = true;
            } else {
                wasLastMessageBlank = false;
            }

            ChatStackerModule.RepeatingMessage messageData = messages.get(message.content());

            if (messageData != null && dupersunited$isRecent(messageData)) {
                trimmedMessages.removeAll(messageData.instances());
                messageData.instances().clear();

                messageRef.set(messageData.originalMessage()
                    .copy()
                    .append(" ")
                    .append(Component.literal("(" + messageData.count().incrementAndGet() + ")")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))));
            } else {
                messageData = new ChatStackerModule.RepeatingMessage(message.content().copy(), new ArrayList<>(), new MutableInt(1));
                messages.put(message.content(), messageData);
            }

            messageDataRef.set(messageData);
        }
    }

    @Unique
    private boolean dupersunited$isRecent(ChatStackerModule.RepeatingMessage msg) {
        if (msg.instances().isEmpty()) return false;
        // check if any of its visible lines are within the recency window
        for (GuiMessage.Line instance : msg.instances()) {
            int idx = trimmedMessages.indexOf(instance);
            if (idx >= 0 && idx < STACK_RECENCY_LINES) return true;
        }
        return false;
    }

    @WrapOperation(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    public List<FormattedCharSequence> dupersunited$addMessage(GuiMessage instance, Font font, int maxWidth, Operation<List<FormattedCharSequence>> original, @Share("message") LocalRef<MutableComponent> overriddenMessage) {
        if (overriddenMessage.get() != null) {
            return ComponentRenderUtils.wrapComponents(overriddenMessage.get(), maxWidth, font);
        } else {
            return original.call(instance, font, maxWidth);
        }
    }

    @Redirect(method = "addMessageToDisplayQueue", at = @At(value = "NEW", target = "(Lnet/minecraft/client/multiplayer/chat/GuiMessage;Lnet/minecraft/util/FormattedCharSequence;Z)Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"))
    public GuiMessage.Line dupersunited$createLine(GuiMessage parent, FormattedCharSequence content, boolean endOfEntry, @Share("messageData") LocalRef<ChatStackerModule.RepeatingMessage> messageData) {
        GuiMessage.Line visible = new GuiMessage.Line(parent, content, endOfEntry);
        if (messageData.get() != null) messageData.get().instances().add(visible);
        return visible;
    }
}