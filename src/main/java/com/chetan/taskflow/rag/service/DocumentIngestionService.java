package com.chetan.taskflow.rag.service;

import com.chetan.taskflow.rag.chunker.StructuredDocumentChunker;
import com.chetan.taskflow.rag.model.DocumentChunk;
import com.chetan.taskflow.rag.model.DocumentSection;
import com.chetan.taskflow.rag.parser.StructuredDocumentParser;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentIngestionService {

    private final StructuredDocumentParser parser;
    private final StructuredDocumentChunker chunker;
    private final VectorStore vectorStore;

    public DocumentIngestionService(
            StructuredDocumentParser parser,
            StructuredDocumentChunker chunker,
            VectorStore vectorStore) {

        this.parser = parser;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
    }

    public int ingestDocuments() throws IOException {

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();

        Resource[] resources =
                resolver.getResources(
                        "classpath*:documents/*.docx"
                );

        List<Document> springAiDocuments =
                new ArrayList<>();

        for (Resource resource : resources) {

            String source = resource.getFilename();

            try (InputStream inputStream =
                         resource.getInputStream()) {

                /*
                 * DOCX
                 *   ↓
                 * DocumentSection
                 */
                List<DocumentSection> sections =
                        parser.parse(
                                inputStream,
                                source
                        );

                /*
                 * DocumentSection
                 *   ↓
                 * DocumentChunk
                 */
                List<DocumentChunk> chunks =
                        chunker.chunk(sections);

                /*
                 * DocumentChunk
                 *   ↓
                 * Spring AI Document
                 */
                for (DocumentChunk chunk : chunks) {

                    Map<String, Object> metadata =
                            new HashMap<>();

                    metadata.put(
                            "source",
                            chunk.source()
                    );

                    metadata.put(
                            "title",
                            chunk.title()
                    );

                    metadata.put(
                            "level",
                            chunk.level()
                    );

                    metadata.put(
                            "chunkIndex",
                            chunk.chunkIndex()
                    );

                    /*
                     * parentTitle can legitimately be null
                     * for Heading 1 sections.
                     */
                    if (chunk.parentTitle() != null) {

                        metadata.put(
                                "parentTitle",
                                chunk.parentTitle()
                        );
                    }

                    Document document =
                            new Document(
                                    chunk.content(),
                                    metadata
                            );

                    springAiDocuments.add(document);
                }
            }
        }

        /*
         * This is where Spring AI:
         *
         * 1. generates embeddings
         * 2. sends text to the configured EmbeddingModel
         * 3. receives vectors
         * 4. stores content + metadata + vectors
         *    in PGvector
         */
        vectorStore.add(springAiDocuments);

        return springAiDocuments.size();
    }
}