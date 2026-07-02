package com.deveshparmar.codesage.review.application;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UnifiedDiffParser {

    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");

    public Set<Integer> parseChangedNewLines(String patch) {
        Set<Integer> changedLines = new LinkedHashSet<>();
        if (patch == null || patch.isBlank()) {
            return changedLines;
        }

        int newLine = 0;
        boolean inHunk = false;

        for (String line : patch.split("\n", -1)) {
            Matcher matcher = HUNK_HEADER.matcher(line);
            if (matcher.find()) {
                newLine = Integer.parseInt(matcher.group(3));
                inHunk = true;
                continue;
            }
            if (!inHunk) {
                continue;
            }
            if (line.startsWith("+") && !line.startsWith("+++")) {
                changedLines.add(newLine);
                newLine++;
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                // Removed line affects old file only.
            } else if (line.startsWith(" ") || line.startsWith("\t")) {
                newLine++;
            } else if (line.startsWith("\\")) {
                // No newline at end of file marker.
            }
        }
        return changedLines;
    }
}
