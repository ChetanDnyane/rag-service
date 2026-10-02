package com.chetan.taskflow.rag.model;

public record DocumentChunk(
        String source,
        String title,
        int level,
        String parentTitle,
        int chunkIndex,
        String content
) {
}