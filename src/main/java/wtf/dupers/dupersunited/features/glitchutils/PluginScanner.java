package wtf.dupers.dupersunited.features.glitchutils;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.account.SessionManager;
import wtf.dupers.dupersunited.utils.IClientCommandSource;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

import java.util.*;

import static wtf.dupers.dupersunited.MainClient.mc;

public class PluginScanner {
    private static boolean scanning = false;
    private static int completionId = -1;
    private static int ticksWaiting = 0;

    private static final Set<String> treePlugins = new ObjectRBTreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private static final SortedSet<String> foundPlugins = new ObjectRBTreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private static final Set<String> VERSION_ALIASES = Set.of(
        "version", "ver", "about", "bukkit:version", "bukkit:ver", "bukkit:about"
    );
    private static String versionAlias = null;

    private static final Set<String> ANTICHEAT_LIST = Set.of(
        "nocheatplus", "negativity", "warden", "horizon", "vulcan",
        "spartan", "grimac", "matrix", "kauri", "themis", "intave",
        "anticheat", "witherac", "godseye", "coreprotect", "wraith",
        "antixrayheuristics", "anticheatreloaded", "exploitsx",
        "foxaddition", "guardianac", "ggintegrity", "lightanticheat",
        "anarchyexploitfixes", "abc", "illegalstack", "polar"
    );

    public static void onCommandTree(RootCommandNode<ClientSuggestionProvider> rootNode) {
        treePlugins.clear();
        versionAlias = null;

        rootNode.getChildren().stream().filter(node -> node instanceof LiteralCommandNode<?>).forEach(node -> {
            String name = node.getName();

            String[] split = name.split(":");
            if (split.length > 1) {
                treePlugins.add(split[0]);
            }

            if (versionAlias == null && VERSION_ALIASES.contains(name) && node.getChildren().stream().anyMatch(childNode -> childNode instanceof ArgumentCommandNode<?,?>)) {
                versionAlias = name;
            }
        });
    }

    public static void startScan() {
        if (mc.getConnection() == null || mc.player == null) return;

        if (scanning) {
            MainCommand.sendMessage("Already scanning plugins, silly!", true);
            return;
        }

        foundPlugins.clear();
        scanning = true;
        completionId = ((IClientCommandSource) mc.getConnection().getSuggestionsProvider()).dupersunited$beginCompletion();
        ticksWaiting = 0;

        MainCommand.sendMessage("Starting plugin scan...", true);

        String cmd = versionAlias != null ? versionAlias : "ver";
        mc.getConnection().send(new ServerboundCommandSuggestionPacket(completionId, cmd + " "));
    }

    public static void onTick() {
        if (!scanning) return;

        if (++ticksWaiting >= 100) {
            printResults();
        }
    }

    public static void onCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (scanning && packet.id() == completionId) {
            assert mc.getConnection() != null; // this function called from command handler

            ((IClientCommandSource) mc.getConnection().getSuggestionsProvider()).dupersunited$endCompletion();

            var suggestions = packet.toSuggestions().getList();

            if (!suggestions.isEmpty()) {
                for (var s : suggestions) {
                    String name = s.getText().trim();
                    if (!name.isEmpty() && !name.equals(SessionManager.getUsername())) {
                        foundPlugins.add(name);
                    }
                }
            }

            printResults();
        }
    }

    private static void printResults() {
        scanning = false;
        completionId = -1;
        ticksWaiting = 0;
        if (mc.player == null) return;

        foundPlugins.addAll(treePlugins);

        if (foundPlugins.isEmpty()) {
            MainCommand.sendMessage(
                Component.literal("Could not find any plugins.").withStyle(ChatFormatting.RED),
                true
            );
            return;
        }

        MutableComponent text = Component.literal("Found ")
            .append(Component.literal(Integer.toString(foundPlugins.size())).withStyle(ChatFormatting.AQUA))
            .append(" plugins: ")
            .append(ComponentUtils.formatList(foundPlugins, Optional.of(ComponentUtils.DEFAULT_SEPARATOR), plugin ->
                Component.literal(plugin).withStyle(isAnticheat(plugin) ? ChatFormatting.BLUE : ChatFormatting.GREEN)));

        MainCommand.sendMessage(text, true);
    }

    private static boolean isAnticheat(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return ANTICHEAT_LIST.contains(n) || n.contains("exploit") || n.contains("anti") || n.contains("shield");
    }
}