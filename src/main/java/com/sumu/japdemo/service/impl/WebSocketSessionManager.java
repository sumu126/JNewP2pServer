package com.sumu.japdemo.service.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Component
public class WebSocketSessionManager {

    private static SocketIOServer staticServer;

    @Autowired
    private SocketIOServer server;

    @PostConstruct
    public void init() {
        staticServer = this.server;
    }

    public static boolean isSessionActive(String nodeId) {
        if (staticServer == null) {
            return false;
        }
        try {
            UUID sessionId = UUID.fromString(nodeId);
            SocketIOClient client = staticServer.getClient(sessionId);
            return client != null && client.isChannelOpen();
        } catch (Exception e) {
            return false;
        }
    }
}
