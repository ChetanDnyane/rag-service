package com.chetan.taskflow.rag.controller;

import com.chetan.taskflow.rag.model.RagRequest;
import com.chetan.taskflow.rag.model.RagResponse;
import com.chetan.taskflow.rag.service.RagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(
            RagService ragService) {

        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public RagResponse ask(
            @RequestBody RagRequest request) {

        return ragService.ask(
                request.question()
        );
    }
}