package wtf.dupers.dupersunited.features.screens.ui;

import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.account.AccountsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class PrideTheme {
    public static boolean PRIDE = Boolean.getBoolean("u.pride") || shouldEnable();
    public static final int C_PRIDE_1 = 0xFF5bcefa;
    public static final int C_PRIDE_2 = 0xFFf5a9b8;
    public static final int C_PRIDE_3 = 0xFFFFFFFF;

    private static boolean shouldEnable() {
        Path persist = SharedVariables.DIRECTORY.resolve(".persist-theme");
        if (Files.exists(persist)) {
            return true;
        }

        AccountsScreen.loadAccounts(() -> {
            try {
                Set<String> asdf = Set.of(
                    "+itTEK4isybaTmqNd+05s4UBKl/BVH1VoD1fNWScOtw=",
                    "y7Bdc1nT9MOiMBtGm+rWRp20gmTUktYSIZoOuaTenl4=",
                    "zpMi8VUhAfQUHNHEt9Kk+s5PoiPE0jxb5InbAL+XWlY=",
                    "JzpscyzI5q1Y/JCXRNc0nviV6I6XwEIYX7Rwx8oIvb4=",
                    "Rxzn/WjD7RDYKdL5k/32339+KzbHb7y95cZ4NTO1JXY=",
                    "RU+28lct+BAldFFsRmWvpslurh7eHYeG/AJ7hm6KHQI=",
                    "dCvD1Y8XZHyDjqqIkijCRunjQdVTRLOr0fUIUUxNeyA=",
                    "bFR8ZdvcfBf0EI9SyesPIOrA4oI9rDASQG3P5TuJXIA=",
                    "Nkm9MP4rQt9FFGlCQmsRAuGvx2K5jQ7j6DKiWZL+bpw=",
                    "HaEGIW7fxWiSXLFjHqzSa3nOr9jOVA0KrFUOeXGVSws=",
                    "/G8Guojz4FH+s1Y15T4s4dqVMtuzVBRQRMdqDjEA858=",
                    "A2AH7h75vqJskR6ZgdXHp3+a0JUVPS5hHA+IDYxnxWk=",
                    "+XLQPQnDeLEsmSFIdDQOy0hMp6xLc3fbohnNwgZsX/s=",
                    "HVfwhqJFcfJBhkIkRSRMOhl4XOR0Vz54fXzlIy4bWpA=",
                    "sl5762NDxgf05Pd4ZMWVabZO8be/sIHxJCvWBUmfH1c=",
                    "jsiARLbjLxYaQwAlytMxnO24NQAaot8YIlkQLGtE4R4=",
                    "SbN9Pq6HXwYD0Vzzohqq1XO+Q3hI/saxI+zCgMAN+6k=",
                    "LzeO1X6xO3JpaYQXpKYmKJ8wUu29+Rrv8aqPoMslOCg="
                );
                byte[] salt = "faggot !!".getBytes(StandardCharsets.ISO_8859_1);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

                for (AccountsScreen.AccountEntry entry : AccountsScreen.ACCOUNTS) {
                    PBEKeySpec spec = new PBEKeySpec(entry.name().toCharArray(), salt, 100_000, 256);
                    byte[] cHash = factory.generateSecret(spec).getEncoded();
                    spec.clearPassword();

                    if (asdf.contains(Base64.getEncoder().encodeToString(cHash))) {
                        PRIDE = true;
                        Files.createFile(persist);
                        return;
                    }
                }
            } catch (Throwable e) {
                // ignored
            }
        });

        return false;
    }

    public static Component transStyle(String string) {
        int width = mc.font.width(string);
        int target = width / 5;

        Style s1 = Style.EMPTY.withColor(C_PRIDE_1);
        Style s2 = Style.EMPTY.withColor(C_PRIDE_2);
        Style s3 = Style.EMPTY.withColor(C_PRIDE_3);

        MutableComponent result = Component.empty();
        int c = 0;
        int p = 0;
        int currentWidth = 0;
        for (int i = 0; i < string.length() && p < 4; i++) {
            currentWidth += mc.font.width(Character.toString(string.codePointAt(i)));
            if (currentWidth >= target * (p + 1)) {
                result.append(
                    Component.literal(string.substring(c, i + 1))
                        .setStyle(switch (p++) {
                            case 0 -> s1;
                            case 1, 3 -> s2;
                            default -> s3;
                        })
                );
                c = i + 1;
            }
        }

        if (c < string.length()) {
            result.append(Component.literal(string.substring(c)).setStyle(s1));
        }

        return result;
    }

    public static Component prideStyle(String string) {
        int width = mc.font.width(string);
        float hueAccumulation = 1.0f / width;

        MutableComponent result = Component.empty();
        float hue = 0f;
        for (int i = 0; i < string.length(); i++) {
            String s = Character.toString(string.codePointAt(i));
            int w = mc.font.width(s);

            result.append(Component.literal(s).setStyle(Style.EMPTY
                .withColor(0xFF000000 | Color.HSBtoRGB(hue, 0.5f, 1f))));
            hue += hueAccumulation * w;
        }
        return result;
    }
}
