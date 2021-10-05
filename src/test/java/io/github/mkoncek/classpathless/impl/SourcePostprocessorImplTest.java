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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.impl.postprocessors.AddAbstract;

public class SourcePostprocessorImplTest {
    private static final SystemJavac javac = new SystemJavac();

    @Test
    void testAbstractSimple() throws IOException {
        String source;
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/DerivedClass.CPLC.java")) {
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        source = SourcePostprocessorImpl.makeAbstract(source, 4, 12);
        source = SourcePostprocessorImpl.makeAbstract(source, 3, 8);

        assertEquals(""
                + "package postprocessor;\n"
                + "\n"
                + "public abstract class DerivedClass extends AbstractBase {\n"
                + "    static abstract class DerivedNestedClass extends AbstractBase.AbstractNestedBase {\n"
                + "    }\n"
                + "}\n"
                + "", source);
    }

    @Test
    void testAbstract() throws IOException {
        var abstractFile = "src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/AbstractBase.java";

        var baseFiles = new String[] {
                "src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/AbstractBase.class",
                "src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/AbstractBase$AbstractNestedBase.class",
        };

        for (var file : baseFiles) {
            Files.deleteIfExists(Paths.get(file));
        }

        javac.compile(abstractFile);

        var derivedFiles = new String[] {
                "src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/DerivedClass.class",
                "src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/DerivedClass$DerivedNestedClass.class",
        };

        for (var file : derivedFiles) {
            Files.deleteIfExists(Paths.get(file));
        }

        javac.compile(abstractFile,
                "src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/DerivedClass.java");

        var classes = new ArrayList<IdentifiedBytecode>();

        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/DerivedClass.class")) {
            classes.add(new IdentifiedBytecode(new ClassIdentifier("postprocessor.DerivedClass"), is.readAllBytes()));
        }
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/DerivedClass$DerivedNestedClass.class")) {
            classes.add(new IdentifiedBytecode(new ClassIdentifier("postprocessor.DerivedClass$DerivedNestedClass"), is.readAllBytes()));
        }
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/AbstractBase.class")) {
            classes.add(new IdentifiedBytecode(new ClassIdentifier("postprocessor.AbstractBase"), is.readAllBytes()));
        }
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/AbstractBase$AbstractNestedBase.class")) {
            classes.add(new IdentifiedBytecode(new ClassIdentifier("postprocessor.AbstractBase$AbstractNestedBase"), is.readAllBytes()));
        }

        var cplc = new CompilerJavac(new CompilerJavac.Arguments().useHostSystemClasses(true));
        cplc.setPostProcessor(new AddAbstract());

        var provider = new SimpleClassesProvider(classes);

        IdentifiedSource source;
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/DerivedClass.CPLC.java")) {
            source = new IdentifiedSource(new ClassIdentifier("postprocessor.DerivedClass"), is.readAllBytes());
        }

        var cplcResult = cplc.compileClass(provider, Optional.of(new NullMessagesListener()), source);

        for (var bytecode : cplcResult) {
            var name = bytecode.getClassIdentifier().getFullName();
            name = name.substring(name.lastIndexOf('.') + 1);

            try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/postprocessor/" + name + ".class")) {
                assertArrayEquals(is.readAllBytes(), bytecode.getFile());
            }
        }
    }
}
