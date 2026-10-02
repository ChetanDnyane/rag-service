package com.chetan.taskflow.rag.model;

public record DocumentSection(
        String source,
        String title,
        int level,
        String parentTitle,
        String content
) {
}