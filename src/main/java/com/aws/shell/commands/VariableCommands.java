package com.aws.shell.commands;

import com.aws.shell.context.SessionContext;
import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Variable Commands
 * <p>
 * Provides shell variable management functionality.
 * Variables can be set, retrieved, and used in other commands.
 */
@ShellComponent
public class VariableCommands {

    private final SessionContext sessionContext;

    public VariableCommands(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    /**
     * Set a variable
     * <p>
     * Usage:
     * set MY_VAR value
     * set INSTANCE_ID i-1234567890abcdef0
     * set BUCKET_NAME my-test-bucket
     */
    @ShellMethod(key = "set", value = "Set a shell variable")
    public String setVariable(String name, String value) {
        // Resolve any variables in the value
        String resolvedValue = sessionContext.resolveVariables(value);
        sessionContext.set(name, resolvedValue);
        return "Variable set: " + name + " = " + resolvedValue;
    }

    /**
     * Get a variable value
     * <p>
     * Usage:
     * get MY_VAR
     */
    @ShellMethod(key = "get", value = "Get a variable value")
    public String getVariable(String name) {
        if (!sessionContext.has(name)) {
            return "Variable not found: " + name;
        }
        return sessionContext.getString(name);
    }

    /**
     * List all variables
     * <p>
     * Usage:
     * vars
     * variables
     */
    @ShellMethod(key = {"vars", "variables"}, value = "List all shell variables")
    public String listVariables() {
        Map<String, Object> vars = sessionContext.getAll();

        if (vars.isEmpty()) {
            return "No variables set";
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Variable", "Value"});

        vars.forEach((key, value) -> {
            String valueStr = value.toString();
            // Truncate long values for display
            if (valueStr.length() > 100) {
                valueStr = valueStr.substring(0, 97) + "...";
            }
            rows.add(new String[]{key, valueStr});
        });

        return OutputFormatter.toTable(rows);
    }

    /**
     * Unset a variable
     * <p>
     * Usage:
     * unset MY_VAR
     */
    @ShellMethod(key = "unset", value = "Unset a shell variable")
    public String unsetVariable(String name) {
        if (!sessionContext.has(name)) {
            return "Variable not found: " + name;
        }
        sessionContext.remove(name);
        return "Variable unset: " + name;
    }

    /**
     * Clear all variables
     * <p>
     * Usage:
     * clear-vars
     */
    @ShellMethod(key = "clear-vars", value = "Clear all shell variables")
    public String clearVariables() {
        int count = sessionContext.size();
        sessionContext.clear();
        return "Cleared " + count + " variable" + (count != 1 ? "s" : "");
    }

    /**
     * Export command output to a variable
     * This is a helper command that can save the first word/line of output
     * <p>
     * Usage:
     * export MY_VAR some-value
     */
    @ShellMethod(key = "export", value = "Export a value to a variable (alias for set)")
    public String exportVariable(String name, String value) {
        return setVariable(name, value);
    }

    /**
     * Echo a value with variable substitution
     * <p>
     * Usage:
     * echo Hello $USER
     * echo Instance ID is $INSTANCE_ID
     */
    @ShellMethod(key = "echo", value = "Echo text with variable substitution")
    public String echo(String text) {
        return sessionContext.resolveVariables(text);
    }
}
