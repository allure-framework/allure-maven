package io.qameta.allure.maven;

import org.apache.maven.shared.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Properties util class.
 */
class PropertyUtils {

    /**
     * Replaces the placeholders in properties.
     * You can use properties like:
     * name1=value1
     * name2=value2 with ${name1}
     * You can also use system and maven properties for the placeholder.
     */
    static void prepareProperties(Properties baseProperties) {
        for (String name : baseProperties.stringPropertyNames()) {
            baseProperties.setProperty(name, getPropertyValue(baseProperties.getProperty(name), baseProperties));
        }
    }

    private static String getPropertyValue(String property, Properties baseProperties ) {
        String result = property;
        Map<String, Integer> inspector = new HashMap<>();
        Pattern pattern = Pattern.compile("\\$\\{\\s*(.*?)\\s*(?<!\\\\)\\}");
        while (true) {
            Matcher matcher = pattern.matcher(result);
            if (!matcher.find()) {
                break;
            }

            String match = matcher.group(0);
            String name = matcher.group(1);

            if (!inspector.containsKey(name)) {
                inspector.put(name, Integer.valueOf(1));
            } else {
                inspector.put(name, Integer.valueOf(inspector.get(name).intValue() + 1));
            }

            String value = baseProperties.getProperty(name);
            if(value == null && !StringUtils.isEmpty(value)) {
                value = System.getProperty(name);
            }

            if (inspector.get(name).intValue() > 25) {
                value = "...";
            }
            if (value != null) {
                result = result.replace(match, value);
            }
        }
        return result;
    }
}
