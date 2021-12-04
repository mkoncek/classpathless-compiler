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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.helpers.NullMessagesListener;
import io.github.mkoncek.classpathless.helpers.SimpleClassesProvider;

public class TransitiveImportTest {
    @Test
    void testTransitiveImport() throws IOException {
        var classes = new HashSet<IdentifiedBytecode>();

        for (var name : new String[] {
                "target/test-classes/io/github/mkoncek/classpathless/impl/imported/$A$.class",
                "target/test-classes/io/github/mkoncek/classpathless/impl/imported/$A$$$B$.class",
                "target/test-classes/io/github/mkoncek/classpathless/impl/imported/$A$$$B$$$C$.class",
                "target/test-classes/io/github/mkoncek/classpathless/impl/files/NestedImporter.class",
                "target/test-classes/io/github/mkoncek/classpathless/impl/files/NestedImporter$Nested.class",
                "target/test-classes/io/github/mkoncek/classpathless/impl/files/NestedImporter$Nested$Nested2.class",
        }) {
            try (var fis = new FileInputStream(name)) {
                var ci = new ClassIdentifier(name.substring("target/test-classes/".length(), name.length() - 6).replace('/', '.'));
                classes.add(new IdentifiedBytecode(ci, fis.readAllBytes()));
            }
        }
        var cp = new SimpleClassesProvider(classes);
        var cplc = new CompilerJavac();
        try (var fis = new FileInputStream("src/test/java/io/github/mkoncek/classpathless/impl/files/NestedImporter.java")) {
            var result = cplc.compileClass(cp, Optional.of(NullMessagesListener.INSTANCE),
                    new IdentifiedSource(new ClassIdentifier("io.github.mkoncek.classpathless.impl.files.NestedImporter"), fis.readAllBytes()));
            assertEquals(3, result.size());
        }
    }
}
