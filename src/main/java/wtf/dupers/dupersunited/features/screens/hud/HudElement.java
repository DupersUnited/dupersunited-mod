package wtf.dupers.dupersunited.features.screens.hud;

public class HudElement {
    public final String id;
    public int x, y;
    public float scale = 1.0f;
    public boolean rightAligned = false;

    private static final int BASE_W = 120;
    private static final int BASE_H = 10;

    public HudElement(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int getW() {
        return (int)(BASE_W * scale);
    }

    public int getH() {
        return (int)(BASE_H * scale);
    }

    public int getScreenX(int screenWidth) {
        return rightAligned ? screenWidth - x - getW() : x;
    }

    public boolean isHovered(double mx, double my, int screenWidth) {
        int sx = getScreenX(screenWidth);
        return mx >= sx && mx <= sx + getW() && my >= y && my <= y + getH();
    }

    public boolean isHovered(double mx, double my) {
        return isHovered(mx, my, Integer.MAX_VALUE);
    }
}