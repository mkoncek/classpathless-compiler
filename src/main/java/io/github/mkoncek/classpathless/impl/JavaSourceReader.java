/*-
 * Copyright (c) 2020 Marián Konček
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mkoncek.classpathless.impl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Dummy simple class to extract information from an <code>InputStream</code>
 * representing a Java source file.
 *
 * Reads by lines removing comments until the first "{" symbol (start
 * of class / interface / enum / ...) declaration. Then removes all the content
 * until the first "@" (annotation). Then searches for the package pattern
 * in the leftover string.
 *
 * @author Marián Konček
 *
 */
public class JavaSourceReader {
    public static final Pattern packagePattern = Pattern.compile(
            "package\\s+([^;]*);");
    private static final Pattern importPattern = Pattern.compile("^import(.*);$");

    private BufferedReader br;

    public JavaSourceReader(InputStream is) {
        this.br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    static class ReadLineResult {
        String line = "";
        boolean insideComment = false;

        /**
         * Reads one line of input, ignoring content inside comments. Stores the
         * result in this.line and this.insideComment tells whether this line
         * continues with a block (/*) comment.
         * @param line The line of text to read.
         * @param insideComment Whether of not this line started inside a block comment.
         */
        public ReadLineResult(final String line, final boolean insideComment) {
            int pos = 0;

            if (insideComment) {
                var blockCommentEnd = line.indexOf("*/", pos);
                if (blockCommentEnd != -1) {
                    pos = blockCommentEnd + 2;
                } else {
                    this.insideComment = true;
                    return;
                }
            }

            while (pos < line.length()) {
                var lineCommentStart = line.indexOf("//", pos);
                var blockCommentStart = line.indexOf("/*", pos);

                if (lineCommentStart != -1 && blockCommentStart != -1) {
                    if (lineCommentStart < blockCommentStart) {
                        this.line += line.substring(pos, lineCommentStart);
                        return;
                    } else {
                        var start = line.substring(pos, blockCommentStart);
                        var blockCommentEnd = line.indexOf("*/", blockCommentStart + 2);
                        if (blockCommentEnd == -1) {
                            this.insideComment = true;
                            this.line += start;
                            return;
                        } else {
                            this.line += line.substring(pos, blockCommentStart);
                            pos = blockCommentEnd + 2;
                        }
                    }
                } else if (lineCommentStart != -1) {
                    this.line += line.substring(pos, lineCommentStart);
                    return;
                } else if (blockCommentStart != -1) {
                    var start = line.substring(pos, blockCommentStart);
                    var blockCommentEnd = line.indexOf("*/", blockCommentStart + 2);
                    if (blockCommentEnd == -1) {
                        this.insideComment = true;
                        this.line += start;
                        return;
                    } else {
                        this.line += line.substring(pos, blockCommentStart);
                        pos = blockCommentEnd + 2;
                    }
                } else {
                    this.line += line.substring(pos,  line.length());
                    return;
                }
            }
        }
    }

    /**
     * Read the package name.
     * @return Package name or null if none was found.
     * @throws IOException If an IO error occurs.
     */
    public String readSourcePackage() throws IOException {
        var text = new StringBuilder();

        boolean insideComment = false;
        while (br.ready()) {
            var line = br.readLine();
            if (line == null) {
                break;
            }

            var result = new ReadLineResult(line, insideComment);
            insideComment = result.insideComment;

            var bracketPos = result.line.indexOf('{');
            if (bracketPos != -1) {
                text.append(result.line.substring(0, bracketPos));
                break;
            } else {
                text.append(result.line);
            }

            text.append(" ");
        }

        br.close();

        {
            var ampersandPos = text.indexOf("@");
            if (ampersandPos != -1) {
                text.delete(ampersandPos, text.length());
            }
        }

        var textString = text.toString();

        var matcher = packagePattern.matcher(textString);

        if (matcher.find()) {
            textString = matcher.group(1);
            textString = textString.replaceAll("\\s", "");
        } else {
            textString = null;
        }

        return textString;
    }

    /**
     * Read the package name.
     * @param source The source code
     * @return Package name or null if none was found.
     * @throws IOException If an IO error occurs.
     */
    public static String readSourcePackage(String source) throws IOException {
        var text = new StringBuilder();

        boolean insideComment = false;
        for (String line : source.split("\\R")) {
            var result = new ReadLineResult(line, insideComment);
            insideComment = result.insideComment;

            var bracketPos = result.line.indexOf('{');
            if (bracketPos != -1) {
                text.append(result.line.substring(0, bracketPos));
                break;
            } else {
                text.append(result.line);
            }

            text.append(" ");
        }

        {
            var ampersandPos = text.indexOf("@");
            if (ampersandPos != -1) {
                text.delete(ampersandPos, text.length());
            }
        }

        var textString = text.toString();

        var matcher = packagePattern.matcher(textString);

        if (matcher.find()) {
            textString = matcher.group(1);
            textString = textString.replaceAll("\\s", "");
        } else {
            textString = null;
        }

        return textString;
    }

    public static Collection<String> readImports(String source) throws IOException {
        var result = new ArrayList<String>();

        for (String line : source.split("\\R")) {
            if (line.contains("{")) {
                break;
            }

            var matcher = importPattern.matcher(line);
            if (matcher.matches()) {
                result.add(matcher.group(1).replaceAll("\\s", ""));
            }
        }

        return result;
    }
}
