package wtf.dupers.dupersunited.modules.render;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.FloatSetting;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class FreecamModule extends Module {
    public final FloatSetting speed = register(new FloatSetting("Speed", 1f, 0f, 10f));

    public double posX, posY, posZ;
    public double prevPosX, prevPosY, prevPosZ;
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private CameraType prePerspective;
    private boolean forward, backward, left, right, up, down;

    public FreecamModule() {
        super("Freecam", "Detaches your camera from your player.", Category.render);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) return;

        yaw       = mc.player.getYRot();
        pitch     = mc.player.getXRot();
        lastYaw   = yaw;
        lastPitch = pitch;

        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        posX = prevPosX = camPos.x;
        posY = prevPosY = camPos.y;
        posZ = prevPosZ = camPos.z;

        prePerspective = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        unpress();
    }

    @Override
    protected void onDisable() {
        mc.options.setCameraType(prePerspective);
        forward = backward = left = right = up = down = false;
        unpress();
        if (mc.player != null) mc.player.noPhysics = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK)
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        forward  = isKeyDown(mc.options.keyUp.getDefaultKey().getValue());
        backward = isKeyDown(mc.options.keyDown.getDefaultKey().getValue());
        left     = isKeyDown(mc.options.keyLeft.getDefaultKey().getValue());
        right    = isKeyDown(mc.options.keyRight.getDefaultKey().getValue());
        up       = isKeyDown(mc.options.keyJump.getDefaultKey().getValue());
        down     = isKeyDown(mc.options.keyShift.getDefaultKey().getValue());

        Vec3 fwd  = Vec3.directionFromRotation(0, yaw);
        Vec3 side = Vec3.directionFromRotation(0, yaw + 90);

        double velX = 0, velY = 0, velZ = 0;
        float s = speed.getValue() * 0.5f;

        boolean movingForward = false;
        boolean movingLateral = false;

        if (forward)  { velX += fwd.x  * s; velZ += fwd.z  * s; movingForward = true; }
        if (backward) { velX -= fwd.x  * s; velZ -= fwd.z  * s; movingForward = true; }
        if (right)    { velX += side.x * s; velZ += side.z * s; movingLateral = true; }
        if (left)     { velX -= side.x * s; velZ -= side.z * s; movingLateral = true; }

        if (movingForward && movingLateral) {
            double diag = 1.0 / Math.sqrt(2);
            velX *= diag;
            velZ *= diag;
        }

        if (up)   velY += s;
        if (down) velY -= s;

        prevPosX = posX; prevPosY = posY; prevPosZ = posZ;
        posX += velX; posY += velY; posZ += velZ;
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw   = yaw;
        lastPitch = pitch;

        yaw   += (float) deltaX;
        pitch += (float) deltaY;
        pitch  = Mth.clamp(pitch, -90f, 90f);
    }

    public double getLerpedX(float tickDelta)     { return Mth.lerp(tickDelta, prevPosX, posX); }
    public double getLerpedY(float tickDelta)     { return Mth.lerp(tickDelta, prevPosY, posY); }
    public double getLerpedZ(float tickDelta)     { return Mth.lerp(tickDelta, prevPosZ, posZ); }
    public double getLerpedYaw(float tickDelta)   { return Mth.lerp(tickDelta, lastYaw,   yaw);   }
    public double getLerpedPitch(float tickDelta) { return Mth.lerp(tickDelta, lastPitch, pitch); }

    private void unpress() {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }

    private boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(mc.getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }
}