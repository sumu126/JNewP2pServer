package com.sumu.japdemo.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class FileSearchResult {
    private String hash;
    private String fileName;
    private Long fileSize;
    private Integer nodeCount;
    private List<String> nodes;
    private Boolean isExactMatch;
}
