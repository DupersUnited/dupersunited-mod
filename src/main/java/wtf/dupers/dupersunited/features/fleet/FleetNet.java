package wtf.dupers.dupersunited.features.fleet;

import org.java_websocket.WebSocket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.features.ModSettings;
import wtf.dupers.dupersunited.features.fleet.net.FleetClient;
import wtf.dupers.dupersunited.features.fleet.net.FleetPackets;
import wtf.dupers.dupersunited.features.fleet.net.FleetServer;
import wtf.dupers.dupersunited.modules.ModuleManager;
import wtf.dupers.dupersunited.modules.glitcha.FleetModule;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class FleetNet {
    public enum State {
        IDLE,
        HOSTING,
        CONNECTED
    }

    private static State state = State.IDLE;
    private static FleetServer server;
    private static FleetClient client;

    private static final Map<WebSocket, FleetPackets.Worker> connectionMap = new ConcurrentHashMap<>();
    private static final List<FleetPackets.Worker> roster = Collections.synchronizedList(new ArrayList<>());

    public static State state() {
        return state;
    }

    public static boolean host(int port) {
        if (state == State.HOSTING) {
            Module.moduleLog("Fleet", "you're already hosting.");
            return false;
        }
        if (state == State.CONNECTED) {
            Module.moduleLog("Fleet", "disconnect from your host before hosting.");
            return false;
        }
        try {
            server = new FleetServer(port);
            server.start();
            connectionMap.clear();
            roster.clear();
            state = State.HOSTING;
            return true;
        } catch (Exception e) {
            Module.moduleLog("Fleet", "failed to host: " + e.getMessage());
            return false;
        }
    }

    public static void onHostStarted(int port) {
        if (state != State.HOSTING) return;
        Module.moduleLog("Fleet", "hosting on port " + port + ".");
    }

    public static void onHostFailed(String reason) {
        if (state != State.HOSTING) return;
        Module.moduleLog("Fleet", "failed to host: " + reason);
        stop();
    }

    public static boolean connect(int port) {
        if (state == State.HOSTING) {
            Module.moduleLog("Fleet", "stop hosting before joining another host.");
            return false;
        }

        if (state == State.CONNECTED) {
            Module.moduleLog("Fleet", "you're already connected to a host.");
            return false;
        }

        client = new FleetClient(port);
        client.connect();
        return true;
    }

    public static void stop() {
        state = State.IDLE;

        try {
            if (server != null) server.stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (client != null) client.close();
        server = null;
        client = null;
        connectionMap.clear();
        roster.clear();

        mc.execute(() -> {
            FleetModule module = MainClient.getModule(FleetModule.class);
            if (module != null && module.isEnabled()) module.setEnabled(false);
        });
    }

    public static void say(String message) {
        if (state == State.HOSTING) {
            FleetModule module = MainClient.getModule(FleetModule.class);
            long delay = module == null ? 0 : module.getDelay();
            if (server != null) server.sendAll(FleetPackets.encodeH2W(new FleetPackets.SayH2W(message, delay)));
            FleetScheduler.after(() -> mc.execute(() -> sendChatNow(message)), delay);
        } else if (state == State.CONNECTED && client != null) {
            client.send(FleetPackets.encodeW2H(new FleetPackets.SayAllW2H(message)));
        } else {
            Module.moduleLog("Fleet", "not hosting or connected.");
        }
    }

    public static List<FleetPackets.Worker> workers() {
        synchronized (roster) {
            return List.copyOf(roster);
        }
    }

    public static String workerNames() {
        StringBuilder names = new StringBuilder();
        for (FleetPackets.Worker w : workers()) {
            if (!names.isEmpty()) names.append(", ");
            names.append(w.name());
        }
        return names.toString();
    }

    public static void onConnected() {
        state = State.CONNECTED;
        roster.clear();
        Module.moduleLog("Fleet", "connected to host.");
        client.send(FleetPackets.encodeW2H(new FleetPackets.HelloW2H(
            mc.getUser().getName(), mc.getUser().getProfileId())));
    }

    public static void onDisconnected(boolean wasConnected) {
        if (wasConnected) Module.moduleLog("Fleet", "lost the host.");
        stop();
    }

    public static void onWorkerMessage(WebSocket conn, ByteBuffer bytes) {
        if (state != State.HOSTING) return;
        FleetPackets.W2HPacket packet;
        try {
            packet = FleetPackets.decodeW2H(bytes);
        } catch (Exception e) {
            return;
        }
        if (packet instanceof FleetPackets.HelloW2H(String name, java.util.UUID id)) {
            FleetPackets.Worker worker = new FleetPackets.Worker(name, id);
            connectionMap.put(conn, worker);
            roster.add(worker);
            Module.moduleLog("Fleet", statusMessage(worker, "joined", roster.size()));
            if (server != null) {
                server.sendAll(FleetPackets.encodeH2W(new FleetPackets.WorkersH2W(workers())));
            }
        } else if (packet instanceof FleetPackets.SayAllW2H(String message)) {
            FleetModule module = MainClient.getModule(FleetModule.class);
            long delay = module == null ? 0 : module.getDelay();
            if (server != null) server.sendAll(FleetPackets.encodeH2W(new FleetPackets.SayH2W(message, delay)));
            FleetScheduler.after(() -> mc.execute(() -> sendChatNow(message)), delay);
        }
    }

    public static void onWorkerLeft(WebSocket conn) {
        FleetPackets.Worker gone = connectionMap.remove(conn);
        if (gone != null) roster.remove(gone);
        if (gone != null && state == State.HOSTING) {
            Module.moduleLog("Fleet", statusMessage(gone, "left", roster.size()));
            if (server != null) server.sendAll(FleetPackets.encodeH2W(new FleetPackets.WorkersH2W(workers())));
        }
    }

    public static void onHostMessage(ByteBuffer bytes) {
        FleetPackets.H2WPacket packet;

        try {
            packet = FleetPackets.decodeH2W(bytes);
        } catch (Exception e) {
            return;
        }

        if (packet instanceof FleetPackets.SayH2W(String message, long delayMs)) {
            if (ModSettings.logFleetActions.getValue()) Module.moduleLog("Fleet", "host made you say: " + message);
            FleetScheduler.after(() -> mc.execute(() -> sendChatNow(message)), delayMs);
        } else if (packet instanceof FleetPackets.WorkersH2W(List<FleetPackets.Worker> workerList)) {
            roster.clear();
            roster.addAll(workerList);
            Module.moduleLog("Fleet", "workers (" + roster.size() + "): " + workerNames());
        }
    }

    private static void sendChatNow(String message) {
        if (mc.player == null) return;
        if (message.startsWith("/")) mc.player.connection.sendCommand(message.substring(1));
        else mc.player.connection.sendChat(message);
    }

    private static Component statusMessage(FleetPackets.Worker worker, String status, int count) {
        return Component.empty()
            .append(Component.literal(worker.name()).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(worker.id().toString()).withStyle(ChatFormatting.GRAY)))))
            .append(Component.literal(" " + status + " (" + count + " workers).").withStyle(ChatFormatting.WHITE));
    }
}