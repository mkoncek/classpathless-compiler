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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public interface ClasspathlessCompiler {
    static class Arguments {
        private boolean useHostSystemClasses = true;
        private boolean useHostJavaLangObject = true;
        private List<String> compilerOptions = Collections.emptyList();
        private Map<String, String> patchModules = Collections.emptyMap();

        /**
         * @return A view of the compiler argument strings.
         */
        public List<String> compilerOptions() {
            return Collections.unmodifiableList(compilerOptions);
        }

        /**
         * @return The value of the option.
         */
        public boolean useHostSystemClasses() {
            return useHostSystemClasses;
        }

        /**
         * @return The value of the option.
         */
        public boolean useHostJavaLangObject() {
            return useHostJavaLangObject;
        }

        /**
         * @return The view of package names mapped to module names.
         */
        public Map<String, String> patchModules() {
            return Collections.unmodifiableMap(patchModules);
        }

        /**
         * Set flags which will be passed to the compiler.
         * @param value A collection of compiler flags.
         * @return this.
         */
        public Arguments compilerOptions(Collection<String> value) {
            compilerOptions = new ArrayList<String>(value.size());
            Iterator<String> it = value.iterator();
            while (it.hasNext()) {
                String option = it.next();
                if (option.equals("--patch-module")) {
                    this.patchModules = new TreeMap<>();
                    String patchModuleString = it.next();
                    String[] patchModules = patchModuleString.split("=");
                    String moduleName = patchModules[0];
                    for (String packageName : patchModules[1].split(":")) {
                        this.patchModules.put(packageName, moduleName);
                    }
                } else {
                    compilerOptions.add(option);
                }
            }
            return this;
        }

        /**
         * Set flag whether or not to use host system classes. System classes
         * are those provided with the installation of JDK. If set to false, the
         * compiler will use system classes provided by the provider.
         * @param value The value of the option.
         * @return this.
         */
        public Arguments useHostSystemClasses(boolean value) {
            useHostSystemClasses = value;
            return this;
        }

        /**
         * Set flag whether or not to use host java.lang.Object class regardless
         * of "useHostSystemClasses". Setting this to true is a workaround
         * preventing processes running under the DCEVM JVM from crashing.
         * @param value The value of the option.
         * @return this.
         */
        public Arguments useHostJavaLangObject(boolean value) {
            useHostJavaLangObject = value;
            return this;
        }
    }

    /**
     * @param classesProvider Provider for missing elements on the classpath.
     * @param messagesListener Accepts any diagnostic or logging information
     * from the compiler.
     * @param javaSourceFiles Files to compile.
     * @return Compiled bytecode of all javaSourceFiles.
     */
    Collection<IdentifiedBytecode> compileClass(ClassesProvider classesProvider,
            Optional<MessagesListener> messagesListener, IdentifiedSource... javaSourceFiles);
}
