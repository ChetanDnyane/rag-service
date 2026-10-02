package com.chetan.taskflow.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentSearchService {

    private final VectorStore vectorStore;

    public DocumentSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> search(String query) {

        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .build();

        return vectorStore.similaritySearch(searchRequest);
    }
}