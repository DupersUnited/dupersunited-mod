package wtf.dupers.dupersunited.features.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import wtf.dupers.dupersunited.MainClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GlbCosmeticModel {
    private static final int GLB_MAGIC = 0x46546C67;
    private static final int UNSIGNED_BYTE = 5121;
    private static final int UNSIGNED_SHORT = 5123;
    private static final int UNSIGNED_INT = 5125;
    private static final String MODEL_PATH = "/assets/dupersunited/models/cosmetics/";
    private static final GlbCosmeticModel EMPTY = new GlbCosmeticModel("empty", List.of(), null);

    private final String name;
    private final List<Primitive> primitives;
    private final byte[] texture;
    private Identifier textureId;

    private GlbCosmeticModel(String name, List<Primitive> primitives, byte[] texture) {
        this.name = name;
        this.primitives = primitives;
        this.texture = texture;
    }

    public static GlbCosmeticModel load(String name) {
        try (InputStream input = GlbCosmeticModel.class.getResourceAsStream(MODEL_PATH + name)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + name);
            }

            ByteBuffer glb = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
            readHeader(glb);
            JsonObject json = JsonParser.parseString(
                    new String(readChunk(glb), StandardCharsets.UTF_8).trim()
            ).getAsJsonObject();
            ByteBuffer binary = ByteBuffer.wrap(readChunk(glb)).order(ByteOrder.LITTLE_ENDIAN);
            return parse(name, json, binary);
        } catch (Exception exception) {
            MainClient.LOGGER.error("Failed to load cosmetic GLB {}", name, exception);
            return EMPTY;
        }
    }

    private static void readHeader(ByteBuffer glb) {
        if (glb.getInt() != GLB_MAGIC) {
            throw new IllegalArgumentException("Invalid GLB");
        }
        glb.getInt();
        glb.getInt();
    }

    private static byte[] readChunk(ByteBuffer glb) {
        int length = glb.getInt();
        glb.getInt();
        byte[] chunk = new byte[length];
        glb.get(chunk);
        return chunk;
    }

    private static GlbCosmeticModel parse(String name, JsonObject json, ByteBuffer binary) {
        List<Primitive> primitives = new ArrayList<>();
        int sceneIndex = json.has("scene") ? json.get("scene").getAsInt() : 0;
        JsonObject scene = objectAt(json, "scenes", sceneIndex);

        for (JsonElement root : scene.getAsJsonArray("nodes")) {
            loadNode(json, binary, root.getAsInt(), new Matrix4f(), primitives);
        }

        normalize(primitives);
        return new GlbCosmeticModel(name, List.copyOf(primitives), readTexture(json, binary));
    }

    private static void loadNode(JsonObject json, ByteBuffer binary, int index,
                                 Matrix4f parent, List<Primitive> primitives) {
        JsonObject node = objectAt(json, "nodes", index);
        Matrix4f world = new Matrix4f(parent).mul(nodeTransform(node));

        if (node.has("mesh")) {
            loadMesh(json, binary, node.get("mesh").getAsInt(), world, primitives);
        }
        if (node.has("children")) {
            for (JsonElement child : node.getAsJsonArray("children")) {
                loadNode(json, binary, child.getAsInt(), world, primitives);
            }
        }
    }

    private static void loadMesh(JsonObject json, ByteBuffer binary, int meshIndex,
                                 Matrix4f transform, List<Primitive> primitives) {
        JsonArray meshPrimitives = objectAt(json, "meshes", meshIndex).getAsJsonArray("primitives");
        for (JsonElement element : meshPrimitives) {
            JsonObject primitive = element.getAsJsonObject();
            JsonObject attributes = primitive.getAsJsonObject("attributes");
            float[] positions = readFloats(json, binary, attributes.get("POSITION").getAsInt(), 3);
            float[] normals = attributes.has("NORMAL")
                    ? readFloats(json, binary, attributes.get("NORMAL").getAsInt(), 3)
                    : new float[positions.length];
            float[] textureCoordinates = attributes.has("TEXCOORD_0")
                    ? readFloats(json, binary, attributes.get("TEXCOORD_0").getAsInt(), 2)
                    : new float[positions.length / 3 * 2];

            applyTransform(positions, normals, transform);
            int[] indices = readIndices(json, binary, primitive.get("indices").getAsInt());
            primitives.add(new Primitive(positions, normals, textureCoordinates, indices));
        }
    }

    private static Matrix4f nodeTransform(JsonObject node) {
        if (node.has("matrix")) {
            JsonArray values = node.getAsJsonArray("matrix");
            float[] matrix = new float[16];
            for (int index = 0; index < matrix.length; index++) {
                matrix[index] = values.get(index).getAsFloat();
            }
            return new Matrix4f().set(matrix);
        }

        Vector3f translation = new Vector3f();
        Vector3f scale = new Vector3f(1);
        Quaternionf rotation = new Quaternionf();

        if (node.has("translation")) {
            JsonArray values = node.getAsJsonArray("translation");
            translation.set(values.get(0).getAsFloat(), values.get(1).getAsFloat(),
                    values.get(2).getAsFloat());
        }
        if (node.has("scale")) {
            JsonArray values = node.getAsJsonArray("scale");
            scale.set(values.get(0).getAsFloat(), values.get(1).getAsFloat(),
                    values.get(2).getAsFloat());
        }
        if (node.has("rotation")) {
            JsonArray values = node.getAsJsonArray("rotation");
            rotation.set(values.get(0).getAsFloat(), values.get(1).getAsFloat(),
                    values.get(2).getAsFloat(), values.get(3).getAsFloat());
        }
        return new Matrix4f().translationRotateScale(translation, rotation, scale);
    }

    private static void applyTransform(float[] positions, float[] normals, Matrix4f matrix) {
        Matrix3f normalMatrix = new Matrix3f(matrix).invert().transpose();
        Vector3f vector = new Vector3f();

        for (int index = 0; index < positions.length; index += 3) {
            matrix.transformPosition(vector.set(
                    positions[index], positions[index + 1], positions[index + 2]));
            positions[index] = vector.x;
            positions[index + 1] = vector.y;
            positions[index + 2] = vector.z;

            normalMatrix.transform(vector.set(
                    normals[index], normals[index + 1], normals[index + 2])).normalize();
            normals[index] = vector.x;
            normals[index + 1] = vector.y;
            normals[index + 2] = vector.z;
        }
    }

    private static void normalize(List<Primitive> primitives) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (Primitive primitive : primitives) {
            for (int index = 0; index < primitive.positions.length; index += 3) {
                minX = Math.min(minX, primitive.positions[index]);
                minY = Math.min(minY, primitive.positions[index + 1]);
                minZ = Math.min(minZ, primitive.positions[index + 2]);
                maxX = Math.max(maxX, primitive.positions[index]);
                maxZ = Math.max(maxZ, primitive.positions[index + 2]);
            }
        }

        float centerX = (minX + maxX) / 2;
        float centerZ = (minZ + maxZ) / 2;
        for (Primitive primitive : primitives) {
            for (int index = 0; index < primitive.positions.length; index += 3) {
                primitive.positions[index] -= centerX;
                primitive.positions[index + 1] -= minY;
                primitive.positions[index + 2] -= centerZ;
            }
        }
    }

    private static float[] readFloats(JsonObject json, ByteBuffer binary,
                                     int accessorIndex, int components) {
        JsonObject accessor = objectAt(json, "accessors", accessorIndex);
        JsonObject view = objectAt(json, "bufferViews", accessor.get("bufferView").getAsInt());
        int count = accessor.get("count").getAsInt();
        int start = byteOffset(view) + byteOffset(accessor);
        int stride = view.has("byteStride") ? view.get("byteStride").getAsInt() : components * Float.BYTES;
        float[] values = new float[count * components];

        for (int index = 0; index < count; index++) {
            for (int component = 0; component < components; component++) {
                values[index * components + component] = binary.getFloat(
                        start + index * stride + component * Float.BYTES);
            }
        }
        return values;
    }

    private static int[] readIndices(JsonObject json, ByteBuffer binary, int accessorIndex) {
        JsonObject accessor = objectAt(json, "accessors", accessorIndex);
        JsonObject view = objectAt(json, "bufferViews", accessor.get("bufferView").getAsInt());
        int count = accessor.get("count").getAsInt();
        int type = accessor.get("componentType").getAsInt();
        int start = byteOffset(view) + byteOffset(accessor);
        int[] indices = new int[count];

        for (int index = 0; index < count; index++) {
            indices[index] = switch (type) {
                case UNSIGNED_BYTE -> binary.get(start + index) & 0xFF;
                case UNSIGNED_SHORT -> binary.getShort(start + index * Short.BYTES) & 0xFFFF;
                case UNSIGNED_INT -> binary.getInt(start + index * Integer.BYTES);
                default -> throw new IllegalArgumentException("Index type " + type);
            };
        }
        return indices;
    }

    private static byte[] readTexture(JsonObject json, ByteBuffer binary) {
        if (!json.has("images")) return null;

        JsonObject image = objectAt(json, "images", 0);
        JsonObject view = objectAt(json, "bufferViews", image.get("bufferView").getAsInt());
        byte[] bytes = new byte[view.get("byteLength").getAsInt()];
        ByteBuffer copy = binary.duplicate();
        copy.position(byteOffset(view));
        copy.get(bytes);
        return bytes;
    }

    private static JsonObject objectAt(JsonObject json, String array, int index) {
        return json.getAsJsonArray(array).get(index).getAsJsonObject();
    }

    private static int byteOffset(JsonObject object) {
        return object.has("byteOffset") ? object.get("byteOffset").getAsInt() : 0;
    }

    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light) {
        if (primitives.isEmpty()) return;

        prepareTexture();
        queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(textureId), (entry, vertices) -> {
            for (Primitive primitive : primitives) {
                primitive.render(entry, vertices, light);
            }
        });
    }

    private void prepareTexture() {
        if (textureId != null) return;

        textureId = Identifier.of("dupersunited", "cosmetics/" + name.replace('.', '_'));
        try {
            NativeImage image = texture == null ? new NativeImage(1, 1, false) : NativeImage.read(texture);
            if (texture == null) {
                image.setColorArgb(0, 0, 0xFFFFFFFF);
            }
            NativeImageBackedTexture nativeTexture = new NativeImageBackedTexture(
                    () -> "DupersUnited cosmetic " + name, image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, nativeTexture);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Primitive(float[] positions, float[] normals, float[] textureCoordinates, int[] indices) {
        private void render(MatrixStack.Entry entry, VertexConsumer vertices, int light) {
            for (int index = 0; index < indices.length; index += 3) {
                writeVertex(entry, vertices, light, indices[index]);
                writeVertex(entry, vertices, light, indices[index + 1]);
                writeVertex(entry, vertices, light, indices[index + 2]);
                writeVertex(entry, vertices, light, indices[index + 2]);
            }
        }

        private void writeVertex(MatrixStack.Entry entry, VertexConsumer vertices, int light, int index) {
            int position = index * 3;
            int texture = index * 2;
            vertices.vertex(entry, positions[position], positions[position + 1], positions[position + 2])
                    .color(0xFFFFFFFF)
                    .texture(textureCoordinates[texture], textureCoordinates[texture + 1])
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(entry, normals[position], normals[position + 1], normals[position + 2]);
        }
    }
}
