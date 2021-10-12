/*-
 * Copyright (c) 2021 Marián Konček
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

import io.github.mkoncek.classpathless.api.SourcePreprocessor;

public abstract class SourcePreprocessorImpl implements SourcePreprocessor {
    /**
     * Fix import statements. Currently does:
     * 1) {@code import a..b.c} -&gt; {@code import a.$b$c;}
     * @param source The source code.
     * @return Resulting source code.
     */
    public static String fixImports(String source) {
        var resultContent = new StringBuilder();

        for (var line : source.split("\\R")) {
            if (line.startsWith("import ") && line.endsWith(";")) {
                int pos = line.indexOf("..");
                if (pos > 0) {
                    line = line.substring(0, pos + 1) + line.substring(pos + 1).replace('.', '$');
                }
            }

            resultContent.append(line);
            resultContent.append(System.lineSeparator());
        }

        return resultContent.toString();
    }
}
