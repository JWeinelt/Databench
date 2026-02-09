package de.julianweinelt.databench.flow.flow;

import de.julianweinelt.databench.flow.storage.LocalStorage;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class FlowSocketServer extends WebSocketServer {
    private final List<Client> clients = new ArrayList<>();

    public FlowSocketServer() {
        super(new InetSocketAddress(
                LocalStorage.instance().getConfig().getInternalServerAddress(),
                LocalStorage.instance().getConfig().getInternalSocketPort()));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    @Override
    public void onMessage(WebSocket conn, String message) {

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {

    }
}
