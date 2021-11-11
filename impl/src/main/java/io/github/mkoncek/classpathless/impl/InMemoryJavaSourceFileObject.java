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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedSource;

/**
 * @author Marián Konček
 */
public class InMemoryJavaSourceFileObject extends IdentifiedJavaFileObject {
    private String source;

    protected InMemoryJavaSourceFileObject(String name) {
        super(URI.create("string:///" + name), Kind.SOURCE);
    }

    public InMemoryJavaSourceFileObject(String name, String source) {
        this(name);
        this.source = source;
    }

    public InMemoryJavaSourceFileObject(String name, InputStream is) throws IOException {
        this(name);
        var byteStream = new ByteArrayOutputStream();
        is.transferTo(byteStream);
        this.source = byteStream.toString(StandardCharsets.UTF_8);
    }

    @Override
    ClassIdentifier getClassIdentifier() {
        // Remove "string:///" prefix
        return new ClassIdentifier(toUri().toString().substring(10));
    }

    IdentifiedSource getIdentifiedSource() {
        return new IdentifiedSource(getClassIdentifier(), source.getBytes(StandardCharsets.UTF_8));
    }

    void setSource(String source) {
        this.source = source;
    }

    public InMemoryJavaSourceFileObject(Path path) throws IOException {
        this(path.toFile().getCanonicalPath().substring(1), new FileInputStream(path.toFile()));
    }

    public InMemoryJavaSourceFileObject(IdentifiedSource source) {
        // NOTE: The JavaFileObject's toUri method needs to return something
        // that ends with the proper Java source file name and extension
        // otherwise the compiler throws an error
        this(source.getClassIdentifier().getFullName().replace(".", "/") + ".java",
                source.getSourceCode());
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return source;
    }
}
