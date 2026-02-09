package de.julianweinelt.databench.flow.flow;

import lombok.Getter;
import lombok.Setter;
import org.java_websocket.WebSocket;

import java.util.UUID;

@Getter
@Setter
public class Client {
    private final UUID uniqueId;
    private final WebSocket webSocket;
    private UUID installationId;

    public Client(UUID uniqueId, WebSocket webSocket) {
        this.uniqueId = uniqueId;
        this.webSocket = webSocket;
    }
}
