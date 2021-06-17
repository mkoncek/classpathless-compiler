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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.InMemoryCompiler;
import io.github.mkoncek.classpathless.api.MessagesListener;

public class Compiler implements InMemoryCompiler {
    MessagesListener messagesListener = new MessagesListener() {
        @Override
        public void addMessage(Level level, String message) {
        }
        @Override
        public void addMessage(Level level, String format, Object... args) {
        }
    };

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    InMemoryFileManager fileManager;

    private class DiagnosticToMessagesListener implements DiagnosticListener<JavaFileObject> {
        MessagesListener listener;

        DiagnosticToMessagesListener(MessagesListener listener) {
            this.listener = listener;
        }

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            var msg = diagnostic.getMessage(Locale.ENGLISH);
            final var beginText = "package ";

            if (listener != null) {
                listener.addMessage(Level.SEVERE, MessageFormat.format(
                        "Compiler diagnostic [{0}, {1}]: {2}",
                        diagnostic.getLineNumber(), diagnostic.getColumnNumber(), msg));
            }

            if (diagnostic.getCode().equals("compiler.err.doesnt.exist")) {
                if (msg.startsWith(beginText)) {
                    final var beginIdx = beginText.length();
                    final var endIdx = msg.indexOf(" does not exist");

                    missingDependencies.add(msg.substring(beginIdx, endIdx));
                }
            }
        }
    };
    private List<String> missingDependencies = new ArrayList<>();
    final List<JavaFileObject> compilationUnits = new ArrayList<>();

    public Compiler() throws IOException {
        this.fileManager = new InMemoryFileManager(
                compiler.getStandardFileManager(null, null, null), messagesListener);
    }

    @Override
    public Collection<IdentifiedBytecode> compileClass(
            ClassesProvider classprovider,
            Optional<MessagesListener> messagesConsummer,
            IdentifiedSource... javaSourceFiles) {
        final List<JavaFileObject> compilationUnits = new ArrayList<>();

        try {
            for (var source : javaSourceFiles) {
                /// NOTE the JavaFileObject's toUri method needs to return something
                /// that ends with the proper Java source file name and extension
                /// otherwise the compiler throws an error
                var sourceName = source.getClassIdentifier().getFullName();
                sourceName = sourceName.replace(".", "/") + ".java";

                compilationUnits.add(new InMemoryJavaSourceFileObject(
                        sourceName, source.getSourceCode()));
            }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        var classOutputs = new ArrayList<JavaFileObject>();
        fileManager.setClassProvider(classprovider);

        var availableClasses = new TreeSet<>(classprovider.getClassPathListing());

        fileManager.setAvailableClasses(availableClasses);

        messagesConsummer.ifPresent(ml -> {messagesListener = ml;});
        var listener = new DiagnosticToMessagesListener(messagesListener);

        while (!compiler.getTask(
                null,
                fileManager,
                listener,
                null,
                null,
                compilationUnits)
                .call()) {
            if (!missingDependencies.isEmpty()) {
                messagesListener.addMessage(Level.INFO, "resolving deps: "
                        + missingDependencies.stream().collect(Collectors.joining(", ")));
                break;
            } else {
                throw new RuntimeException("Could not compile file");
            }
        }

        fileManager.setClassProvider(null);
        fileManager.clearAndGetOutput(classOutputs);

        var result = new ArrayList<IdentifiedBytecode>();

        for (final var classOutput : classOutputs) {
            /// Remove the leading "/"
            var identifier = new ClassIdentifier(classOutput.getName().substring(1));
            byte[] content;
            try {
                content = classOutput.openInputStream().readAllBytes();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            result.add(new IdentifiedBytecode(identifier, content));
        }

        return result;
    }
}