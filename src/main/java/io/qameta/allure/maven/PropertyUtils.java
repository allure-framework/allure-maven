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
final class PropertyUtils {

    private PropertyUtils(){
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Replaces the placeholders in properties.
     * You can use properties like:
     * name1=value1
     * name2=value2 with ${name1}
     * You can also use system and maven properties for the placeholder.
     */
    /* default */ static void prepareProperties(Properties baseProperties) {
        for (String name : baseProperties.stringPropertyNames()) {
            baseProperties.setProperty(name, getPropertyValue(baseProperties.getProperty(name), baseProperties));
        }
    }

    private static String getPropertyValue(String property, Properties baseProperties ) {
        String result = property;
        Pattern pattern = Pattern.compile("\\$\\{\\s*(.*?)\\s*(?<!\\\\)\\}");
        while (true) {
            Matcher matcher = pattern.matcher(result);
            if (!matcher.find()) {
                break;
            }

            String match = matcher.group(0);
            String name = matcher.group(1);

            String value = baseProperties.getProperty(name);
            if(value == null && !StringUtils.isEmpty(value)) {
                value = System.getProperty(name);
            }

            if (value != null) {
                result = result.replace(match, value);
            } else {
                result = result.replace(match, "NOT_FOUND");
            }
        }
        return result;
    }
}
