package com.chetan.taskflow.rag.chunker;

import com.chetan.taskflow.rag.model.DocumentChunk;
import com.chetan.taskflow.rag.model.DocumentSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StructuredDocumentChunker {

    /*
     * These are WORD counts, not true LLM token counts.
     *
     * We're using them for now because they make the
     * chunking algorithm easy to understand.
     */
    private static final int MAX_WORDS = 300;
    private static final int OVERLAP_WORDS = 50;

    public List<DocumentChunk> chunk(
            List<DocumentSection> sections) {

        List<DocumentChunk> chunks = new ArrayList<>();

        for (DocumentSection section : sections) {
            chunks.addAll(chunkSection(section));
        }

        return chunks;
    }

    private List<DocumentChunk> chunkSection(
            DocumentSection section) {

        List<DocumentChunk> chunks = new ArrayList<>();

        if (section.content() == null
                || section.content().isBlank()) {

            return chunks;
        }

        /*
         * Convert the section into semantic units.
         *
         * A unit is normally:
         *
         * - one paragraph
         * - one table row
         */
        List<String> units =
                createSemanticUnits(section.content());

        List<String> currentUnits =
                new ArrayList<>();

        int currentWordCount = 0;
        int chunkIndex = 0;

        for (String unit : units) {

            int unitWordCount = countWords(unit);

            /*
             * Edge case:
             *
             * A single paragraph itself is larger than
             * MAX_WORDS.
             */
            if (unitWordCount > MAX_WORDS) {

                /*
                 * First save anything already accumulated.
                 */
                if (!currentUnits.isEmpty()) {

                    chunks.add(
                            createChunk(
                                    section,
                                    chunkIndex++,
                                    currentUnits
                            )
                    );

                    currentUnits.clear();
                    currentWordCount = 0;
                }

                /*
                 * We have no semantic boundary available
                 * inside this huge unit, so fall back to
                 * word-based splitting.
                 */
                List<String> pieces =
                        splitLargeUnit(unit);

                for (String piece : pieces) {

                    chunks.add(
                            createChunk(
                                    section,
                                    chunkIndex++,
                                    List.of(piece)
                            )
                    );
                }

                continue;
            }

            /*
             * Would this unit overflow the current chunk?
             */
            if (!currentUnits.isEmpty()
                    && currentWordCount + unitWordCount
                    > MAX_WORDS) {

                /*
                 * Finish the current chunk.
                 */
                chunks.add(
                        createChunk(
                                section,
                                chunkIndex++,
                                currentUnits
                        )
                );

                /*
                 * IMPORTANT:
                 *
                 * Instead of starting completely empty,
                 * carry semantic units from the end of the
                 * previous chunk into the next chunk.
                 */
                List<String> overlapUnits =
                        getOverlapUnits(currentUnits);

                currentUnits =
                        new ArrayList<>(overlapUnits);

                currentWordCount =
                        countWords(currentUnits);
            }

            currentUnits.add(unit);
            currentWordCount += unitWordCount;
        }

        /*
         * Save the final chunk.
         */
        if (!currentUnits.isEmpty()) {

            chunks.add(
                    createChunk(
                            section,
                            chunkIndex,
                            currentUnits
                    )
            );
        }

        return chunks;
    }

    /**
     * Convert section content into semantic units.
     *
     * Paragraphs are separated by blank lines.
     *
     * Tables contain rows separated by single line breaks,
     * so each row also becomes an independent unit.
     */
    private List<String> createSemanticUnits(
            String content) {

        List<String> units = new ArrayList<>();

        String[] blocks =
                content.split("\\R\\s*\\R");

        for (String block : blocks) {

            if (block == null || block.isBlank()) {
                continue;
            }

            /*
             * A block may be:
             *
             * normal paragraph
             *
             * OR
             *
             * multiple table rows separated by newlines.
             */
            String[] lines =
                    block.split("\\R");

            for (String line : lines) {

                if (line != null && !line.isBlank()) {
                    units.add(line.trim());
                }
            }
        }

        return units;
    }

    /**
     * Select complete semantic units from the END
     * of the previous chunk until we reach roughly
     * OVERLAP_WORDS.
     */
    private List<String> getOverlapUnits(
            List<String> previousUnits) {

        List<String> overlapUnits =
                new ArrayList<>();

        int overlapWordCount = 0;

        /*
         * Walk backwards because overlap comes from
         * the END of the previous chunk.
         */
        for (int i = previousUnits.size() - 1;
             i >= 0;
             i--) {

            String unit = previousUnits.get(i);

            int words = countWords(unit);

            /*
             * Always allow at least one unit.
             *
             * After that, stop if adding another complete
             * unit would exceed our overlap target.
             */
            if (!overlapUnits.isEmpty()
                    && overlapWordCount + words
                    > OVERLAP_WORDS) {

                break;
            }

            /*
             * Insert at position 0 because we're walking
             * backwards but want original document order.
             */
            overlapUnits.add(0, unit);

            overlapWordCount += words;

            if (overlapWordCount >= OVERLAP_WORDS) {
                break;
            }
        }

        return overlapUnits;
    }

    /**
     * Last-resort splitting when ONE semantic unit
     * exceeds MAX_WORDS.
     *
     * Example: one enormous paragraph with no useful
     * internal paragraph boundaries.
     */
    private List<String> splitLargeUnit(
            String text) {

        List<String> pieces = new ArrayList<>();

        String[] words =
                text.trim().split("\\s+");

        StringBuilder currentPiece =
                new StringBuilder();

        int currentWordCount = 0;

        for (String word : words) {

            if (currentWordCount >= MAX_WORDS) {

                pieces.add(
                        currentPiece
                                .toString()
                                .trim()
                );

                currentPiece =
                        new StringBuilder();

                currentWordCount = 0;
            }

            if (!currentPiece.isEmpty()) {
                currentPiece.append(" ");
            }

            currentPiece.append(word);
            currentWordCount++;
        }

        if (!currentPiece.isEmpty()) {

            pieces.add(
                    currentPiece
                            .toString()
                            .trim()
            );
        }

        return pieces;
    }

    private DocumentChunk createChunk(
            DocumentSection section,
            int chunkIndex,
            List<String> units) {

        String content =
                String.join("\n", units);

        return new DocumentChunk(
                section.source(),
                section.title(),
                section.level(),
                section.parentTitle(),
                chunkIndex,
                content
        );
    }

    private int countWords(
            List<String> units) {

        int count = 0;

        for (String unit : units) {
            count += countWords(unit);
        }

        return count;
    }

    private int countWords(
            String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        return text
                .trim()
                .split("\\s+")
                .length;
    }
}