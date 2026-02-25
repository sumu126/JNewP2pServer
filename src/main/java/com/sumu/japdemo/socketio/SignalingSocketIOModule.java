package com.sumu.japdemo.socketio;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.sumu.japdemo.entity.dto.DownloadResult;
import com.sumu.japdemo.entity.dto.FileRegister;
import com.sumu.japdemo.entity.dto.FileSearchResult;
import com.sumu.japdemo.entity.dto.WebRtcSignal;
import com.sumu.japdemo.service.FileIndexService;
import com.sumu.japdemo.service.NodeManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SignalingSocketIOModule {

    private static final Logger logger = LoggerFactory.getLogger(SignalingSocketIOModule.class);

    @Autowired
    private SocketIOServer server;

    @Autowired
    private NodeManagerService nodeManagerService;

    @Autowired
    private FileIndexService fileIndexService;

    @OnConnect
    public void onConnect(SocketIOClient client) {
        String nodeId = client.getSessionId().toString();
        InetSocketAddress remoteAddress = (InetSocketAddress) client.getRemoteAddress();
        String ipAddress = remoteAddress.getAddress().getHostAddress();

        logger.info("用户连接: {} 来自 {}", nodeId, ipAddress);

        nodeManagerService.registerNode(nodeId, ipAddress);
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String nodeId = client.getSessionId().toString();

        logger.info("用户断开连接: {}", nodeId);

        nodeManagerService.unregisterNode(nodeId);
    }

    @OnEvent("register-files")
    public void onRegisterFiles(SocketIOClient client, AckRequest ack, List<Map<String, Object>> files) {
        String nodeId = client.getSessionId().toString();

        logger.info("用户 {} 注册文件: {}", nodeId, files != null ? files.size() : 0);

        if (files != null && !files.isEmpty()) {
            List<FileRegister> fileList = new java.util.ArrayList<>();
            for (Map<String, Object> file : files) {
                FileRegister fr = new FileRegister();
                fr.setHash(String.valueOf(file.get("hash")));
                fr.setFileName(String.valueOf(file.get("fileName")));
                Object fileSize = file.get("fileSize");
                if (fileSize instanceof Number) {
                    fr.setFileSize(((Number) fileSize).longValue());
                } else {
                    fr.setFileSize(Long.parseLong(String.valueOf(fileSize)));
                }
                fileList.add(fr);
            }
            nodeManagerService.registerFiles(nodeId, fileList);
        }
    }

    @OnEvent("unregister-files")
    public void onUnregisterFiles(SocketIOClient client, AckRequest ack, List<String> hashes) {
        String nodeId = client.getSessionId().toString();

        logger.info("用户 {} 取消注册文件: {}", nodeId, hashes);

        if (hashes != null && !hashes.isEmpty()) {
            nodeManagerService.unregisterFiles(nodeId, hashes);
            logger.info("用户 {} 取消注册了 {} 个文件", nodeId, hashes.size());
        }
    }

    @OnEvent("search-files")
    public void onSearchFiles(SocketIOClient client, AckRequest ack, String query) {
        String nodeId = client.getSessionId().toString();

        logger.info("用户 {} 搜索文件: {}", nodeId, query);

        List<FileSearchResult> results = fileIndexService.searchFiles(query);
        client.sendEvent("search-results", results);

        logger.info("搜索结果: 找到 {} 个匹配文件", results.size());
    }

    @OnEvent("request-download")
    public void onRequestDownload(SocketIOClient client, AckRequest ack, String fileHash) {
        String nodeId = client.getSessionId().toString();

        logger.info("用户 {} 请求下载文件: {}", nodeId, fileHash);

        DownloadResult result = fileIndexService.getDownloadNodes(fileHash);

        if (result != null && result.getNodeCount() > 0) {
            client.sendEvent("download-nodes-found", result);
            logger.info("为用户 {} 找到文件 {} 的 {} 个下载节点", nodeId, fileHash, result.getNodeCount());
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("fileHash", fileHash);
            error.put("error", result == null ? "文件未在索引中找到" : "没有节点拥有该文件");
            client.sendEvent("download-nodes-not-found", error);
        }
    }

    @OnEvent("webrtc-signal")
    public void onWebrtcSignal(SocketIOClient client, AckRequest ack, Map<String, Object> data) {
        String fromUserId = client.getSessionId().toString();
        String targetUserId = String.valueOf(data.get("targetUserId"));
        Object signalData = data.get("signal");

        logger.debug("转发WebRTC信令: {} -> {}", fromUserId, targetUserId);

        SocketIOClient targetClient = findClient(targetUserId);
        if (targetClient != null) {
            Map<String, Object> forwardData = new HashMap<>();
            forwardData.put("fromUserId", fromUserId);
            forwardData.put("signal", signalData);
            targetClient.sendEvent("webrtc-signal", forwardData);
        }
    }

    @OnEvent("heartbeat")
    public void onHeartbeat(SocketIOClient client, AckRequest ack) {
        String nodeId = client.getSessionId().toString();

        nodeManagerService.updateHeartbeat(nodeId);
        client.sendEvent("heartbeat-ack");
    }

    private SocketIOClient findClient(String sessionId) {
        for (SocketIOClient client : server.getAllClients()) {
            if (client.getSessionId().toString().equals(sessionId)) {
                return client;
            }
        }
        return null;
    }
}
