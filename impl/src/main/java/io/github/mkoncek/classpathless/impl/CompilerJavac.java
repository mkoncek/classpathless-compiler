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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

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
import io.github.mkoncek.classpathless.api.SourcePostprocessor;
import io.github.mkoncek.classpathless.util.BytecodeExtractor;

/**
 * An implementation using javax.tools compiler API
 */
public class CompilerJavac implements ClasspathlessCompiler {
    private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private InMemoryFileManager fileManager;
    private Arguments arguments;
    private List<SourcePostprocessor> postprocessors = new ArrayList<>();

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

    public CompilerJavac(Arguments arguments) {
        this.fileManager = new InMemoryFileManager(
                compiler.getStandardFileManager(null, null, null));
        this.arguments = arguments;
    }

    public CompilerJavac() {
        this(new Arguments().useHostSystemClasses(true));
    }

    public void addPostProcessor(SourcePostprocessor postprocessor) {
        postprocessors.add(postprocessor);
    }

    private static void transitiveImport(IdentifiedBytecode bytecode,
            ClassesProvider classprovider, Set<String> result, LoggingSwitch loggingSwitch) {
        result.add(bytecode.getClassIdentifier().getFullName());
        for (var typename : BytecodeExtractor.extractTypenames(bytecode.getFile())) {
            if (result.add(typename)) {
                int nestedPos = -1;
                while ((nestedPos = typename.indexOf('$', nestedPos + 1)) != -1 &&
                        nestedPos + 1 < typename.length()) {
                    var nestedTypename = typename.substring(0, nestedPos);
                    if (!result.contains(nestedTypename)) {
                        var outerBytecodes = classprovider.getClass(new ClassIdentifier(nestedTypename));
                        if (outerBytecodes.isEmpty()) {
                            loggingSwitch.logln(Level.FINE, "Typename {0} is not a valid class", nestedTypename);
                        } else {
                            result.add(nestedTypename);
                            for (var outerBytecode : outerBytecodes) {
                                for (var nestedClass : BytecodeExtractor.extractNestedClasses(
                                        outerBytecode.getFile(), classprovider)) {
                                    result.add(nestedClass);
                                    loggingSwitch.logln(Level.FINE, "Adding nested class {0}", nestedClass);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Collection<IdentifiedBytecode> compileClass(
            ClassesProvider classprovider,
            Optional<MessagesListener> messagesConsumer,
            IdentifiedSource... javaSourceFiles) {
        var messagesListener = messagesConsumer.orElse(new NullMessagesListener());
        var loggingSwitch = new LoggingSwitch();
        loggingSwitch.setMessagesListener(messagesListener);

        Collection<InMemoryJavaSourceFileObject> compilationUnits = new ArrayList<>();
        var availableClasses = new TreeSet<String>();

        for (var source : javaSourceFiles) {
            compilationUnits.add(new InMemoryJavaSourceFileObject(source));
            for (var bytecode : classprovider.getClass(source.getClassIdentifier())) {
                transitiveImport(bytecode, classprovider, availableClasses, loggingSwitch);
                for (var nestedClass : BytecodeExtractor.extractNestedClasses(bytecode.getFile(), classprovider)) {
                    for (var nestedBytecode : classprovider.getClass(new ClassIdentifier(nestedClass))) {
                        transitiveImport(nestedBytecode, classprovider, availableClasses, loggingSwitch);
                    }
                }
            }
        }

        loggingSwitch.logln(Level.INFO, "Found typenames in the bytecode: {0}", availableClasses);

        for (var additionalClass : classprovider.getClassPathListing()) {
            if (additionalClass.charAt(0) == '[') {
                loggingSwitch.logln(Level.FINE, "Ignoring class from classpath listing: {0}", additionalClass);
                continue;
            }
            if (additionalClass.contains("/")) {
                loggingSwitch.logln(Level.FINE, "Ignoring lambda class from classpath listing: {0}", additionalClass);
                continue;
            }
            availableClasses.add(additionalClass);
        }

        // Workaround to expose this annotation even though it only has SOURCE
        // retention. The decompilers use it anyway.
        availableClasses.add("java.lang.Override");

        loggingSwitch.logln(Level.INFO, "All available typenames: {0}", availableClasses);

        fileManager.setClassProvider(classprovider);
        fileManager.setAvailableClasses(availableClasses);
        fileManager.setLoggingSwitch(loggingSwitch);
        fileManager.setArguments(arguments);

        for (boolean sourcesChanged = true; sourcesChanged;) {
            sourcesChanged = false;
            var diagnosticListener = new DiagnosticToMessagesListener(messagesListener);

            if (compiler.getTask(null, fileManager, diagnosticListener,
                    arguments.compilerOptions(), null, compilationUnits).call()) {
                break;
            }

            messagesListener.addMessage(Level.SEVERE, "Compilation failed");

            for (var entry : diagnosticListener.compilationErrors.entrySet()) {
                for (var cu : compilationUnits) {
                    if (entry.getKey().equals(cu.getClassIdentifier())) {
                        for (var postprocessor : postprocessors) {
                            var result = postprocessor.postprocess(
                                    cu.getIdentifiedSource(), entry.getValue());
                            if (result.changed) {
                                sourcesChanged = true;
                                messagesListener.addMessage(Level.INFO,
                                        "SourcePostprocessor: reloading the source code of {0}",
                                        cu.getClassIdentifier().getFullName());
                                cu.setSource(result.source.getSourceCode());
                            }
                        }
                    }
                }
            }

            if (!sourcesChanged) {
                throw new RuntimeException("Could not compile file");
            }
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

        fileManager.setClassProvider(null);
        fileManager.setLoggingSwitch(null);

        for (var resultFile : result) {
            loggingSwitch.logln(Level.INFO, "Compilation result: {0}", resultFile.getClassIdentifier().getFullName());
        }

        return result;
    }
}
