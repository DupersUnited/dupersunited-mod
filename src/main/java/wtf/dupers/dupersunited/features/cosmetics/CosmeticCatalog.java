package wtf.dupers.dupersunited.features.cosmetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CosmeticCatalog {
    public enum Slot { HEAD, TAIL }
    public record Cube(float x1, float y1, float z1, float x2, float y2, float z2, int color) {}
    public record Item(String id, String name, Slot slot, String creator, String source, String model, List<Cube> cubes) {}

    private static final String VOXEL = "https://sketchfab.com/VoxelBear";
    private static final String BUNNY = "https://sketchfab.com/3d-models/bunny-ears-c137a6601fc24e548a17d4fe689fe28d";
    private static final String DUCK = "https://sketchfab.com/3d-models/minecraft-style-duck-blockbench-2c7fd8edfbb5490489fbdcc5855035bf";
    private static final List<Item> ITEMS = build();

    private CosmeticCatalog() {}

    public static List<Item> all() { return ITEMS; }
    public static List<Item> forSlot(Slot slot) { return ITEMS.stream().filter(i -> i.slot() == slot).toList(); }
    public static Item get(String id) { return ITEMS.stream().filter(i -> i.id().equals(id)).findFirst().orElse(ITEMS.getFirst()); }
    public static Item named(Slot slot, String name) { return ITEMS.stream().filter(i -> i.slot() == slot && i.name().equals(name)).findFirst().orElse(forSlot(slot).getFirst()); }
    public static List<Item> search(Slot slot, String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return forSlot(slot).stream().filter(i -> i.name().toLowerCase(Locale.ROOT).contains(q) || i.creator().toLowerCase(Locale.ROOT).contains(q)).toList();
    }

    private static List<Item> build() {
        List<Item> out = new ArrayList<>();
        out.add(item("none-head", "None", Slot.HEAD, "DupersUnited", "", List.of()));
        out.add(item("bunny-ears", "Bunny Ears", Slot.HEAD, "Silvia_Rose", BUNNY, ears(0xFFF1D4D4, 0xFFFF94B7)));
        out.add(new Item("minecraft-duck", "Minecraft Duck", Slot.HEAD, "Kadzuo", DUCK, "minecraft_style_duck_blockbench.glb", List.of()));
        out.add(item("cap", "Cap", Slot.HEAD, "VoxelBear", VOXEL, cap(0xFF3B82F6)));
        out.add(item("top-hat", "Top Hat", Slot.HEAD, "VoxelBear", VOXEL, topHat()));
        out.add(item("red-mushroom", "Red Mushroom", Slot.HEAD, "VoxelBear", VOXEL, mushroom(0xFFD83A3A)));
        out.add(item("propeller", "Propeller Hat", Slot.HEAD, "VoxelBear", VOXEL, propeller()));
        out.add(item("beanie", "Beanie", Slot.HEAD, "VoxelBear", VOXEL, beanie()));
        out.add(item("blue-mushroom", "Blue Mushroom", Slot.HEAD, "VoxelBear", VOXEL, mushroom(0xFF4287D6)));
        out.add(item("witch-hat", "Witch Hat", Slot.HEAD, "VoxelBear", VOXEL, witchHat()));
        out.add(item("green-mushroom", "Green Mushroom", Slot.HEAD, "VoxelBear", VOXEL, mushroom(0xFF46A758)));
        out.add(item("paper-bag", "Paper Bag", Slot.HEAD, "VoxelBear", VOXEL, paperBag()));
        out.add(item("straw-hat", "Straw Hat", Slot.HEAD, "VoxelBear", VOXEL, strawHat()));
        out.add(item("crown", "Crown", Slot.HEAD, "VoxelBear", VOXEL, crown()));
        out.add(item("chef-cap", "Chef Cap", Slot.HEAD, "VoxelBear", VOXEL, chefCap()));
        out.add(catEarItem("slim-white-cat-ears","Slim White Cat Ears",0xFFD8D8D5,0xFFF4F4F2,0xFFFFAFC8));
        out.add(catEarItem("slim-pink-cat-ears","Slim Pink Cat Ears",0xFFC72C68,0xFFFF72AD,0xFFFFC4D9));
        out.add(catEarItem("slim-brown-cat-ears","Slim Brown Cat Ears",0xFF4A281B,0xFF7B4930,0xFFE8B995));
        out.add(item("none-tail", "None", Slot.TAIL, "DupersUnited", "", List.of()));
        out.add(item("cat-tail", "Cat Tail", Slot.TAIL, "DupersUnited", "", tail(0xFF30343B, false)));
        out.add(item("fox-tail", "Fox Tail", Slot.TAIL, "DupersUnited", "", tail(0xFFD76B2D, true)));
        out.add(item("bunny-puff", "Bunny Puff", Slot.TAIL, "DupersUnited", "", puff()));
        out.add(item("raccoon-tail", "Raccoon Tail", Slot.TAIL, "DupersUnited", "", raccoon()));
        out.add(item("dragon-tail", "Dragon Tail", Slot.TAIL, "DupersUnited", "", dragon()));
        out.add(item("snow-catgirl-tail", "Snow Catgirl Tail", Slot.TAIL, "DupersUnited", "", catgirlTail(0xFFF2F2F0,0xFFB7DFFF,0xFFFFAFC8)));
        out.add(item("midnight-catgirl-tail", "Midnight Catgirl Tail", Slot.TAIL, "DupersUnited", "", catgirlTail(0xFF17181D,0xFF4B3A5C,0xFFCF4058)));
        out.add(item("orange-catgirl-tail", "Orange Catgirl Tail", Slot.TAIL, "DupersUnited", "", catgirlTail(0xFFE8792F,0xFF8C3F20,0xFFFFE0B4)));
        return List.copyOf(out);
    }

    private static Item item(String id, String name, Slot slot, String creator, String source, List<Cube> cubes) { return new Item(id, name, slot, creator, source, "", cubes); }
    private static Item catEarItem(String id,String name,int outer,int shade,int inner){return new Item(id,name,Slot.HEAD,"DupersUnited","","",blockyCatEars(outer,shade,inner));}
    private static List<Cube> blockyCatEars(int outer,int shade,int inner){
        float front=-.03125f,back=.03125f,detail=front-.003f;
        return List.of(
            c(-.375f,-.5625f,front,-.0625f,-.5f,back,outer),c(.0625f,-.5625f,front,.375f,-.5f,back,outer),
            c(-.375f,-.625f,front,-.0625f,-.5625f,back,outer),c(.0625f,-.625f,front,.375f,-.5625f,back,outer),
            c(-.375f,-.6875f,front,-.125f,-.625f,back,outer),c(.125f,-.6875f,front,.375f,-.625f,back,outer),
            c(-.3125f,-.75f,front,-.125f,-.6875f,back,outer),c(.125f,-.75f,front,.3125f,-.6875f,back,outer),
            c(-.3125f,-.8125f,front,-.1875f,-.75f,back,outer),c(.1875f,-.8125f,front,.3125f,-.75f,back,outer),
            c(-.3125f,-.875f,front,-.1875f,-.8125f,back,outer),c(.1875f,-.875f,front,.3125f,-.8125f,back,outer),
            c(-.3125f,-.6875f,detail,-.125f,-.5625f,front,shade),c(.125f,-.6875f,detail,.3125f,-.5625f,front,shade),
            c(-.25f,-.75f,detail,-.125f,-.6875f,front,shade),c(.125f,-.75f,detail,.25f,-.6875f,front,shade),
            c(-.25f,-.6875f,detail-.003f,-.125f,-.5625f,detail,inner),c(.125f,-.6875f,detail-.003f,.25f,-.5625f,detail,inner),
            c(-.25f,-.75f,detail-.003f,-.1875f,-.6875f,detail,inner),c(.1875f,-.75f,detail-.003f,.25f,-.6875f,detail,inner));
    }
    private static Cube c(float x1,float y1,float z1,float x2,float y2,float z2,int color){ return new Cube(x1,y1,z1,x2,y2,z2,color); }
    private static List<Cube> ears(int outer,int inner){ return List.of(c(-.27f,-.78f,-.10f,-.08f,-.45f,.10f,outer),c(.08f,-.78f,-.10f,.27f,-.45f,.10f,outer),c(-.22f,-.72f,-.11f,-.13f,-.51f,-.09f,inner),c(.13f,-.72f,-.11f,.22f,-.51f,-.09f,inner)); }
    private static List<Cube> cap(int color){ return List.of(c(-.31f,-.61f,-.31f,.31f,-.45f,.31f,color),c(-.32f,-.48f,-.50f,.32f,-.40f,-.25f,color)); }
    private static List<Cube> topHat(){ return List.of(c(-.38f,-.50f,-.38f,.38f,-.42f,.38f,0xFF161616),c(-.25f,-.88f,-.25f,.25f,-.49f,.25f,0xFF202020),c(-.25f,-.58f,-.26f,.25f,-.50f,.26f,0xFF8E2735)); }
    private static List<Cube> mushroom(int color){ return List.of(c(-.43f,-.66f,-.43f,.43f,-.46f,.43f,color),c(-.16f,-.78f,-.16f,.16f,-.50f,.16f,0xFFE8DEC5),c(-.29f,-.68f,-.44f,-.15f,-.55f,-.42f,0xFFF5F1DE),c(.12f,-.68f,-.44f,.28f,-.55f,-.42f,0xFFF5F1DE)); }
    private static List<Cube> propeller(){ List<Cube> x=new ArrayList<>(cap(0xFFE54B4B)); x.add(c(-.05f,-.78f,-.05f,.05f,-.60f,.05f,0xFFB8B8B8)); x.add(c(-.36f,-.83f,-.07f,.36f,-.76f,.07f,0xFF52A7E8)); return x; }
    private static List<Cube> beanie(){ return List.of(c(-.32f,-.65f,-.32f,.32f,-.43f,.32f,0xFF7B4AB5),c(-.12f,-.79f,-.12f,.12f,-.61f,.12f,0xFFE8B34B)); }
    private static List<Cube> witchHat(){ return List.of(c(-.44f,-.51f,-.44f,.44f,-.43f,.44f,0xFF2D1B45),c(-.27f,-.76f,-.27f,.27f,-.50f,.27f,0xFF372052),c(-.14f,-1.00f,-.14f,.14f,-.75f,.14f,0xFF442861),c(-.27f,-.59f,-.28f,.27f,-.52f,.28f,0xFFB66CD8)); }
    private static List<Cube> paperBag(){ return List.of(c(-.34f,-.86f,-.35f,.34f,-.34f,.35f,0xFFB68A55),c(-.20f,-.67f,-.36f,-.08f,-.56f,-.34f,0xFF2D241D),c(.08f,-.67f,-.36f,.20f,-.56f,-.34f,0xFF2D241D)); }
    private static List<Cube> strawHat(){ return List.of(c(-.48f,-.53f,-.48f,.48f,-.43f,.48f,0xFFD8B24E),c(-.29f,-.70f,-.29f,.29f,-.52f,.29f,0xFFE2C45E),c(-.29f,-.58f,-.30f,.29f,-.52f,.30f,0xFFB54D3B)); }
    private static List<Cube> crown(){ return List.of(c(-.34f,-.66f,-.34f,.34f,-.43f,.34f,0xFFFFC72C),c(-.34f,-.86f,-.34f,-.21f,-.64f,-.21f,0xFFFFD83D),c(-.06f,-.91f,-.34f,.06f,-.64f,-.21f,0xFFFFD83D),c(.21f,-.86f,-.34f,.34f,-.64f,-.21f,0xFFFFD83D)); }
    private static List<Cube> chefCap(){ return List.of(c(-.31f,-.59f,-.31f,.31f,-.42f,.31f,0xFFF4F4F2),c(-.42f,-.82f,-.28f,-.05f,-.56f,.28f,0xFFFFFFFF),c(-.12f,-.89f,-.31f,.22f,-.56f,.31f,0xFFFFFFFF),c(.12f,-.80f,-.28f,.43f,-.56f,.28f,0xFFFFFFFF)); }
    private static List<Cube> tail(int color,boolean tip){ List<Cube>x=new ArrayList<>(List.of(c(-.10f,.02f,.22f,.10f,.35f,.42f,color),c(-.11f,.26f,.34f,.11f,.64f,.55f,color),c(-.12f,.54f,.43f,.12f,.87f,.65f,tip?0xFFF4E7D1:color))); return x; }
    private static List<Cube> puff(){ return List.of(c(-.19f,.10f,.28f,.19f,.48f,.66f,0xFFF0EEE9),c(-.25f,.19f,.35f,.25f,.41f,.59f,0xFFFFFFFF)); }
    private static List<Cube> raccoon(){ return List.of(c(-.13f,.02f,.25f,.13f,.31f,.49f,0xFF565A61),c(-.14f,.27f,.38f,.14f,.49f,.61f,0xFF24272C),c(-.15f,.45f,.47f,.15f,.67f,.69f,0xFF777B82),c(-.16f,.63f,.55f,.16f,.84f,.77f,0xFF24272C)); }
    private static List<Cube> dragon(){ return List.of(c(-.10f,.02f,.22f,.10f,.30f,.43f,0xFF557A48),c(-.09f,.25f,.35f,.09f,.54f,.54f,0xFF45683D),c(-.07f,.49f,.47f,.07f,.75f,.62f,0xFF35552F),c(-.03f,.70f,.57f,.03f,.91f,.66f,0xFF274525),c(-.03f,.20f,.21f,.03f,.29f,.28f,0xFF9AC260)); }
    private static List<Cube> catgirlTail(int base,int shade,int tip){return List.of(c(-.10f,.03f,.23f,.10f,.27f,.43f,base),c(-.12f,.22f,.34f,.11f,.48f,.56f,shade),c(-.09f,.43f,.47f,.15f,.69f,.68f,base),c(.02f,.64f,.56f,.25f,.88f,.77f,shade),c(.14f,.82f,.62f,.37f,1.04f,.82f,tip),c(-.13f,.39f,.46f,.14f,.45f,.69f,tip),c(.00f,.76f,.55f,.27f,.82f,.78f,tip));}
}
