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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.CompilationError;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.MessagesListener;
import io.github.mkoncek.classpathless.util.BytecodeExtractor;

/**
 * An implementation using javax.tools compiler API
 */
public class CompilerJavac implements ClasspathlessCompiler {
    private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private Arguments arguments;

    private static ClassIdentifier getIdentifier(JavaFileObject object) {
        // Remove the leading "/"
        return new ClassIdentifier(object.getName().substring(1));
    }

    private static class DiagnosticToMessagesListener implements DiagnosticListener<JavaFileObject> {
        MessagesListener listener;
        Map<ClassIdentifier, Collection<CompilationError>> compilationErrors;

        DiagnosticToMessagesListener(MessagesListener listener) {
            this.listener = listener;
            this.compilationErrors = new TreeMap<>();
        }

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            var msg = diagnostic.getMessage(Locale.ENGLISH);

            var source = diagnostic.getSource();
            if (source != null) {
                compilationErrors.computeIfAbsent(getIdentifier(source), k -> new ArrayList<>())
                .add(new CompilationError(diagnostic.getLineNumber(), diagnostic.getColumnNumber(),
                        diagnostic.getCode(), diagnostic.getMessage(null)));
            }

            if (listener != null) {
                var severity = Level.SEVERE;
                var errCode = diagnostic.getCode();
                if (errCode != null) {
                    if (errCode.startsWith("compiler.warn")) {
                        severity = Level.WARNING;
                    } else if (errCode.startsWith("compiler.note")) {
                        severity = Level.WARNING;
                    }
                }

                listener.addMessage(severity, "Compiler diagnostic at {5}[{0}, {1}]: {2}{3}(code: {4})",
                        diagnostic.getLineNumber(), diagnostic.getColumnNumber(), msg,
                        System.lineSeparator(), errCode,
                        (source != null ? "(" + source.getName() + ") " : " "));
            }
        }
    }

    /**
     * This is used in exceptional situations like runtime exceptions thrown
     * from the compiler.
     */
    private static class WriterToMessagesListener extends Writer {
        MessagesListener listener;

        WriterToMessagesListener(MessagesListener listener) {
            this.listener = listener;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            var message = new String(cbuf, off, len);
            if (message.endsWith(System.lineSeparator())) {
                message = message.substring(0, message.length() - System.lineSeparator().length());
            }
            if (!message.isBlank()) {
                listener.addMessage(Level.SEVERE, message);
            }
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    public CompilerJavac(Arguments arguments) {
        this.arguments = arguments;
    }

    public CompilerJavac() {
        this(new Arguments().useHostSystemClasses(true));
    }

    @Override
    public Collection<IdentifiedBytecode> compileClass(
            ClassesProvider classesProvider,
            Optional<MessagesListener> messagesConsumer,
            IdentifiedSource... javaSourceFiles) {
        var messagesListener = messagesConsumer.orElse(new NullMessagesListener());
        var diagnosticListener = new DiagnosticToMessagesListener(messagesListener);
        var fileManager = new InMemoryFileManager(
                compiler.getStandardFileManager(diagnosticListener, null, StandardCharsets.UTF_8));

        try (var loggingSwitch = new LoggingSwitch()) {
            loggingSwitch.setMessagesListener(messagesListener);

            loggingSwitch.logln(Level.INFO, "Starting a compilation task of sources: {0}",
                    Stream.of(javaSourceFiles).map(jsf -> jsf.getClassIdentifier().getFullName())
                    .collect(Collectors.toUnmodifiableList()));

            Collection<InMemoryJavaSourceFileObject> compilationUnits = new ArrayList<>();
            var availableClasses = new TreeSet<String>();

            for (var source : javaSourceFiles) {
                compilationUnits.add(new InMemoryJavaSourceFileObject(source));
                for (var bytecode : classesProvider.getClass(source.getClassIdentifier())) {
                    for (var newClass : BytecodeExtractor.extractFullClassGroup(bytecode.getFile(), classesProvider)) {
                        if (availableClasses.add(newClass)) {
                            loggingSwitch.logln(Level.FINE, "Adding class to classpath listing (nested group): {0}", newClass);
                        }
                    }
                }
            }

            // This also imports system classes but that shouldn't be a problem
            for (var className : new ArrayList<>(availableClasses)) {
                for (var bytecode : classesProvider.getClass(new ClassIdentifier(className))) {
                    for (var newClass : BytecodeExtractor.extractTypenames(bytecode.getFile())) {
                        if (availableClasses.add(newClass)) {
                            loggingSwitch.logln(Level.FINE, "Adding class to classpath listing (direct import): {0}", newClass);
                        }
                    }
                }
            }

            for (var className : new ArrayList<>(availableClasses)) {
                for (var bytecode : classesProvider.getClass(new ClassIdentifier(className))) {
                    for (var newClass : BytecodeExtractor.extractFullClassGroup(bytecode.getFile(), classesProvider)) {
                        if (availableClasses.add(newClass)) {
                            loggingSwitch.logln(Level.FINE, "Adding class to classpath listing (nested group of direct import): {0}", newClass);
                        }
                    }
                }
            }

            loggingSwitch.logln(Level.INFO, "Found type names in the bytecode: {0}", availableClasses);

            for (var additionalClass : classesProvider.getClassPathListing()) {
                if (additionalClass.charAt(0) == '[') {
                    loggingSwitch.logln(Level.FINE, "Ignoring array type from classpath listing: {0}", additionalClass);
                    continue;
                }
                if (additionalClass.contains("/")) {
                    loggingSwitch.logln(Level.FINE, "Ignoring lambda type from classpath listing: {0}", additionalClass);
                    continue;
                }
                availableClasses.add(additionalClass);
            }

            loggingSwitch.logln(Level.INFO, "All available type names: {0}", availableClasses);

            fileManager.setClassesProvider(classesProvider);
            fileManager.setAvailableClasses(availableClasses);
            fileManager.setLoggingSwitch(loggingSwitch);
            fileManager.setArguments(arguments);

            if (!compiler.getTask(new WriterToMessagesListener(messagesListener), fileManager, diagnosticListener,
                    arguments.compilerOptions(), null, compilationUnits).call()) {
                throw new RuntimeException("Could not compile file");
            }

            var result = new ArrayList<IdentifiedBytecode>();
            var classOutputs = new ArrayList<JavaFileObject>();

            fileManager.clearAndGetOutput(classOutputs);

            for (final var classOutput : classOutputs) {
                byte[] content;
                try {
                    content = classOutput.openInputStream().readAllBytes();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                result.add(new IdentifiedBytecode(getIdentifier(classOutput), content));
            }

            fileManager.setClassesProvider(null);
            fileManager.setLoggingSwitch(null);

            for (var resultFile : result) {
                loggingSwitch.logln(Level.INFO, "Compilation result: {0}", resultFile.getClassIdentifier().getFullName());
            }

            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
