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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;

/**
 * @implNote This class is implemented in terms of lazy loading. That makes it
 * possible for the JavaFileManager to expose file objects in its listing which
 * actually cannot be provided by the provider.
 * @author Marián Konček
 */
public class InMemoryJavaClassFileObject extends IdentifiedJavaFileObject {
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP"}, justification = "logging is safe to share")
    private LoggingSwitch loggingSwitch;
    private ClassesProvider classProvider;
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP"}, justification = "it is intended to share the same stream with the writer")
    private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

    public InMemoryJavaClassFileObject(String name, ClassesProvider classProvider, LoggingSwitch loggingSwitch) {
        super(URI.create("class:///" + name), Kind.CLASS);
        this.classProvider = classProvider;
        this.loggingSwitch = loggingSwitch;
    }

    public InMemoryJavaClassFileObject(String name, ClassesProvider classProvider) {
        this(name, classProvider, new LoggingSwitch.Null());
    }

    @Override
    ClassIdentifier getClassIdentifier() {
        return new ClassIdentifier(toUri().toString().substring(9));
    }

    @Override
    public InputStream openInputStream() throws IOException {
        loggingSwitch.traceThis(this, getClassIdentifier().getFullName(), "openInputStream");

        if (classProvider != null) {
            var bytecodes = classProvider.getClass(getClassIdentifier());
            if (bytecodes.size() == 1) {
                loggingSwitch.logln(Level.FINEST, "Found bytecode for {0}", this);
                byteStream.write(bytecodes.iterator().next().getFile());
                classProvider = null;
            } else if (bytecodes.size() == 0) {
                loggingSwitch.logln(Level.FINEST, "Bytecode for {0} not found", this);
                throw new RuntimeException("Compiler tried to access the bytecode of \"" + getClassIdentifier().getFullName() + "\" which could not be provided");
            } else {
                throw new IllegalStateException("[CPLC] InMemoryJavaClassFileObject::openInputStream: ClassesProvider provded more than one class");
            }
        }

        return new ByteArrayInputStream(byteStream.toByteArray());
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        loggingSwitch.traceThis(this, getClassIdentifier().getFullName(), "openOutputStream");
        return byteStream;
    }
}
