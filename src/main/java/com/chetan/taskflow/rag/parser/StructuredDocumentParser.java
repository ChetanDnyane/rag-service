package com.chetan.taskflow.rag.parser;

import com.chetan.taskflow.rag.model.DocumentSection;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class StructuredDocumentParser {

    public List<DocumentSection> parse(
            InputStream inputStream,
            String source) throws IOException {

        List<DocumentSection> sections = new ArrayList<>();

        try (XWPFDocument document = new XWPFDocument(inputStream)) {

            String currentTitle = null;
            int currentLevel = 0;
            String currentParentTitle = null;

            StringBuilder currentContent = new StringBuilder();

            /*
             * Stores the most recent heading at each level.
             *
             * Example:
             *
             * headingHierarchy[1] = "38. Spring AI + RAG..."
             * headingHierarchy[2] = "38.5 Document Ingestion Flow"
             */
            String[] headingHierarchy = new String[10];

            /*
             * TEMPORARY DEBUG FLAG.
             *
             * We enable this when section 9 starts and disable
             * it when section 10 starts.
             */
            boolean debugTechnologyBaseline = false;

            /*
             * getBodyElements() returns paragraphs and tables
             * in document order.
             */
            for (IBodyElement element : document.getBodyElements()) {

                /*
                 * ==================================================
                 * PARAGRAPH
                 * ==================================================
                 */
                if (element instanceof XWPFParagraph paragraph) {

                    String text = paragraph.getText();

                    if (text == null) {
                        text = "";
                    }

                    /*
                     * Start debugging when we encounter section 9.
                     */
                    if ("9. Technology Baseline".equals(text.trim())) {

                        debugTechnologyBaseline = true;

                        System.out.println();
                        System.out.println(
                                "========================================"
                        );
                        System.out.println(
                                "START DEBUGGING SECTION 9"
                        );
                        System.out.println(
                                "========================================"
                        );
                    }

                    /*
                     * Print everything Apache POI tells us
                     * about paragraphs while we're inside
                     * the section being investigated.
                     */
                    if (debugTechnologyBaseline) {

                        System.out.println();
                        System.out.println("PARAGRAPH FOUND");

                        System.out.println(
                                "  class = "
                                        + paragraph
                                        .getClass()
                                        .getName()
                        );

                        System.out.println(
                                "  elementType = "
                                        + paragraph.getElementType()
                        );

                        System.out.println(
                                "  style = "
                                        + paragraph.getStyle()
                        );

                        System.out.println(
                                "  text = ["
                                        + text
                                        + "]"
                        );
                    }

                    /*
                     * Section 10 means we've passed the area
                     * we want to investigate.
                     *
                     * Notice this happens AFTER printing section
                     * 10, which gives us a useful boundary marker.
                     */
                    boolean endDebugging =
                            "10. Docker Packaging"
                                    .equals(text.trim());

                    /*
                     * Normal parsing starts here.
                     */
                    if (!text.isBlank()) {

                        text = text.trim();

                        int headingLevel =
                                getHeadingLevel(paragraph);

                        /*
                         * This paragraph is a heading.
                         */
                        if (headingLevel > 0) {

                            /*
                             * New heading means the previous
                             * section has ended.
                             */
                            if (currentTitle != null) {

                                sections.add(
                                        new DocumentSection(
                                                source,
                                                currentTitle,
                                                currentLevel,
                                                currentParentTitle,
                                                currentContent
                                                        .toString()
                                                        .trim()
                                        )
                                );
                            }

                            /*
                             * Start the new section.
                             */
                            currentTitle = text;
                            currentLevel = headingLevel;

                            /*
                             * Determine the parent heading.
                             */
                            if (headingLevel > 1) {

                                currentParentTitle =
                                        headingHierarchy[
                                                headingLevel - 1
                                                ];

                            } else {

                                currentParentTitle = null;
                            }

                            /*
                             * Remember this heading.
                             */
                            headingHierarchy[headingLevel] = text;

                            /*
                             * Clear stale child headings.
                             */
                            for (int i = headingLevel + 1;
                                 i < headingHierarchy.length;
                                 i++) {

                                headingHierarchy[i] = null;
                            }

                            /*
                             * New section starts with
                             * empty content.
                             */
                            currentContent =
                                    new StringBuilder();

                        } else if (currentTitle != null) {

                            /*
                             * Normal paragraph belonging to
                             * the current section.
                             */
                            appendContent(
                                    currentContent,
                                    text
                            );
                        }
                    }

                    /*
                     * Stop debugging once section 10
                     * has been processed.
                     */
                    if (endDebugging) {

                        System.out.println();
                        System.out.println(
                                "========================================"
                        );
                        System.out.println(
                                "END DEBUGGING SECTION 9"
                        );
                        System.out.println(
                                "========================================"
                        );
                        System.out.println();

                        debugTechnologyBaseline = false;
                    }
                }

                /*
                 * ==================================================
                 * TABLE
                 * ==================================================
                 */
                else if (element instanceof XWPFTable table) {

                    /*
                     * Debug information for any table found
                     * between sections 9 and 10.
                     */
                    if (debugTechnologyBaseline) {

                        System.out.println();
                        System.out.println("TABLE FOUND");

                        System.out.println(
                                "  class = "
                                        + table
                                        .getClass()
                                        .getName()
                        );

                        System.out.println(
                                "  elementType = "
                                        + table.getElementType()
                        );

                        System.out.println(
                                "  rows = "
                                        + table.getNumberOfRows()
                        );

                        System.out.println(
                                "  rawText = ["
                                        + table.getText()
                                        + "]"
                        );

                        /*
                         * Print every row and cell separately.
                         */
                        int rowIndex = 0;

                        for (XWPFTableRow row :
                                table.getRows()) {

                            System.out.println(
                                    "  ROW " + rowIndex
                            );

                            int cellIndex = 0;

                            for (XWPFTableCell cell :
                                    row.getTableCells()) {

                                System.out.println(
                                        "    CELL "
                                                + cellIndex
                                                + " = ["
                                                + cell.getText()
                                                + "]"
                                );

                                cellIndex++;
                            }

                            rowIndex++;
                        }
                    }

                    /*
                     * Existing table parsing logic.
                     *
                     * The table belongs to whichever
                     * section is currently active.
                     */
                    if (currentTitle != null) {

                        String tableText =
                                extractTableText(table);

                        if (!tableText.isBlank()) {

                            appendContent(
                                    currentContent,
                                    tableText
                            );
                        }
                    }
                }

                /*
                 * ==================================================
                 * UNKNOWN BODY ELEMENT
                 * ==================================================
                 *
                 * Normally getBodyElements() gives us paragraphs
                 * and tables. If something else appears while
                 * debugging section 9, we want to know.
                 */
                else {

                    if (debugTechnologyBaseline) {

                        System.out.println();
                        System.out.println(
                                "UNKNOWN BODY ELEMENT FOUND"
                        );

                        System.out.println(
                                "  class = "
                                        + element
                                        .getClass()
                                        .getName()
                        );

                        System.out.println(
                                "  elementType = "
                                        + element.getElementType()
                        );
                    }
                }
            }

            /*
             * Save the final section because there is no
             * following heading to trigger its save.
             */
            if (currentTitle != null) {

                sections.add(
                        new DocumentSection(
                                source,
                                currentTitle,
                                currentLevel,
                                currentParentTitle,
                                currentContent
                                        .toString()
                                        .trim()
                        )
                );
            }
        }

        return sections;
    }

    /**
     * Detect Word heading styles:
     *
     * Heading1
     * Heading2
     * Heading3
     * ...
     */
    private int getHeadingLevel(
            XWPFParagraph paragraph) {

        String style = paragraph.getStyle();

        if (style == null || style.isBlank()) {
            return 0;
        }

        if (style.matches("(?i)Heading\\d+")) {

            String level =
                    style.replaceAll(
                            "(?i)Heading",
                            ""
                    );

            try {

                return Integer.parseInt(level);

            } catch (NumberFormatException ignored) {

                return 0;
            }
        }

        return 0;
    }

    /**
     * Converts a Word table into readable text.
     *
     * Example:
     *
     * Technology | Choice
     * Backend | Java + Spring Boot
     * Database | PostgreSQL
     */
    private String extractTableText(
            XWPFTable table) {

        StringBuilder tableContent =
                new StringBuilder();

        for (XWPFTableRow row :
                table.getRows()) {

            List<String> cellValues =
                    new ArrayList<>();

            for (XWPFTableCell cell :
                    row.getTableCells()) {

                String cellText =
                        extractCellText(cell);

                cellValues.add(cellText);
            }

            String rowText =
                    String.join(
                            " | ",
                            cellValues
                    );

            if (!rowText.isBlank()) {

                if (!tableContent.isEmpty()) {
                    tableContent.append("\n");
                }

                tableContent.append(rowText);
            }
        }

        return tableContent.toString();
    }

    /**
     * Extract all paragraphs contained inside
     * a single Word table cell.
     */
    private String extractCellText(
            XWPFTableCell cell) {

        StringBuilder cellContent =
                new StringBuilder();

        for (XWPFParagraph paragraph :
                cell.getParagraphs()) {

            String text =
                    paragraph.getText();

            if (text == null || text.isBlank()) {
                continue;
            }

            if (!cellContent.isEmpty()) {
                cellContent.append(" ");
            }

            cellContent.append(
                    text.trim()
            );
        }

        return cellContent.toString();
    }

    /**
     * Append paragraph/table text to the current
     * logical document section.
     */
    private void appendContent(
            StringBuilder currentContent,
            String text) {

        if (text == null || text.isBlank()) {
            return;
        }

        if (!currentContent.isEmpty()) {
            currentContent.append("\n\n");
        }

        currentContent.append(
                text.trim()
        );
    }
}