package com.chetan.taskflow.rag.controller;

import com.chetan.taskflow.rag.chunker.StructuredDocumentChunker;
import com.chetan.taskflow.rag.model.DocumentChunk;
import com.chetan.taskflow.rag.model.DocumentSection;
import com.chetan.taskflow.rag.parser.StructuredDocumentParser;
import com.chetan.taskflow.rag.service.DocumentIngestionService;
import com.chetan.taskflow.rag.service.DocumentSearchService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.*;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final StructuredDocumentParser parser;
    private final StructuredDocumentChunker chunker;
    private final DocumentIngestionService ingestionService;
    private final DocumentSearchService searchService;

    public DocumentController(
            StructuredDocumentParser parser,
            StructuredDocumentChunker chunker,
            DocumentIngestionService ingestionService,
            DocumentSearchService searchService) {

        this.parser = parser;
        this.chunker = chunker;
        this.ingestionService = ingestionService;
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public List<Document> search(
            @RequestParam String query) {

        return searchService.search(query);
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingestDocuments()
            throws IOException {

        int count =
                ingestionService.ingestDocuments();

        return Map.of(
                "message", "Documents ingested successfully",
                "chunksStored", count
        );
    }

    @GetMapping("/chunks")
    public List<DocumentChunk> getChunks()
            throws IOException {

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();

        Resource[] resources =
                resolver.getResources(
                        "classpath*:documents/*.docx"
                );

        List<DocumentSection> allSections =
                new ArrayList<>();

        for (Resource resource : resources) {

            try (InputStream inputStream =
                         resource.getInputStream()) {

                allSections.addAll(
                        parser.parse(
                                inputStream,
                                resource.getFilename()
                        )
                );
            }
        }

        return chunker.chunk(allSections);
    }

    @GetMapping("/sections")
    public List<DocumentSection> getSections()
            throws IOException {

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();

        /*
         * Discover every DOCX file under:
         *
         * src/main/resources/documents/
         *
         * No filenames are hardcoded.
         */
        Resource[] resources =
                resolver.getResources(
                        "classpath*:documents/*.docx"
                );

        List<DocumentSection> allSections =
                new ArrayList<>();

        for (Resource resource : resources) {

            String source = resource.getFilename();

            try (InputStream inputStream =
                         resource.getInputStream()) {

                List<DocumentSection> sections =
                        parser.parse(
                                inputStream,
                                source
                        );

                allSections.addAll(sections);
            }
        }

        return allSections;
    }
}