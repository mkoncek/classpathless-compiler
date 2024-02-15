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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClasspathlessCompiler.Arguments;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.api.MessagesListener;
import io.github.mkoncek.classpathless.helpers.NullClassesProvider;
import io.github.mkoncek.classpathless.helpers.PrintingMessagesListener;

public class CompilerJavacTest {
    private static final SystemJavac javac = new SystemJavac();
    private static final Optional<MessagesListener> printingListener = Optional.of(new PrintingMessagesListener());
    private static final Comparator<byte[]> byteArrayComparator = (byte[] lhs, byte[] rhs) -> Arrays.compare(lhs, rhs);

    private static void setProperties() {
        // System.setProperty("io.github.mkoncek.cplc.logging", "");
        // System.setProperty("io.github.mkoncek.cplc.loglevel", "finest");
        // System.setProperty("io.github.mkoncek.cplc.tracing", "");
    }

    @Test
    public void testSimple() throws Exception {
        setProperties();
        var expectedBytecodeContents = new TreeSet<byte[]>(byteArrayComparator);
        var classes = new String[] {
                "src/test/resources/io/github/mkoncek/classpathless/impl/simple-class/Hello.class",
        };
        for (var cf : classes) {
            Files.deleteIfExists(Paths.get(cf));
        }
        javac.compile("src/test/resources/io/github/mkoncek/classpathless/impl/simple-class/Hello.java");
        for (var cf : classes) {
            try (var is = new FileInputStream(cf)) {
                expectedBytecodeContents.add(is.readAllBytes());
            }
        }
        byte[] content;
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/simple-class/Hello.java")) {
            content = is.readAllBytes();
        }
        var jc = new CompilerJavac();
        var source = new IdentifiedSource(new ClassIdentifier("Hello"), content);
        var compilationResult = jc.compileClass(new NullClassesProvider(), printingListener, source);
        assertEquals(expectedBytecodeContents.size(), compilationResult.size());
        var compiledBytecodes = new TreeSet<>(byteArrayComparator);
        for (var ib : compilationResult) {
            compiledBytecodes.add(ib.getFile());
        }
        var actit = compiledBytecodes.iterator();
        var expit = expectedBytecodeContents.iterator();
        while (expit.hasNext()) {
            assertArrayEquals(expit.next(), actit.next());
        }
        assertEquals(false, actit.hasNext());
    }

    @Test
    public void testNested() throws Exception {
        setProperties();
        var expectedBytecodeContents = new TreeSet<byte[]>(byteArrayComparator);
        var classes = new String[] {
                "src/test/resources/io/github/mkoncek/classpathless/impl/nested-class/Hello.class",
                "src/test/resources/io/github/mkoncek/classpathless/impl/nested-class/Hello$Inner.class",
        };
        for (var cf : classes) {
            Files.deleteIfExists(Paths.get(cf));
        }
        javac.compile("src/test/resources/io/github/mkoncek/classpathless/impl/nested-class/Hello.java");
        for (var cf : classes) {
            try (var is = new FileInputStream(cf)) {
                expectedBytecodeContents.add(is.readAllBytes());
            }
        }
        byte[] content;
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/nested-class/Hello.java")) {
            content = is.readAllBytes();
        }
        var jc = new CompilerJavac();
        var source = new IdentifiedSource(new ClassIdentifier("Hello"), content);
        var compilationResult = jc.compileClass(new NullClassesProvider(), printingListener, source);
        assertEquals(expectedBytecodeContents.size(), compilationResult.size());
        var compiledBytecodes = new TreeSet<>(byteArrayComparator);
        for (var ib : compilationResult) {
            compiledBytecodes.add(ib.getFile());
        }
        var actit = compiledBytecodes.iterator();
        var expit = expectedBytecodeContents.iterator();
        while (expit.hasNext()) {
            assertArrayEquals(expit.next(), actit.next());
        }
        assertEquals(false, actit.hasNext());
    }

    @Test
    public void testAnonymous() throws Exception {
        setProperties();
        var expectedBytecodeContents = new TreeSet<byte[]>(byteArrayComparator);
        var classes = new String[] {
                "src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello.class",
                "src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello$1.class",
                "src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello$InnerInterface.class",
        };
        for (var cf : classes) {
            Files.deleteIfExists(Paths.get(cf));
        }
        javac.compile("src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello.java");
        for (var cf : classes) {
            try (var is = new FileInputStream(cf)) {
                expectedBytecodeContents.add(is.readAllBytes());
            }
        }
        byte[] content;
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello.java")) {
            content = is.readAllBytes();
        }
        var jc = new CompilerJavac();
        var source = new IdentifiedSource(new ClassIdentifier("Hello"), content);
        var compilationResult = jc.compileClass(new NullClassesProvider(), printingListener, source);
        assertEquals(expectedBytecodeContents.size(), compilationResult.size());
        var compiledBytecodes = new TreeSet<>(byteArrayComparator);
        for (var ib : compilationResult) {
            compiledBytecodes.add(ib.getFile());
        }
        var actit = compiledBytecodes.iterator();
        var expit = expectedBytecodeContents.iterator();
        while (expit.hasNext()) {
            assertArrayEquals(expit.next(), actit.next());
        }
        assertEquals(false, actit.hasNext());
    }

    @Test
    public void testRepeated() throws Exception {
        setProperties();
        var jc = new CompilerJavac();
        for (int i = 0; i != 3; ++i) {
            var expectedBytecodeContents = new TreeSet<byte[]>(byteArrayComparator);
            var classes = new String[] {
                    "src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello.class",
                    "src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello$1.class",
                    "src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello$InnerInterface.class",
            };
            for (var cf : classes) {
                Files.deleteIfExists(Paths.get(cf));
            }
            javac.compile("src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello.java");
            for (var cf : classes) {
                try (var is = new FileInputStream(cf)) {
                    expectedBytecodeContents.add(is.readAllBytes());
                }
            }
            byte[] content;
            try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/anonymous-inner-class/Hello.java")) {
                content = is.readAllBytes();
            }
            var source = new IdentifiedSource(new ClassIdentifier("Hello"), content);
            var compilationResult = jc.compileClass(new NullClassesProvider(), printingListener, source);
            assertEquals(expectedBytecodeContents.size(), compilationResult.size());
            var compiledBytecodes = new TreeSet<>(byteArrayComparator);
            for (var ib : compilationResult) {
                compiledBytecodes.add(ib.getFile());
            }
            var actit = compiledBytecodes.iterator();
            var expit = expectedBytecodeContents.iterator();
            while (expit.hasNext()) {
                assertArrayEquals(expit.next(), actit.next());
            }
            assertEquals(false, actit.hasNext());
        }
    }

    @Test
    public void testJavaLangModule() throws Exception {
        setProperties();
        var expectedBytecodeContents = new TreeSet<byte[]>(byteArrayComparator);
        var classes = new String[] {
                "src/test/resources/io/github/mkoncek/classpathless/impl/modules/Runnable.class",
        };
        for (var cf : classes) {
            Files.deleteIfExists(Paths.get(cf));
        }
        javac.compile(List.of("--patch-module", "java.base=."),
                "src/test/resources/io/github/mkoncek/classpathless/impl/modules/Runnable.java");
        for (var cf : classes) {
            try (var is = new FileInputStream(cf)) {
                expectedBytecodeContents.add(is.readAllBytes());
            }
        }
        byte[] content;
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/modules/Runnable.java")) {
            content = is.readAllBytes();
        }
        var jc = new CompilerJavac(new Arguments().patchModules(Map.of("java.lang", "java.base")));
        var source = new IdentifiedSource(new ClassIdentifier("java.lang.Runnable"), content);
        var compilationResult = jc.compileClass(new NullClassesProvider(), printingListener, source);
        assertEquals(expectedBytecodeContents.size(), compilationResult.size());
        var compiledBytecodes = new TreeSet<>(byteArrayComparator);
        for (var ib : compilationResult) {
            compiledBytecodes.add(ib.getFile());
        }
        var actit = compiledBytecodes.iterator();
        var expit = expectedBytecodeContents.iterator();
        while (expit.hasNext()) {
            assertArrayEquals(expit.next(), actit.next());
        }
        assertEquals(false, actit.hasNext());
    }

    @Test
    public void testOtherJavaModule() throws Exception {
        setProperties();
        var expectedBytecodeContents = new TreeSet<byte[]>(byteArrayComparator);
        var classes = new String[] {
                "src/test/resources/io/github/mkoncek/classpathless/impl/modules/Filter.class",
        };
        for (var cf : classes) {
            Files.deleteIfExists(Paths.get(cf));
        }
        javac.compile(List.of("--patch-module", "java.logging=."),
                "src/test/resources/io/github/mkoncek/classpathless/impl/modules/Filter.java");
        for (var cf : classes) {
            try (var is = new FileInputStream(cf)) {
                expectedBytecodeContents.add(is.readAllBytes());
            }
        }
        byte[] content;
        try (var is = new FileInputStream("src/test/resources/io/github/mkoncek/classpathless/impl/modules/Filter.java")) {
            content = is.readAllBytes();
        }
        var jc = new CompilerJavac(new Arguments().patchModules(Map.of("java.util.logging", "java.logging")));
        var source = new IdentifiedSource(new ClassIdentifier("java.util.logging.Filter"), content);
        var compilationResult = jc.compileClass(new NullClassesProvider(), printingListener, source);
        assertEquals(expectedBytecodeContents.size(), compilationResult.size());
        var compiledBytecodes = new TreeSet<>(byteArrayComparator);
        for (var ib : compilationResult) {
            compiledBytecodes.add(ib.getFile());
        }
        var actit = compiledBytecodes.iterator();
        var expit = expectedBytecodeContents.iterator();
        while (expit.hasNext()) {
            assertArrayEquals(expit.next(), actit.next());
        }
        assertEquals(false, actit.hasNext());
    }
}
