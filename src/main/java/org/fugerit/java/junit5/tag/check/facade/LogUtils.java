package org.fugerit.java.junit5.tag.check.facade;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

@Slf4j
public class LogUtils {

    private LogUtils() {}

    /**
     * Logs a message surrounded by a simple ASCII border using log.warn().
     * The border adapts to the length of the longest line in the message.
     *
     * @param lines The warning message.
     */
    public static void logWarningInBox(Collection<String> lines) {
        // 1. Find the maximum line length
        int maxLength = 0;
        for (String line : lines) {
            if (line.length() > maxLength) {
                maxLength = line.length();
            }
        }

        // 2. Determine the border length: max line length + 2 spaces for padding
        int contentWidth = maxLength + 2; // + 2 for the ' ' padding before and after text

        // 3. Create the top and bottom borders using the Java 8 compatible helper
        String horizontalLine = StringUtils.repeat("-", contentWidth );
        String horizontalBorder = "+" + horizontalLine + "+";

        // Log the top border
        log.warn(horizontalBorder);

        // 4. Log each line with padding
        for (String line : lines) {
            // Calculate necessary trailing spaces for uniform box width
            int trailingSpaces = maxLength - line.length();
            String paddedSpaces =  StringUtils.repeat( " ", trailingSpaces);

            String paddedLine = "| " + line + paddedSpaces + " |";
            log.warn(paddedLine);
        }

        // Log the bottom border
        log.warn(horizontalBorder);
    }

}