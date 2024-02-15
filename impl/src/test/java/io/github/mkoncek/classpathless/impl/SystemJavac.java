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

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class SystemJavac {
    private JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    private StandardJavaFileManager fm = javac.getStandardFileManager(null, null, StandardCharsets.UTF_8);

    void compile(String... sources) {
        compile(null, sources);
    }

    void compile(List<String> options, String... sources) {
        if (!javac.getTask(null, fm, null, options, null, fm.getJavaFileObjects(sources)).call()) {
            throw new RuntimeException("Compilation failed");
        }
    }
}
