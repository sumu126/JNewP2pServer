package com.sumu.japdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sumu.japdemo.entity.FileInfo;
import com.sumu.japdemo.entity.NodeFile;
import com.sumu.japdemo.entity.NodeInfo;
import com.sumu.japdemo.entity.dto.FileRegister;
import com.sumu.japdemo.mapper.FileInfoMapper;
import com.sumu.japdemo.mapper.NodeFileMapper;
import com.sumu.japdemo.mapper.NodeInfoMapper;
import com.sumu.japdemo.service.FileIndexService;
import com.sumu.japdemo.service.NodeManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NodeManagerServiceImpl implements NodeManagerService {

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private NodeFileMapper nodeFileMapper;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private FileIndexService fileIndexService;

    @Override
    public void registerNode(String nodeId, String ipAddress) {
        NodeInfo nodeInfo = nodeInfoMapper.selectOne(
                new LambdaQueryWrapper<NodeInfo>()
                        .eq(NodeInfo::getNodeId, nodeId)
        );
        if (nodeInfo == null) {
            nodeInfo = new NodeInfo();
            nodeInfo.setCreateTime(LocalDateTime.now());
        }
        nodeInfo.setNodeId(nodeId);
        nodeInfo.setIpAddress(ipAddress);
        nodeInfo.setActive(true);
        nodeInfo.setLastHeartbeat(LocalDateTime.now());
        nodeInfo.setUpdateTime(LocalDateTime.now());

        if (nodeInfo.getId() == null) {
            nodeInfoMapper.insert(nodeInfo);
        } else {
            nodeInfoMapper.updateById(nodeInfo);
        }
    }

    @Override
    @Transactional
    public void registerFiles(String nodeId, List<FileRegister> files) {
        Set<String> newFileHashes = new HashSet<>();

        for (FileRegister file : files) {
            String fileHash = file.getHash();
            newFileHashes.add(fileHash);

            NodeFile existingNodeFile = nodeFileMapper.selectOne(
                    new LambdaQueryWrapper<NodeFile>()
                            .eq(NodeFile::getNodeId, nodeId)
                            .eq(NodeFile::getFileHash, fileHash)
            );
            if (existingNodeFile == null) {
                NodeFile nodeFile = new NodeFile();
                nodeFile.setNodeId(nodeId);
                nodeFile.setFileHash(fileHash);
                nodeFile.setCreateTime(LocalDateTime.now());
                nodeFileMapper.insert(nodeFile);
            }

            FileInfo existingFile = fileInfoMapper.selectOne(
                    new LambdaQueryWrapper<FileInfo>()
                            .eq(FileInfo::getFileHash, fileHash)
            );
            if (existingFile == null) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileHash(fileHash);
                fileInfo.setFileName(file.getFileName());
                fileInfo.setFileSize(file.getFileSize());
                fileInfo.setNodeCount(1);
                fileInfo.setCreateTime(LocalDateTime.now());
                fileInfo.setUpdateTime(LocalDateTime.now());
                fileInfoMapper.insert(fileInfo);
            } else {
                existingFile.setUpdateTime(LocalDateTime.now());
                fileInfoMapper.updateById(existingFile);
            }

            fileIndexService.updateFileNodeCount(fileHash);
        }

        List<NodeFile> oldFiles = nodeFileMapper.selectList(
                new LambdaQueryWrapper<NodeFile>()
                        .eq(NodeFile::getNodeId, nodeId)
        );
        for (NodeFile oldFile : oldFiles) {
            if (!newFileHashes.contains(oldFile.getFileHash())) {
                nodeFileMapper.delete(
                        new LambdaQueryWrapper<NodeFile>()
                                .eq(NodeFile::getNodeId, nodeId)
                                .eq(NodeFile::getFileHash, oldFile.getFileHash())
                );
                fileIndexService.updateFileNodeCount(oldFile.getFileHash());
            }
        }
    }

    @Override
    @Transactional
    public void unregisterFiles(String nodeId, List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return;
        }

        for (String hash : hashes) {
            nodeFileMapper.delete(
                    new LambdaQueryWrapper<NodeFile>()
                            .eq(NodeFile::getNodeId, nodeId)
                            .eq(NodeFile::getFileHash, hash)
            );
            fileIndexService.updateFileNodeCount(hash);
        }
    }

    @Override
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
    }

    @Override
    public void updateHeartbeat(String nodeId) {
        NodeInfo nodeInfo = nodeInfoMapper.selectOne(
                new LambdaQueryWrapper<NodeInfo>()
                        .eq(NodeInfo::getNodeId, nodeId)
        );
        if (nodeInfo != null) {
            nodeInfo.setLastHeartbeat(LocalDateTime.now());
            nodeInfo.setActive(true);
            nodeInfo.setUpdateTime(LocalDateTime.now());
            nodeInfoMapper.updateById(nodeInfo);
        }
    }

    @Override
    public boolean isNodeActive(String nodeId) {
        NodeInfo nodeInfo = nodeInfoMapper.selectOne(
                new LambdaQueryWrapper<NodeInfo>()
                        .eq(NodeInfo::getNodeId, nodeId)
        );
        return nodeInfo != null && Boolean.TRUE.equals(nodeInfo.getActive());
    }

    @Override
    public List<String> getActiveNodeIds() {
        List<NodeInfo> activeNodes = nodeInfoMapper.selectList(
                new LambdaQueryWrapper<NodeInfo>()
                        .eq(NodeInfo::getActive, true)
        );
        return activeNodes.stream()
                .map(NodeInfo::getNodeId)
                .collect(Collectors.toList());
    }
}
