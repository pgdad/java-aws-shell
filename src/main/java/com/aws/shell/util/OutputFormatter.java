package com.aws.shell.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

/**
 * Utility class for formatting output in various formats
 */
public class OutputFormatter {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Format object as JSON
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "Error formatting JSON: " + e.getMessage();
        }
    }

    /**
     * Format a list as a simple table
     */
    public static String toTable(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }

        // Calculate column widths
        int cols = rows.get(0).length;
        int[] widths = new int[cols];

        for (String[] row : rows) {
            for (int i = 0; i < Math.min(cols, row.length); i++) {
                if (row[i] != null) {
                    widths[i] = Math.max(widths[i], row[i].length());
                }
            }
        }

        // Build table
        StringBuilder sb = new StringBuilder();
        boolean isHeader = true;

        for (String[] row : rows) {
            for (int i = 0; i < cols; i++) {
                String value = i < row.length && row[i] != null ? row[i] : "";
                sb.append(String.format("%-" + widths[i] + "s", value));
                if (i < cols - 1) {
                    sb.append("  ");
                }
            }
            sb.append("\n");

            // Add separator after header
            if (isHeader) {
                for (int i = 0; i < cols; i++) {
                    sb.append("-".repeat(widths[i]));
                    if (i < cols - 1) {
                        sb.append("  ");
                    }
                }
                sb.append("\n");
                isHeader = false;
            }
        }

        return sb.toString();
    }

    /**
     * Format a simple key-value display
     */
    public static String toKeyValue(List<String[]> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }

        // Find max key length
        int maxKeyLength = pairs.stream()
                .filter(pair -> pair.length > 0 && pair[0] != null)
                .mapToInt(pair -> pair[0].length())
                .max()
                .orElse(0);

        StringBuilder sb = new StringBuilder();
        for (String[] pair : pairs) {
            if (pair.length >= 2) {
                String key = pair[0] != null ? pair[0] : "";
                String value = pair[1] != null ? pair[1] : "";
                sb.append(String.format("%-" + maxKeyLength + "s : %s\n", key, value));
            }
        }

        return sb.toString();
    }
}
