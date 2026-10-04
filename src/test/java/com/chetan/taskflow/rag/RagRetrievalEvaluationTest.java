package com.chetan.taskflow.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RagRetrievalEvaluationTest {

    @Autowired
    private VectorStore vectorStore;

    @Test
    void shouldRetrieveKubernetesSection() {

        // Given
        String question =
                "Why is Kubernetes justified for this architecture?";

        // When
        List<Document> results =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(5)
                                .build()
                );

        // Then
        boolean expectedSectionFound =
                results.stream()
                        .anyMatch(document ->
                                "32. Why Kubernetes Is Justified Here"
                                        .equals(
                                                document.getMetadata()
                                                        .get("title")
                                        )
                        );

        assertTrue(
                expectedSectionFound,
                "Expected Kubernetes section was not found in top 5 results"
        );
    }
}