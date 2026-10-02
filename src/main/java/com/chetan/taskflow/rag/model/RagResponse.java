package com.chetan.taskflow.rag.model;

import java.util.List;

public record RagResponse(
        String answer,
        List<String> sources
) {
}