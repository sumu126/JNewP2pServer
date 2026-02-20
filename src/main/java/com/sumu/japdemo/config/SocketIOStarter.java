package com.sumu.japdemo.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.sumu.japdemo.socketio.SignalingSocketIOModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SocketIOStarter implements CommandLineRunner {

    @Autowired
    private SocketIOServer server;

    @Autowired
    private SignalingSocketIOModule signalingSocketIOModule;

    @Autowired
    private Environment env;

    @Override
    public void run(String... args) throws Exception {
        server.addListeners(signalingSocketIOModule);
        server.start();

        String port = env.getProperty("socketio.port", "11451");
        System.out.println("========================================");
        System.out.println("Socket.IO 信令服务器启动于端口: " + port);
        System.out.println("========================================");
    }
}
