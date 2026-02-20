package com.sumu.japdemo.service;

import com.sumu.japdemo.entity.dto.FileRegister;

import java.util.List;

public interface NodeManagerService {

    void registerNode(String nodeId, String ipAddress);

    void registerFiles(String nodeId, List<FileRegister> files);

    void unregisterNode(String nodeId);

    void updateHeartbeat(String nodeId);

    boolean isNodeActive(String nodeId);

    List<String> getActiveNodeIds();
}
