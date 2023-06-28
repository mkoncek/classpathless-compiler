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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.MessagesListener;
import io.github.mkoncek.classpathless.helpers.DiagnosticToMessagesListener;
import io.github.mkoncek.classpathless.helpers.NullMessagesListener;
import io.github.mkoncek.classpathless.helpers.WriterToMessagesListener;
import io.github.mkoncek.classpathless.util.BytecodeExtractorAccessor;

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
        var messagesListener = messagesConsumer.orElse(NullMessagesListener.INSTANCE);
        var diagnosticListener = new DiagnosticToMessagesListener(messagesListener);
        var fileManager = new InMemoryFileManager(
                compiler.getStandardFileManager(diagnosticListener, null, StandardCharsets.UTF_8));

        try (var loggingSwitch = new LoggingSwitch()) {
            loggingSwitch.setMessagesListener(messagesListener);

            loggingSwitch.logln(Level.INFO, "Starting a compilation task of sources: {0}",
                    Stream.of(javaSourceFiles).map(jsf -> jsf.getClassIdentifier().getFullName())
                    .collect(Collectors.toUnmodifiableList()));

            var compilationUnits = Arrays.stream(javaSourceFiles)
                    .map( source -> new InMemoryJavaSourceFileObject(source))
                    .collect(Collectors.toList());

            var availableClasses = initializePossibleDependence(classesProvider, loggingSwitch, javaSourceFiles);

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
                try (var is = classOutput.openInputStream()) {
                    result.add(new IdentifiedBytecode(getIdentifier(classOutput), is.readAllBytes()));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
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

    private static TreeSet<String> initializePossibleDependence(ClassesProvider classesProvider, LoggingSwitch loggingSwitch, IdentifiedSource[] javaSourceFiles) {
        var availableClasses = new TreeSet<String>();
        for (var source : javaSourceFiles) {
            var bytecodes = classesProvider.getClass(source.getClassIdentifier());
            if (areBytecodesValid(loggingSwitch, source, bytecodes)) {
                for (var bytecode : bytecodes) {
                    if (isBytecodeValid(bytecode, loggingSwitch, source)) {
                        extractAllDependenciesCatched(classesProvider, loggingSwitch, availableClasses, source, bytecode);
                    }
                }
            }
        }
        return availableClasses;
    }

    private static void extractAllDependenciesCatched(ClassesProvider classesProvider, LoggingSwitch loggingSwitch, TreeSet<String> availableClasses,
            IdentifiedSource source, IdentifiedBytecode bytecode) {
        try {
            availableClasses.addAll(extractAllDependencies(classesProvider, loggingSwitch, bytecode));
        } catch (Exception ex) {
            loggingSwitch.logln(Level.SEVERE, "An exception was thrown during the retrieval of referenced classes of bytecode {0} for source {1}: {2}",
                    bytecode.getClassIdentifier().getFullName(),
                    source.getClassIdentifier().getFullName(),
                    ex.toString());
        }
    }

    private static Collection<String> extractAllDependencies(ClassesProvider classesProvider, LoggingSwitch loggingSwitch, IdentifiedBytecode bytecode) {
        return BytecodeExtractorAccessor.extractDependenciesImpl(bytecode, classesProvider,
                groupMember -> loggingSwitch.logln(Level.FINE, "Adding class to classpath listing (nested group): {0}", groupMember),
                directlyReferenced -> loggingSwitch.logln(Level.FINE, "Adding class to classpath listing (directly referenced): {0}",
                        directlyReferenced), referencedOuter -> loggingSwitch.logln(Level.FINE,
                        "Adding class to classpath listing (outer class of directly referenced): {0}", referencedOuter));
    }

    private static boolean isBytecodeValid(IdentifiedBytecode bytecode, LoggingSwitch loggingSwitch, IdentifiedSource source) {
        if (bytecode == null) {
            loggingSwitch.logln(Level.WARNING, "ClassesProvider::getClass returned list contains null object for source {0}",
                    source.getClassIdentifier().getFullName());
            return false;
        } else if (bytecode.getFile().length < 4) {
            // 0xCAFEBABE
            loggingSwitch.logln(Level.SEVERE, "Ignoring invalid bytecode {0} for source {1}",
                    bytecode.getClassIdentifier().getFullName(),
                    source.getClassIdentifier().getFullName());
            return false;
        }
        return true;
    }

    private static boolean areBytecodesValid(LoggingSwitch loggingSwitch, IdentifiedSource source, Collection<IdentifiedBytecode> bytecodes) {
        if (bytecodes == null) {
            loggingSwitch.logln(Level.WARNING, "ClassesProvider::getClass returned null for source {0}",
                    source.getClassIdentifier().getFullName());
            return false;
        }
        return true;
    }
}
