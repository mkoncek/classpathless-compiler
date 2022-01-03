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
package io.github.mkoncek.classpathless.util;

import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Consumer;

import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

public class BytecodeExtractorAccessor {
    public static Collection<String> extractDependenciesImpl(
            IdentifiedBytecode initialClass, ClassesProvider classesProvider,
            Consumer<String> first, Consumer<String> second, Consumer<String> third) {
        var impl = BytecodeExtractor.extractDependenciesImpl2(initialClass, classesProvider, first, second, third);
        var result = new TreeSet<String>();
        for (var entry : impl.entrySet()) {
            result.add(entry.getKey());
            result.addAll(entry.getValue());
        }
        result.remove(initialClass.getClassIdentifier().getFullName());
        return result;
    }
}
