package com.sumu.japdemo.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class DownloadResult {
    private String fileHash;
    private String fileName;
    private Long fileSize;
    private List<String> nodes;
    private Integer nodeCount;
}
