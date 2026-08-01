package wtf.dupers.dupersunited.features.cosmetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CosmeticCatalog {
    public enum Slot {
        HEAD,
        TAIL
    }

    public record Cube(float x1, float y1, float z1, float x2, float y2, float z2, int color) {}

    public record Item(String id, String name, Slot slot, String creator, String source,
                       String model, List<Cube> cubes) {}

    private static final String VOXEL = "https://sketchfab.com/VoxelBear";
    private static final String BUNNY =
            "https://sketchfab.com/3d-models/bunny-ears-c137a6601fc24e548a17d4fe689fe28d";
    private static final String DUCK =
            "https://sketchfab.com/3d-models/minecraft-style-duck-blockbench-2c7fd8edfbb5490489fbdcc5855035bf";
    private static final List<Item> ITEMS = build();

    private CosmeticCatalog() {}

    public static List<Item> forSlot(Slot slot) {
        return ITEMS.stream().filter(item -> item.slot() == slot).toList();
    }

    public static Item named(Slot slot, String name) {
        List<Item> items = forSlot(slot);
        return items.stream()
                .filter(item -> item.name().equals(name))
                .findFirst()
                .orElse(items.getFirst());
    }

    public static List<Item> search(Slot slot, String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return forSlot(slot).stream()
                .filter(item -> item.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || item.creator().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }

    private static List<Item> build() {
        return List.of(
                generated("none-head", "None", Slot.HEAD, List.of()),
                cosmetic("bunny-ears", "Bunny Ears", Slot.HEAD, "Silvia_Rose", BUNNY,
                        ears(0xFFF1D4D4, 0xFFFF94B7)),
                model("minecraft-duck", "Minecraft Duck", "Kadzuo", DUCK,
                        "minecraft_style_duck_blockbench.glb"),
                voxelHat("cap", "Cap", cap(0xFF3B82F6)),
                voxelHat("top-hat", "Top Hat", topHat()),
                voxelHat("red-mushroom", "Red Mushroom", mushroom(0xFFD83A3A)),
                voxelHat("propeller", "Propeller Hat", propeller()),
                voxelHat("beanie", "Beanie", beanie()),
                voxelHat("blue-mushroom", "Blue Mushroom", mushroom(0xFF4287D6)),
                voxelHat("witch-hat", "Witch Hat", witchHat()),
                voxelHat("green-mushroom", "Green Mushroom", mushroom(0xFF46A758)),
                voxelHat("paper-bag", "Paper Bag", paperBag()),
                voxelHat("straw-hat", "Straw Hat", strawHat()),
                voxelHat("crown", "Crown", crown()),
                voxelHat("chef-cap", "Chef Cap", chefCap()),
                catEars("slim-white-cat-ears", "Slim White Cat Ears",
                        0xFFD8D8D5, 0xFFF4F4F2, 0xFFFFAFC8),
                catEars("slim-pink-cat-ears", "Slim Pink Cat Ears",
                        0xFFC72C68, 0xFFFF72AD, 0xFFFFC4D9),
                catEars("slim-brown-cat-ears", "Slim Brown Cat Ears",
                        0xFF4A281B, 0xFF7B4930, 0xFFE8B995),
                generated("none-tail", "None", Slot.TAIL, List.of()),
                generated("cat-tail", "Cat Tail", Slot.TAIL, tail(0xFF30343B, false)),
                generated("fox-tail", "Fox Tail", Slot.TAIL, tail(0xFFD76B2D, true)),
                generated("bunny-puff", "Bunny Puff", Slot.TAIL, puff()),
                generated("raccoon-tail", "Raccoon Tail", Slot.TAIL, raccoon()),
                generated("dragon-tail", "Dragon Tail", Slot.TAIL, dragon()),
                generated("snow-catgirl-tail", "Snow Catgirl Tail", Slot.TAIL,
                        catgirlTail(0xFFF2F2F0, 0xFFB7DFFF, 0xFFFFAFC8)),
                generated("midnight-catgirl-tail", "Midnight Catgirl Tail", Slot.TAIL,
                        catgirlTail(0xFF17181D, 0xFF4B3A5C, 0xFFCF4058)),
                generated("orange-catgirl-tail", "Orange Catgirl Tail", Slot.TAIL,
                        catgirlTail(0xFFE8792F, 0xFF8C3F20, 0xFFFFE0B4))
        );
    }

    private static Item cosmetic(String id, String name, Slot slot, String creator, String source, List<Cube> cubes) {
        return new Item(id, name, slot, creator, source, "", cubes);
    }

    private static Item generated(String id, String name, Slot slot, List<Cube> cubes) {
        return cosmetic(id, name, slot, "DupersUnited", "", cubes);
    }

    private static Item voxelHat(String id, String name, List<Cube> cubes) {
        return cosmetic(id, name, Slot.HEAD, "VoxelBear", VOXEL, cubes);
    }

    private static Item model(String id, String name, String creator, String source, String model) {
        return new Item(id, name, Slot.HEAD, creator, source, model, List.of());
    }

    private static Item catEars(String id, String name, int outer, int shade, int inner) {
        return generated(id, name, Slot.HEAD, blockyCatEars(outer, shade, inner));
    }

    private static Cube cube(float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        return new Cube(x1, y1, z1, x2, y2, z2, color);
    }

    private static List<Cube> mirrored(Cube... leftSide) {
        List<Cube> cubes = new ArrayList<>(leftSide.length * 2);
        cubes.addAll(List.of(leftSide));
        for (Cube cube : leftSide) {
            cubes.add(new Cube(-cube.x2(), cube.y1(), cube.z1(), -cube.x1(), cube.y2(), cube.z2(), cube.color()));
        }
        return List.copyOf(cubes);
    }

    private static List<Cube> blockyCatEars(int outer, int shade, int inner) {
        float front = -0.03125f;
        float back = 0.03125f;
        float detail = front - 0.003f;

        return mirrored(
                cube(-0.375f, -0.5625f, front, -0.0625f, -0.5f, back, outer),
                cube(-0.375f, -0.625f, front, -0.0625f, -0.5625f, back, outer),
                cube(-0.375f, -0.6875f, front, -0.125f, -0.625f, back, outer),
                cube(-0.3125f, -0.75f, front, -0.125f, -0.6875f, back, outer),
                cube(-0.3125f, -0.8125f, front, -0.1875f, -0.75f, back, outer),
                cube(-0.3125f, -0.875f, front, -0.1875f, -0.8125f, back, outer),
                cube(-0.3125f, -0.6875f, detail, -0.125f, -0.5625f, front, shade),
                cube(-0.25f, -0.75f, detail, -0.125f, -0.6875f, front, shade),
                cube(-0.25f, -0.6875f, detail - 0.003f, -0.125f, -0.5625f, detail, inner),
                cube(-0.25f, -0.75f, detail - 0.003f, -0.1875f, -0.6875f, detail, inner)
        );
    }

    private static List<Cube> ears(int outer, int inner) {
        return mirrored(
                cube(-0.27f, -0.78f, -0.10f, -0.08f, -0.45f, 0.10f, outer),
                cube(-0.22f, -0.72f, -0.11f, -0.13f, -0.51f, -0.09f, inner)
        );
    }

    private static List<Cube> cap(int color) {
        return List.of(
                cube(-0.31f, -0.61f, -0.31f, 0.31f, -0.45f, 0.31f, color),
                cube(-0.32f, -0.48f, -0.50f, 0.32f, -0.40f, -0.25f, color)
        );
    }

    private static List<Cube> topHat() {
        return List.of(
                cube(-0.38f, -0.50f, -0.38f, 0.38f, -0.42f, 0.38f, 0xFF161616),
                cube(-0.25f, -0.88f, -0.25f, 0.25f, -0.49f, 0.25f, 0xFF202020),
                cube(-0.25f, -0.58f, -0.26f, 0.25f, -0.50f, 0.26f, 0xFF8E2735)
        );
    }

    private static List<Cube> mushroom(int color) {
        return List.of(
                cube(-0.43f, -0.66f, -0.43f, 0.43f, -0.46f, 0.43f, color),
                cube(-0.16f, -0.78f, -0.16f, 0.16f, -0.50f, 0.16f, 0xFFE8DEC5),
                cube(-0.29f, -0.68f, -0.44f, -0.15f, -0.55f, -0.42f, 0xFFF5F1DE),
                cube(0.12f, -0.68f, -0.44f, 0.28f, -0.55f, -0.42f, 0xFFF5F1DE)
        );
    }

    private static List<Cube> propeller() {
        List<Cube> cubes = new ArrayList<>(cap(0xFFE54B4B));
        cubes.add(cube(-0.05f, -0.78f, -0.05f, 0.05f, -0.60f, 0.05f, 0xFFB8B8B8));
        cubes.add(cube(-0.36f, -0.83f, -0.07f, 0.36f, -0.76f, 0.07f, 0xFF52A7E8));
        return List.copyOf(cubes);
    }

    private static List<Cube> beanie() {
        return List.of(
                cube(-0.32f, -0.65f, -0.32f, 0.32f, -0.43f, 0.32f, 0xFF7B4AB5),
                cube(-0.12f, -0.79f, -0.12f, 0.12f, -0.61f, 0.12f, 0xFFE8B34B)
        );
    }

    private static List<Cube> witchHat() {
        return List.of(
                cube(-0.44f, -0.51f, -0.44f, 0.44f, -0.43f, 0.44f, 0xFF2D1B45),
                cube(-0.27f, -0.76f, -0.27f, 0.27f, -0.50f, 0.27f, 0xFF372052),
                cube(-0.14f, -1.00f, -0.14f, 0.14f, -0.75f, 0.14f, 0xFF442861),
                cube(-0.27f, -0.59f, -0.28f, 0.27f, -0.52f, 0.28f, 0xFFB66CD8)
        );
    }

    private static List<Cube> paperBag() {
        return List.of(
                cube(-0.34f, -0.86f, -0.35f, 0.34f, -0.34f, 0.35f, 0xFFB68A55),
                cube(-0.20f, -0.67f, -0.36f, -0.08f, -0.56f, -0.34f, 0xFF2D241D),
                cube(0.08f, -0.67f, -0.36f, 0.20f, -0.56f, -0.34f, 0xFF2D241D)
        );
    }

    private static List<Cube> strawHat() {
        return List.of(
                cube(-0.48f, -0.53f, -0.48f, 0.48f, -0.43f, 0.48f, 0xFFD8B24E),
                cube(-0.29f, -0.70f, -0.29f, 0.29f, -0.52f, 0.29f, 0xFFE2C45E),
                cube(-0.29f, -0.58f, -0.30f, 0.29f, -0.52f, 0.30f, 0xFFB54D3B)
        );
    }

    private static List<Cube> crown() {
        return List.of(
                cube(-0.34f, -0.66f, -0.34f, 0.34f, -0.43f, 0.34f, 0xFFFFC72C),
                cube(-0.34f, -0.86f, -0.34f, -0.21f, -0.64f, -0.21f, 0xFFFFD83D),
                cube(-0.06f, -0.91f, -0.34f, 0.06f, -0.64f, -0.21f, 0xFFFFD83D),
                cube(0.21f, -0.86f, -0.34f, 0.34f, -0.64f, -0.21f, 0xFFFFD83D)
        );
    }

    private static List<Cube> chefCap() {
        return List.of(
                cube(-0.31f, -0.59f, -0.31f, 0.31f, -0.42f, 0.31f, 0xFFF4F4F2),
                cube(-0.42f, -0.82f, -0.28f, -0.05f, -0.56f, 0.28f, 0xFFFFFFFF),
                cube(-0.12f, -0.89f, -0.31f, 0.22f, -0.56f, 0.31f, 0xFFFFFFFF),
                cube(0.12f, -0.80f, -0.28f, 0.43f, -0.56f, 0.28f, 0xFFFFFFFF)
        );
    }

    private static List<Cube> tail(int color, boolean lightTip) {
        return List.of(
                cube(-0.10f, 0.02f, 0.22f, 0.10f, 0.35f, 0.42f, color),
                cube(-0.11f, 0.26f, 0.34f, 0.11f, 0.64f, 0.55f, color),
                cube(-0.12f, 0.54f, 0.43f, 0.12f, 0.87f, 0.65f,
                        lightTip ? 0xFFF4E7D1 : color)
        );
    }

    private static List<Cube> puff() {
        return List.of(
                cube(-0.19f, 0.10f, 0.28f, 0.19f, 0.48f, 0.66f, 0xFFF0EEE9),
                cube(-0.25f, 0.19f, 0.35f, 0.25f, 0.41f, 0.59f, 0xFFFFFFFF)
        );
    }

    private static List<Cube> raccoon() {
        return List.of(
                cube(-0.13f, 0.02f, 0.25f, 0.13f, 0.31f, 0.49f, 0xFF565A61),
                cube(-0.14f, 0.27f, 0.38f, 0.14f, 0.49f, 0.61f, 0xFF24272C),
                cube(-0.15f, 0.45f, 0.47f, 0.15f, 0.67f, 0.69f, 0xFF777B82),
                cube(-0.16f, 0.63f, 0.55f, 0.16f, 0.84f, 0.77f, 0xFF24272C)
        );
    }

    private static List<Cube> dragon() {
        return List.of(
                cube(-0.10f, 0.02f, 0.22f, 0.10f, 0.30f, 0.43f, 0xFF557A48),
                cube(-0.09f, 0.25f, 0.35f, 0.09f, 0.54f, 0.54f, 0xFF45683D),
                cube(-0.07f, 0.49f, 0.47f, 0.07f, 0.75f, 0.62f, 0xFF35552F),
                cube(-0.03f, 0.70f, 0.57f, 0.03f, 0.91f, 0.66f, 0xFF274525),
                cube(-0.03f, 0.20f, 0.21f, 0.03f, 0.29f, 0.28f, 0xFF9AC260)
        );
    }

    private static List<Cube> catgirlTail(int base, int shade, int tip) {
        return List.of(
                cube(-0.10f, 0.03f, 0.23f, 0.10f, 0.27f, 0.43f, base),
                cube(-0.12f, 0.22f, 0.34f, 0.11f, 0.48f, 0.56f, shade),
                cube(-0.09f, 0.43f, 0.47f, 0.15f, 0.69f, 0.68f, base),
                cube(0.02f, 0.64f, 0.56f, 0.25f, 0.88f, 0.77f, shade),
                cube(0.14f, 0.82f, 0.62f, 0.37f, 1.04f, 0.82f, tip),
                cube(-0.13f, 0.39f, 0.46f, 0.14f, 0.45f, 0.69f, tip),
                cube(0.00f, 0.76f, 0.55f, 0.27f, 0.82f, 0.78f, tip)
        );
    }
}
