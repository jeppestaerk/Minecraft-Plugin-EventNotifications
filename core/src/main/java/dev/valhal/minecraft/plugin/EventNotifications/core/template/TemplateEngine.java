package dev.valhal.minecraft.plugin.EventNotifications.core.template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEngine {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

    private final Map<String, String> globalPlaceholders = new HashMap<>();

    public TemplateEngine(String serverName) {
        globalPlaceholders.put("server_name", serverName);
    }

    public String render(String template, Map<String, String> eventPlaceholders) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Map<String, String> allPlaceholders = new HashMap<>(globalPlaceholders);
        allPlaceholders.putAll(eventPlaceholders);

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = allPlaceholders.getOrDefault(key, "{{" + key + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public void setGlobalPlaceholder(String key, String value) {
        globalPlaceholders.put(key, value);
    }
}
