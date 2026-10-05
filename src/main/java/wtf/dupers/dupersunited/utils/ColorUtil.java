package wtf.dupers.dupersunited.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    public static final int DEEP_SAPPHIRE = 0xFF1E1E2E;
    public static final int PALE_NAVY = 0xFFCDD6F4;
    public static final int SUBTEXT  = 0xFFA6ADC8;
    public static final int MAUVE = 0xFFCBA6F7;
    public static final int LAVENDER = 0xFFB4BEFE;
    public static final int BLUE = 0xFF89B4fA;
    public static final int SAPPHIRE = 0xFF74C7EC;
    public static final int TEAL = 0xFF94E2D5;
    public static final int GREEN = 0xFFA6E3A1;
    public static final int YELLOW = 0xFFf9E2AF;
    public static final int PEACH = 0xFFFAB387;
    public static final int RED = 0xFFF38BA8;
    public static final int CRUST = 0xFF11111B;
    public static final int MANTLE = 0xFF181825;
    public static final int DEEP_INDIGO = 0xFF313244;
    public static final int FADED_INDIGO = 0xFF45475A;
    public static final int FADED_NAVY = 0xFF6C7086;
    public static final int BUTTON_GREEN = 0xFF40A02B;
    public static final int BUTTON_RED = 0xFFD20F39;
    public static final int REBIND_ACTIVE = 0xFFE64553;
    public static final int FIELD_FOCUSED = 0xFF1A2330;
    public static final int BG = 0xED1E1E2E;
    public static final int HDR_BTN = 0xFF7F849C;
    public static final int OFF = 0xFF585B70;
    public static final int HOVER = 0x12CDD6F4;
    public static final int SET_BG = 0xCC11111B;
    public static final int SET_LINE = 0xFF262637;
    public static final int SELECTION = 0x6689B4FA;
    public static final int MAUVE2 = 0xcba6f7;
    public static final int RED2 = 0xf38ba8;

    public static Color toAwtColor(int argb) {
        return new Color(argb, true);
    }

    public static String applyFormattingCodes(String base, char formattingChar) {
        return base.replaceAll(formattingChar+"([0-9a-fk-or])", "§$1");
    }

    public static String applyFormattingCodes(String base) {
        return applyFormattingCodes(base, '&');
    }

    public static Component generateColoredText(String raw) {
        String first = raw.replaceAll("&#[0-9a-fA-F]{6}.*","");
        Pattern rgbPattern = Pattern.compile("&#([0-9a-fA-F]{6})(.*?)(?=&#[0-9a-fA-F]{6}|$)");
        Matcher matcher = rgbPattern.matcher(raw);
        MutableComponent coloredText = Component.literal(applyFormattingCodes(first)).withColor(MAUVE);
        while (matcher.find()) {
            String rgb = matcher.group(1);
            String text = matcher.group(2);
            coloredText.append(Component.literal(applyFormattingCodes(text)).withColor(0xFF000000+Integer.parseInt(rgb, 16)));
        }
        return coloredText;
    }
}