package com.chetan.taskflow.rag.service;

import com.chetan.taskflow.rag.model.RagResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final DocumentSearchService searchService;
    private final ChatClient chatClient;

    public RagService(
            DocumentSearchService searchService,
            ChatClient.Builder chatClientBuilder) {

        this.searchService = searchService;
        this.chatClient = chatClientBuilder.build();
    }

    public RagResponse ask(String question) {

        /*
         * =============================================
         * R = RETRIEVAL
         * =============================================
         *
         * Search pgvector for the chunks most
         * semantically related to the question.
         */
        List<Document> relevantDocuments =
                searchService.search(question);

        /*
         * =============================================
         * A = AUGMENTATION
         * =============================================
         *
         * Convert the retrieved documents into context
         * that we will provide to the LLM.
         */
        String context =
                buildContext(relevantDocuments);

        /*
         * =============================================
         * G = GENERATION
         * =============================================
         *
         * Ask the chat model to answer using the
         * retrieved context.
         */
        String answer =
                chatClient
                        .prompt()
                        .system("""
                                You are a document question-answering assistant.

                                Answer the user's question using only the
                                supplied document context.

                                Do not use outside knowledge.

                                If the answer cannot be determined from the
                                supplied context, say:

                                "I could not find that information in the provided documents."

                                Keep the answer concise and factual.
                                """)
                        .user("""
                                DOCUMENT CONTEXT:

                                %s

                                QUESTION:

                                %s
                                """.formatted(
                                context,
                                question
                        ))
                        .call()
                        .content();

        List<String> sources =
                relevantDocuments
                        .stream()
                        .map(this::createSourceDescription)
                        .distinct()
                        .toList();

        return new RagResponse(
                answer,
                sources
        );
    }

    private String buildContext(
            List<Document> documents) {

        StringBuilder context =
                new StringBuilder();

        for (Document document : documents) {

            Object source =
                    document.getMetadata()
                            .get("source");

            Object title =
                    document.getMetadata()
                            .get("title");

            Object chunkIndex =
                    document.getMetadata()
                            .get("chunkIndex");

            context.append(
                    "SOURCE: "
            ).append(source);

            context.append(
                    "\nSECTION: "
            ).append(title);

            context.append(
                    "\nCHUNK: "
            ).append(chunkIndex);

            context.append(
                    "\nCONTENT:\n"
            );

            context.append(
                    document.getText()
            );

            context.append(
                    "\n\n--------------------\n\n"
            );
        }

        return context.toString();
    }

    private String createSourceDescription(
            Document document) {

        Object source =
                document.getMetadata()
                        .get("source");

        Object title =
                document.getMetadata()
                        .get("title");

        return source + " - " + title;
    }
}