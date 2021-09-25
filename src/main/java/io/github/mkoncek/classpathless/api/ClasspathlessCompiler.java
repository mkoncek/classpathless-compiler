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
package io.github.mkoncek.classpathless.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface ClasspathlessCompiler {
    static class Arguments {
        private boolean useHostSystemClasses = true;
        private List<String> compilerFlags = new ArrayList<>();

        public boolean useHostSystemClasses() {
            return useHostSystemClasses;
        }

        public List<String> compilerOptions() {
            return Collections.unmodifiableList(compilerFlags);
        }

        public Arguments compilerOptions(Collection<String> value) {
            compilerFlags.addAll(value);
            return this;
        }

        public Arguments useHostSystemClasses(boolean value) {
            useHostSystemClasses = value;
            return this;
        }
    }

    /**
     * @param classprovider Provider for missing elements on the classpath.
     * @param messagesConsummer Accepts any diagnostic or logging information
     * from the compiler.
     * @param javaSourceFiles Files to compile.
     * @return Compiled bytecode of all javaSourceFiles.
     */
    Collection<IdentifiedBytecode> compileClass(ClassesProvider classprovider,
            Optional<MessagesListener> messagesConsummer, IdentifiedSource... javaSourceFiles);
}
