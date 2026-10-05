package wtf.dupers.dupersunited.mixin.entity;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.modules.render.EspModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract UUID getUUID();

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void forceGlow(CallbackInfoReturnable<Boolean> cir) {
        EspModule esp = MainClient.MODULE_MANAGER.getModule(EspModule.class);
        if (!esp.isEnabled()) {
            return;
        }
        if (EspModule.selectedEntityIds.isEmpty()) {
            return;
        }

        Entity self = (Entity) (Object) this;
        if (!EspModule.selectedEntityIds.contains(self.getType())) {
            return;
        }

        if (self instanceof Player) {
            if (this.getUUID().version() != 4) {
                cir.setReturnValue(false);
            } else {
                cir.setReturnValue(true);
            }
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColor", at = {@At("RETURN")}, cancellable = true)
    public void changeColorValue(CallbackInfoReturnable<Integer> cir) {
        Module esp = MainClient.MODULE_MANAGER.getModuleByName("esp");
        if (!esp.isEnabled()) {
            return;
        }
        if (EspModule.selectedEntityIds.isEmpty()) {
            return;
        }

        Entity self = (Entity) (Object) this;
        if (!EspModule.selectedEntityIds.contains(self.getType())) {
            return;
        }

        if (self instanceof Player) {
            cir.setReturnValue(0xFFF800F8);
        } else if (self instanceof Mob) {
            cir.setReturnValue(0xFFD68542);
        } else if (self instanceof ItemEntity) {
            cir.setReturnValue(0xFFFFFFFF);
        } else {
            cir.setReturnValue(0xFFF800F8);
        }
    }
}