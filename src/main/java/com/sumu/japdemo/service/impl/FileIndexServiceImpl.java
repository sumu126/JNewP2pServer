package com.sumu.japdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sumu.japdemo.entity.FileInfo;
import com.sumu.japdemo.entity.NodeFile;
import com.sumu.japdemo.entity.dto.DownloadResult;
import com.sumu.japdemo.entity.dto.FileSearchResult;
import com.sumu.japdemo.mapper.FileInfoMapper;
import com.sumu.japdemo.mapper.NodeFileMapper;
import com.sumu.japdemo.service.FileIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileIndexServiceImpl implements FileIndexService {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private NodeFileMapper nodeFileMapper;

    @Override
    public List<FileSearchResult> searchFiles(String query) {
        List<FileSearchResult> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String lowerQuery = query.toLowerCase().trim();
        boolean isHashSearch = lowerQuery.matches("^[a-f0-9]{64}$");

        if (isHashSearch) {
            FileInfo fileInfo = fileInfoMapper.selectOne(
                    new LambdaQueryWrapper<FileInfo>()
                            .eq(FileInfo::getFileHash, lowerQuery)
            );
            if (fileInfo != null) {
                List<String> activeNodes = getActiveNodesForFile(fileInfo.getFileHash());
                if (!activeNodes.isEmpty()) {
                    FileSearchResult result = buildSearchResult(fileInfo, activeNodes, true);
                    results.add(result);
                }
            }
        } else {
            List<FileInfo> files = fileInfoMapper.selectList(
                    new LambdaQueryWrapper<FileInfo>()
                            .like(FileInfo::getFileName, lowerQuery)
            );
            for (FileInfo file : files) {
                List<String> activeNodes = getActiveNodesForFile(file.getFileHash());
                if (!activeNodes.isEmpty()) {
                    FileSearchResult result = buildSearchResult(file, activeNodes, false);
                    results.add(result);
                }
            }
        }

        return results;
    }

    @Override
    public DownloadResult getDownloadNodes(String fileHash) {
        FileInfo fileInfo = fileInfoMapper.selectOne(
                new LambdaQueryWrapper<FileInfo>()
                        .eq(FileInfo::getFileHash, fileHash)
        );
        if (fileInfo == null) {
            return null;
        }
        List<String> activeNodes = getActiveNodesForFile(fileHash);
        if (activeNodes.isEmpty()) {
            return null;
        }
        DownloadResult result = new DownloadResult();
        result.setFileHash(fileHash);
        result.setFileName(fileInfo.getFileName());
        result.setFileSize(fileInfo.getFileSize());
        result.setNodes(activeNodes);
        result.setNodeCount(activeNodes.size());
        return result;
    }

    @Override
    public List<String> getNodeIdsForFile(String fileHash) {
        List<NodeFile> nodeFiles = nodeFileMapper.selectList(
                new LambdaQueryWrapper<NodeFile>()
                        .eq(NodeFile::getFileHash, fileHash)
        );
        return nodeFiles.stream()
                .map(NodeFile::getNodeId)
                .collect(Collectors.toList());
    }

    @Override
    public void updateFileNodeCount(String fileHash) {
        FileInfo fileInfo = fileInfoMapper.selectOne(
                new LambdaQueryWrapper<FileInfo>()
                        .eq(FileInfo::getFileHash, fileHash)
        );
        if (fileInfo != null) {
            int nodeCount = Math.toIntExact(nodeFileMapper.selectCount(
                    new LambdaQueryWrapper<NodeFile>()
                            .eq(NodeFile::getFileHash, fileHash)
            ));
            if (nodeCount == 0) {
                fileInfoMapper.deleteById(fileInfo.getId());
            } else {
                fileInfo.setNodeCount(nodeCount);
                fileInfo.setUpdateTime(LocalDateTime.now());
                fileInfoMapper.updateById(fileInfo);
            }
        }
    }

    private List<String> getActiveNodesForFile(String fileHash) {
        List<String> nodeIds = getNodeIdsForFile(fileHash);
        return nodeIds.stream()
                .filter(this::isNodeSessionActive)
                .collect(Collectors.toList());
    }

    private boolean isNodeSessionActive(String nodeId) {
        return WebSocketSessionManager.isSessionActive(nodeId);
    }

    private FileSearchResult buildSearchResult(FileInfo fileInfo, List<String> activeNodes, boolean isExactMatch) {
        FileSearchResult result = new FileSearchResult();
        result.setHash(fileInfo.getFileHash());
        result.setFileName(fileInfo.getFileName());
        result.setFileSize(fileInfo.getFileSize());
        result.setNodeCount(activeNodes.size());
        result.setNodes(activeNodes);
        result.setIsExactMatch(isExactMatch);
        return result;
    }
}
