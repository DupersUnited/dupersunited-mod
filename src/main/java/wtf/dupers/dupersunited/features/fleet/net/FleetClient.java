package wtf.dupers.dupersunited.features.fleet.net;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import wtf.dupers.dupersunited.features.fleet.FleetNet;
import wtf.dupers.dupersunited.modules.ModuleManager;

import java.net.URI;
import java.nio.ByteBuffer;

public class FleetClient extends WebSocketClient {
    private boolean opened;

    public FleetClient(int port) {
        super(URI.create("ws://127.0.0.1:" + port));
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        opened = true;
        FleetNet.onConnected();
    }

    @Override
    public void onMessage(String message) {
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        if (bytes.hasRemaining()) FleetNet.onHostMessage(bytes);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        FleetNet.onDisconnected(opened);
        opened = false;
    }

    @Override
    public void onError(Exception e) {
        if (!opened) ModuleManager.moduleLog("Fleet", "couldn't reach a host on that port, is it hosting?");
        else ModuleManager.moduleLog("Fleet", "connection error: " + e.getMessage());
    }
}