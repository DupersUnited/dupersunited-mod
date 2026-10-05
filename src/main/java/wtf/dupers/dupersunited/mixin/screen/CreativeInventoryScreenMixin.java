package wtf.dupers.dupersunited.mixin.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Shadow
    private boolean ignoreTextInput;

    @Shadow
    private static CreativeModeTab selectedTab;

    public CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void dupersunited$cacheServer(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (this.ignoreTextInput) cir.setReturnValue(false);
        if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) return;
        super.charTyped(input);
    }
}