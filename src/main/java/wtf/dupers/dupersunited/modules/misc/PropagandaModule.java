package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static wtf.dupers.dupersunited.MainClient.mc;

public class PropagandaModule extends Module {
    private static final List<AdData> AD_REGISTRY = List.of(
        new AdData(Identifier.fromNamespaceAndPath("dupersunited", "textures/ads/dupes.png"),"https://discord.gg/dupes"),
        new AdData(Identifier.fromNamespaceAndPath("dupersunited", "textures/ads/loko.png"),"https://instagram.com/fakefourloko"),
        //new AdData(Identifier.fromNamespaceAndPath("dupersunited", "textures/ads/propaganda.png"),"https://discord.gg/palantir"),
        new AdData(Identifier.fromNamespaceAndPath("dupersunited", "textures/ads/larps.png"),"https://glitcha.wtf"),
        new AdData(Identifier.fromNamespaceAndPath("dupersunited", "textures/ads/please-feed-crosby.png"), "https://github.com/sponsors/crosby-moe")
    );

    private static final int SPAWN_INTERVAL = 30_000;

    private final List<AdInstance> activeAds = new ObjectArrayList<>();
    private long lastSpawnTime = 0;

    public PropagandaModule() {
        super("Propaganda", "no comment", "Misc");
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        long now = System.currentTimeMillis();
        if (now - this.lastSpawnTime >= SPAWN_INTERVAL) {
            AdData adData = AD_REGISTRY.get(ThreadLocalRandom.current().nextInt(AD_REGISTRY.size()));
            AbstractTexture texture = mc.getTextureManager().getTexture(adData.id());

            int textureWidth = texture.getTexture().getWidth(0);
            int textureHeight = texture.getTexture().getHeight(0);

            final float targetCoverage = 0.2F;

            int width;
            int height;
            if ((float) textureWidth / screenWidth > (float) textureHeight / screenHeight) {
                // clamp based on width
                float rawWidth = targetCoverage * screenWidth;
                float size = rawWidth / textureWidth;

                width = Math.round(rawWidth);
                height = Math.round(textureHeight * size);
            } else {
                // clamp based on height
                float rawHeight = targetCoverage * screenHeight;
                float size = rawHeight / textureHeight;

                width = Math.round(textureWidth * size);
                height = Math.round(rawHeight);
            }


            int x = ThreadLocalRandom.current().nextInt(Math.max(0, screenWidth - width));
            int y = ThreadLocalRandom.current().nextInt(Math.max(0, screenHeight - height));

            this.activeAds.add(new AdInstance(adData, x, y, width, height));
            this.lastSpawnTime = now;
        }

        for (AdInstance ad : this.activeAds) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ad.data().id(),
                ad.x(), ad.y(), 0, 0, ad.width(), ad.height(), ad.width(), ad.height());
        }
    }

    public boolean renderTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        for (AdInstance ad : this.activeAds) {
            if (ad.isMouseOver(x, y)) {
                return true; // delete other tooltips
            }
        }

        return false;
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        for (var it = this.activeAds.iterator(); it.hasNext();) {
            AdInstance ad = it.next();
            if (ad.isMouseOver(click.x(), click.y())) {
                if (click.button() == 0) ad.click();
                else it.remove();

                return true;
            }
        }

        return false;
    }

    private record AdData(Identifier id, String url) {}

    private record AdInstance(AdData data, int x, int y, int width, int height) {
        private void click() {
            try {
                String url = this.data().url();
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception e) {
                MainClient.LOGGER.error("Error opening ad url", e);
            }
        }

        private boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= this.x() && mouseX < this.x() + this.width() && mouseY >= this.y() && mouseY < this.y() + this.height();
        }
    }
}