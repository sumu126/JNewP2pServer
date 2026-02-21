package com.sumu.japdemo.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.sumu.japdemo.entity.NodeFile;
import com.sumu.japdemo.entity.NodeInfo;
import com.sumu.japdemo.mapper.NodeFileMapper;
import com.sumu.japdemo.mapper.NodeInfoMapper;
import com.sumu.japdemo.service.FileIndexService;
import com.sumu.japdemo.service.impl.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class NodeCleanupTask implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(NodeCleanupTask.class);

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private NodeFileMapper nodeFileMapper;

    @Autowired
    private FileIndexService fileIndexService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("========== 服务器启动 - 开始清理离线节点 ==========");
        cleanupAllNodes();
        logger.info("========== 服务器启动 - 离线节点清理完成 ==========");
    }

    @Transactional
    public void cleanupAllNodes() {
        List<NodeInfo> activeNodes = nodeInfoMapper.selectList(
                new LambdaQueryWrapper<NodeInfo>()
                        .eq(NodeInfo::getActive, true)
        );

        if (activeNodes.isEmpty()) {
            logger.info("没有需要清理的在线节点");
            return;
        }

        List<String> nodeIds = activeNodes.stream()
                .map(NodeInfo::getNodeId)
                .collect(Collectors.toList());

        logger.info("发现 {} 个标记为在线的旧节点，开始清理: {}", nodeIds.size(), nodeIds);

        for (String nodeId : nodeIds) {
            unregisterNode(nodeId);
        }

        logger.info("清理完成，共处理 {} 个节点", nodeIds.size());
    }

    @Transactional
    public void unregisterNode(String nodeId) {
        List<NodeFile> nodeFiles = nodeFileMapper.selectList(
                new LambdaQueryWrapper<NodeFile>()
                        .eq(NodeFile::getNodeId, nodeId)
        );

        List<String> affectedHashes = nodeFiles.stream()
                .map(NodeFile::getFileHash)
                .distinct()
                .collect(Collectors.toList());

        nodeFileMapper.delete(
                new LambdaQueryWrapper<NodeFile>()
                        .eq(NodeFile::getNodeId, nodeId)
        );

        for (String fileHash : affectedHashes) {
            fileIndexService.updateFileNodeCount(fileHash);
        }

        NodeInfo nodeInfo = nodeInfoMapper.selectOne(
                new LambdaQueryWrapper<NodeInfo>()
                        .eq(NodeInfo::getNodeId, nodeId)
        );
        if (nodeInfo != null) {
            nodeInfo.setActive(false);
            nodeInfo.setUpdateTime(LocalDateTime.now());
            nodeInfoMapper.updateById(nodeInfo);
        }

        logger.info("节点 {} 已离线，清理了 {} 个文件注册", nodeId, affectedHashes.size());
    }

    @Scheduled(fixedRate = 30000)
    public void checkDisconnectedNodes() {
        List<NodeInfo> dbActiveNodes = nodeInfoMapper.selectList(
                new LambdaQueryWrapper<NodeInfo>()
                        .eq(NodeInfo::getActive, true)
        );

        if (dbActiveNodes.isEmpty()) {
            return;
        }

        int disconnectedCount = 0;
        for (NodeInfo dbNode : dbActiveNodes) {
            String nodeId = dbNode.getNodeId();
            boolean isActuallyOnline = WebSocketSessionManager.isSessionActive(nodeId);
            
            if (!isActuallyOnline) {
                logger.warn("节点 {} 数据库标记为在线，但 Socket 连接已断开，清理该节点", nodeId);
                unregisterNode(nodeId);
                disconnectedCount++;
            }
        }

        if (disconnectedCount > 0) {
            logger.info("定时连接检测: 清理了 {} 个假在线节点", disconnectedCount);
        }
    }
}
