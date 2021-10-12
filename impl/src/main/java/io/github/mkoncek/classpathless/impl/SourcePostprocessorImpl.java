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

import java.util.ArrayList;
import java.util.Collection;

import io.github.mkoncek.classpathless.api.CompilationError;
import io.github.mkoncek.classpathless.api.SourcePostprocessor;

public abstract class SourcePostprocessorImpl implements SourcePostprocessor {
    /**
     * Sort the errors backwards so that the source code fixes can be applied on
     * the same file if possible.
     * @param compilationErrors The collection of compilation errors.
     * @return Sorted compilation errors.
     */
    public static Collection<CompilationError> sortBackwards(Collection<CompilationError> compilationErrors) {
        var result = new ArrayList<>(compilationErrors);

        result.sort((lhs, rhs) -> {
            int cmp = Long.compare(rhs.lineNum, lhs.lineNum);
            if (cmp == 0) {
                cmp = Long.compare(rhs.columnNum, lhs.columnNum);
            }
            return cmp;
        });

        return result;
    }

    /**
     * Add {@code abstract} keyword before the class at given coordinates.
     * @param source The source code.
     * @param lineNum Line number.
     * @param columnNum Column number.
     * @return Resulting source code.
     */
    public static String makeAbstract(String source, long lineNum, long columnNum) {
        var result = new StringBuilder();

        for (var line : source.split("\\R")) {
            --lineNum;
            if (lineNum == 0) {
                line = line.substring(0, (int) columnNum - 1) + "abstract " + line.substring((int) columnNum - 1);
            }
            result.append(line);
            result.append(System.lineSeparator());
        }

        return result.toString();
    }
}
