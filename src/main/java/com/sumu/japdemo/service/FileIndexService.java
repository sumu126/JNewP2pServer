package com.sumu.japdemo.service;

import com.sumu.japdemo.entity.dto.DownloadResult;
import com.sumu.japdemo.entity.dto.FileSearchResult;

import java.util.List;

public interface FileIndexService {

    List<FileSearchResult> searchFiles(String query);

    DownloadResult getDownloadNodes(String fileHash);

    List<String> getNodeIdsForFile(String fileHash);

    void updateFileNodeCount(String fileHash);
}
