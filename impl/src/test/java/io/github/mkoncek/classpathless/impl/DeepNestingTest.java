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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.helpers.PrintingMessagesListener;
import io.github.mkoncek.classpathless.helpers.SimpleClassesProvider;

public class DeepNestingTest {
    private static final SystemJavac javac = new SystemJavac();

    @Test
    void testDeepNesting() throws IOException {
        final String prefix = "src/test/resources/io/github/mkoncek/classpathless/impl/deepnesting/";

        var classFiles = new String[] {
                prefix + "A.class",
                prefix + "A$AA.class",
                prefix + "A$AA$AAA.class",
                prefix + "A$AA$AAA$AAAA.class",
                prefix + "B.class",
                prefix + "B$BB.class",
                prefix + "B$BB$BBB.class",
                prefix + "B$BB$BBB$BBBB.class",
        };

        for (var file : classFiles) {
            Files.deleteIfExists(Paths.get(file));
        }

        javac.compile(prefix + "A.java", prefix + "B.java");

        var classes = new ArrayList<IdentifiedBytecode>();

        for (String name : new String[] {"B", "B$BB", "B$BB$BBB", "B$BB$BBB$BBBB"}) {
            try (var is = new FileInputStream(prefix + name + ".class")) {
                classes.add(new IdentifiedBytecode(new ClassIdentifier(
                        "io.github.mkoncek.classpathless.impl.deepnesting." + name), is.readAllBytes()));
            }
        }

        IdentifiedSource source;
        try (var is = new FileInputStream(prefix + "A.java")) {
            source = new IdentifiedSource(new ClassIdentifier("io.github.mkoncek.classpathless.impl.deepnesting.A"), is.readAllBytes());
        }

        var cplc = new CompilerJavac(new CompilerJavac.Arguments().useHostSystemClasses(true));
        var cplcResult = cplc.compileClass(new SimpleClassesProvider(classes), Optional.of(new PrintingMessagesListener()), source);

        for (var bytecode : cplcResult) {
            var name = bytecode.getClassIdentifier().getFullName();
            name = name.substring(name.lastIndexOf('.') + 1);

            try (var is = new FileInputStream(prefix + name + ".class")) {
                assertArrayEquals(is.readAllBytes(), bytecode.getFile());
            }
        }
    }
}
