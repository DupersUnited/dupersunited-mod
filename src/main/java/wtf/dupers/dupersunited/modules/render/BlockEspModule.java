package wtf.dupers.dupersunited.modules.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.features.screens.BlockEspScreen;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.ButtonSetting;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static wtf.dupers.dupersunited.MainClient.mc;

public class BlockEspModule extends Module {

    /*
    THERES AN ISSUE WITH BLOCK ESP THAT JUST MAKES IT NOT PROPERLY RENDER (sometimes) idk it's super odd,
    Please someone else fix it, thank you - litten
     */

    private static BlockEspModule instance;
    public static final Set<Block> selectedBlocks = new ReferenceOpenHashSet<>();

    private final CopyOnWriteArrayList<RenderShape> renderShapes = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BlockESP-Scanner");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean scanning = false;
    private BlockPos lastScanCenter = null;
    private int ticksSinceScan = SCAN_INTERVAL;
    private int lastRange = -1;

    private static final int SCAN_INTERVAL = 60;
    private static final int MARKER_INTERVAL = 60;
    private static final int MAX_RENDER = 50000;
    private static final int MAX_MARKERS = 4096;
    private static final Map<Block, BlockParticleOption> MARKERS = createMarkers();

    private final IntSetting range = register(new IntSetting("Range", 64, 16, 512));
    private final IntSetting red = register(new IntSetting("Red", 0, 0, 255));
    private final IntSetting green = register(new IntSetting("Green", 255, 0, 255));
    private final IntSetting blue = register(new IntSetting("Blue", 255, 0, 255));
    private final BooleanSetting markerIcons = register(new BooleanSetting("Invisible Icons", false));
    private volatile int markerTicks;

    private final ButtonSetting selectBlocks = register(
            new ButtonSetting("SelectBlocks", this::openScreen)
    );

    private static final RenderPipeline ESP_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("dupersunited", "pipeline/esp_lines"))
