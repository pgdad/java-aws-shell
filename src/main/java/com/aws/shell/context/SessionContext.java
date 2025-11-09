package com.aws.shell.context;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Session Context for storing variables across shell commands
 * <p>
 * This component maintains a session-wide key-value store that can be used
 * to save and reuse values across different command executions.
 */
@Component
public class SessionContext {

    private final Map<String, Object> variables = new HashMap<>();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)}|\\$([a-zA-Z_][a-zA-Z0-9_]*)");

    /**
     * Set a variable value
     *
     * @param name  Variable name
     * @param value Variable value
     */
    public void set(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * Get a variable value
     *
     * @param name Variable name
     * @return Variable value or null if not found
     */
    public Object get(String name) {
        return variables.get(name);
    }

    /**
     * Get a variable value as a string
     *
     * @param name Variable name
     * @return Variable value as string or null if not found
     */
    public String getString(String name) {
        Object value = variables.get(name);
        return value != null ? value.toString() : null;
    }

    /**
     * Check if a variable exists
     *
     * @param name Variable name
     * @return true if variable exists
     */
    public boolean has(String name) {
        return variables.containsKey(name);
    }

    /**
     * Remove a variable
     *
     * @param name Variable name
     */
    public void remove(String name) {
        variables.remove(name);
    }

    /**
     * Get all variables
     *
     * @return Map of all variables
     */
    public Map<String, Object> getAll() {
        return new HashMap<>(variables);
    }

    /**
     * Clear all variables
     */
    public void clear() {
        variables.clear();
    }

    /**
     * Resolve variables in a string
     * Supports both $VAR and ${VAR} syntax
     *
     * @param input Input string potentially containing variable references
     * @return String with variables replaced by their values
     */
    public String resolveVariables(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VAR_PATTERN.matcher(input);

        while (matcher.find()) {
            // Group 1 is ${VAR}, Group 2 is $VAR
            String varName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String varValue = getString(varName);

            if (varValue != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
            } else {
                // Keep the original variable reference if not found
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Get the count of stored variables
     *
     * @return Number of variables
     */
    public int size() {
        return variables.size();
    }
}
