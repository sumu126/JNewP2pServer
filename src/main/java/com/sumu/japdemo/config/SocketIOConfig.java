package com.sumu.japdemo.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
public class SocketIOConfig {

    @Value("${socketio.port:11451}")
    private int socketioPort;

    @Value("${socketio.host:0.0.0.0}")
    private String socketioHost;

    private SocketIOServer server;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(socketioHost);
        config.setPort(socketioPort);
        config.setTransports(Transport.POLLING, Transport.WEBSOCKET);
        config.setOrigin("*:*");
        config.setUpgradeTimeout(10000);
        config.setPingTimeout(60000);
        config.setPingInterval(25000);

        server = new SocketIOServer(config);
        return server;
    }

    @PreDestroy
    public void stopSocketIoServer() {
        if (server != null) {
            server.stop();
            System.out.println("Socket.IO 信令服务器已停止");
        }
    }
}