// TODO: I couldn't fucking figure out what this is. - khao 2026
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    public static final RenderType ESP_LINES = RenderType.create(
        "dupersunited_esp_lines",
        RenderSetup.builder(ESP_LINES_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup()
    );

    public BlockEspModule() {
        super("BlockESP", "Outlines blocks through blocks.", Category.render);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
        instance = this;
    }

    public static BlockEspModule getInstance() { return instance; }

    public void openScreen() {
        mc.gui.setScreen(new BlockEspScreen());
    }

    @Override
    public JsonElement writeJson() {
        JsonObject object = (JsonObject) super.writeJson();
        JsonArray espBlocks = new JsonArray();
        for (Block block : selectedBlocks) espBlocks.add(BuiltInRegistries.BLOCK.getKey(block).toString());
        object.add("selected-blocks", espBlocks);
        return object;
    }

    @Override
    public void readJson(JsonElement element) {
        super.readJson(element);
        if (element instanceof JsonObject object && object.has("selected-blocks")) {
            selectedBlocks.clear();
            for (JsonElement el : object.getAsJsonArray("selected-blocks")) {
                BuiltInRegistries.BLOCK.get(Identifier.tryParse(el.getAsString())).ifPresent(entry -> selectedBlocks.add(entry.value()));
            }
        }
    }

    @Override
    public void onEnable() {
        markerTicks = 0;
        invalidateCache();
    }

    @Override
    public void onDisable() { renderShapes.clear(); }

    @Override
    public void onTick() {
        if (selectedBlocks.isEmpty()) { renderShapes.clear(); return; }

        if (mc.level == null || mc.player == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        int currentRange = range.getValue();
        ticksSinceScan++;

        if (currentRange != lastRange) {
            lastRange = currentRange;
            invalidateCache();
        }

        boolean movedFar = lastScanCenter == null
                || Math.abs(playerPos.getX() - lastScanCenter.getX()) > 16
                || Math.abs(playerPos.getY() - lastScanCenter.getY()) > 16
                || Math.abs(playerPos.getZ() - lastScanCenter.getZ()) > 16;

        if ((ticksSinceScan >= SCAN_INTERVAL || movedFar) && !scanning) {
            ticksSinceScan = 0;
            lastScanCenter = playerPos.immutable();
            scheduleRescan(mc.level, playerPos.immutable(), currentRange);
        }

        if (markerIcons.getValue() && markerTicks++ % MARKER_INTERVAL == 0) spawnMarkers();
    }

    public void invalidateCache() {
        lastScanCenter = null;
        ticksSinceScan = SCAN_INTERVAL;
    }

    private void scheduleRescan(Level level, BlockPos center, int r) {
        Set<Block> snapshot = new ReferenceOpenHashSet<>(selectedBlocks);
        scanning = true;

        executor.submit(() -> {
            try {
                List<RenderShape> found = new ArrayList<>();
                rescan(level, center, r, snapshot, found);

                renderShapes.clear();
                renderShapes.addAll(found);
                markerTicks = 0;
            } finally {
                scanning = false;
            }
        });
    }

    /**
     * You don't have to know how this works, you just have to know that it works
     * @author Crosby
     */
    private void rescan(Level level, BlockPos center, int r, Set<Block> snapshot, List<RenderShape> found) {
        int cr = Math.ceilDiv(r, 16);
        int ox = SectionPos.blockToSectionCoord(center.getX());
        int oz = SectionPos.blockToSectionCoord(center.getZ());

        int wMinY = Math.max(level.getMinY(), center.getY() - r);
        int wMaxY = Math.min(level.getMaxY(), center.getY() + r);
        int cMinY = SectionPos.blockToSectionCoord(wMinY);
        int cMaxY = SectionPos.blockToSectionCoord(wMaxY);

        for (int cx = ox - cr; cx <= ox + cr; cx++) {
            for (int cz = oz - cr; cz <= oz + cr; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);

                int minX = Math.max(SectionPos.sectionToBlockCoord(cx, 0), center.getX() - r);
                int maxX = Math.min(SectionPos.sectionToBlockCoord(cx, 15), center.getX() + r);
                int minZ = Math.max(SectionPos.sectionToBlockCoord(cz, 0), center.getZ() - r);
                int maxZ = Math.min(SectionPos.sectionToBlockCoord(cz, 15), center.getZ() + r);

                for (int cy = cMinY; cy <= cMaxY; cy++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(cy));

                    if (section.maybeHas(state -> snapshot.contains(state.getBlock()))) {
                        int minY = Math.max(SectionPos.sectionToBlockCoord(cy, 0), wMinY);
                        int maxY = Math.min(SectionPos.sectionToBlockCoord(cy, 15), wMaxY);

                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                for (int x = minX; x <= maxX; x++) {
                                    BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                    if (snapshot.contains(state.getBlock())) {
                                        BlockPos pos = new BlockPos(x, y, z);
                                        VoxelShape shape = state.getShape(level, pos);
                                        found.add(new RenderShape(
                                            shape != Shapes.block() ? shape.singleEncompassing() : shape,
                                            pos,
                                            state.getBlock()
                                        ));
                                        if (found.size() >= MAX_RENDER) return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void spawnMarkers() {
        int count = 0;
        for (RenderShape renderShape : renderShapes) {
            BlockParticleOption marker = MARKERS.get(renderShape.block());
            if (marker == null) continue;
            BlockPos pos = renderShape.pos();
            mc.level.addParticle(
                marker, true, false,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                0.0, 0.0, 0.0
            );
            if (++count >= MAX_MARKERS) return;
        }
    }

    public static boolean hasMarker(Block block) {
        return MARKERS.containsKey(block);
    }

    private static Map<Block, BlockParticleOption> createMarkers() {
        Map<Block, BlockParticleOption> markers = new IdentityHashMap<>();
        for (Block block : List.of(
            Blocks.BARRIER,
            Blocks.LIGHT,
            Blocks.STRUCTURE_VOID,
            Blocks.STRUCTURE_BLOCK,
            Blocks.JIGSAW,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.NETHER_PORTAL,
            Blocks.END_PORTAL,
            Blocks.END_GATEWAY,
            Blocks.SPAWNER,
            Blocks.TRIAL_SPAWNER,
            Blocks.VAULT,
            Blocks.MOVING_PISTON,
            Blocks.PISTON_HEAD,
            Blocks.BEDROCK,
            Blocks.REINFORCED_DEEPSLATE
        )) {
            markers.put(block, new BlockParticleOption(ParticleTypes.BLOCK_MARKER, block.defaultBlockState()));
        }
        return markers;
    }

    public void onRender(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Vec3 cameraPos) {
        if (!isEnabled() || renderShapes.isEmpty()) return;

        int color = ARGB.color(red.getValue(), green.getValue(), blue.getValue());
        int count = 0;

        for (RenderShape renderShape : renderShapes) {
            if (count++ >= MAX_RENDER) break;

            poseStack.pushPose();
            poseStack.translate(
                renderShape.pos().getX() - cameraPos.x,
                renderShape.pos().getY() - cameraPos.y,
                renderShape.pos().getZ() - cameraPos.z
            );

            submitNodeCollector.submitShapeOutline(
                poseStack,
                renderShape.shape(),
                ESP_LINES,
                color,
                1.5f,
                false
            );

            poseStack.popPose();
        }
    }

    private record RenderShape(VoxelShape shape, BlockPos pos, Block block) {}
}
