package wtf.dupers.dupersunited.features.fleet.net;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.features.fleet.FleetNet;
import wtf.dupers.dupersunited.modules.ModuleManager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class FleetServer extends WebSocketServer {
    private final int port;
    private boolean started;

    public FleetServer(int port) {
        super(new InetSocketAddress(port));
        this.port = port;
        setReuseAddr(true);
    }

    @Override
    public void onStart() {
        started = true;
        FleetNet.onHostStarted(port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        FleetNet.onWorkerLeft(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer bytes) {
        if (bytes.hasRemaining()) FleetNet.onWorkerMessage(conn, bytes);
    }

    @Override
    public void onError(WebSocket conn, Exception e) {
        if (!started) FleetNet.onHostFailed(e.getMessage());
        else Module.moduleLog("Fleet", "host error: " + e.getMessage());
    }

    public void sendTo(WebSocket conn, ByteBuffer payload) {
        conn.send(payload.duplicate());
    }

    public void sendAll(ByteBuffer payload) {
        broadcast(payload.duplicate());
    }
}