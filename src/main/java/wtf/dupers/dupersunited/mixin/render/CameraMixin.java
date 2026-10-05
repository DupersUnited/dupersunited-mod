package wtf.dupers.dupersunited.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.FreeLookModule;
import wtf.dupers.dupersunited.modules.render.FreecamModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private Entity entity;
    @Shadow private boolean detached;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    public abstract float getCameraEntityPartialTicks(DeltaTracker deltaTracker);

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(DeltaTracker deltaTracker, CallbackInfo ci) {
        float tickDelta = getCameraEntityPartialTicks(deltaTracker);

        FreecamModule freecam = MainClient.MODULE_MANAGER.getModule(FreecamModule.class);
        if (freecam != null && freecam.isEnabled() && entity != null) {
            this.setPosition(new Vec3(freecam.getLerpedX(tickDelta), freecam.getLerpedY(tickDelta), freecam.getLerpedZ(tickDelta)));
            this.setRotation((float) freecam.getLerpedYaw(tickDelta), (float) freecam.getLerpedPitch(tickDelta));
            return;
        }

        if (entity == null || !detached) return;

        FreeLookModule freeLook = MainClient.MODULE_MANAGER.getModule(FreeLookModule.class);
        if (freeLook == null || !freeLook.isEnabled()) return;

        double renderX = entity.getPosition(tickDelta).x;
        double renderY = entity.getPosition(tickDelta).y + entity.getEyeHeight();
        double renderZ = entity.getPosition(tickDelta).z;

        double distance = 4.0;
        double radYaw   = Math.toRadians(freeLook.smoothYaw);

        double x = renderX - Math.sin(radYaw) * distance;
        double z = renderZ + Math.cos(radYaw) * distance;

        this.setPosition(new Vec3(x, renderY, z));

        double dx = renderX - x;
        double dy = 0;
        double dz = renderZ - z;

        float facingYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float facingPitch = -(float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        this.setRotation(facingYaw, facingPitch);
    }
}