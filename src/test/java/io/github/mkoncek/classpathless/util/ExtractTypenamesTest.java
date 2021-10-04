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
package io.github.mkoncek.classpathless.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;
import io.github.mkoncek.classpathless.util.extract.Dummy;
import io.github.mkoncek.classpathless.util.extract.DummyAnnotation;
import io.github.mkoncek.classpathless.util.extract.DummyException;
import io.github.mkoncek.classpathless.util.extract.DummyNested;

public class ExtractTypenamesTest {
    private static final String DUMMY_NAME = Dummy.class.getName();
    private static final String DUMMY_ANNOTATION_NAME = DummyAnnotation.class.getName();

    @Test
    void testUnused() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/Unused.class")) {
            assertFalse(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testUsed() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/Used.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testNew() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/New.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testNewArray() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/NewArray.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testNewMultiArray() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/NewMultiArray.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testReturnType() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ReturnType.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testArgumentType() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ArgumentType.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testClassField() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ClassField.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testClassFieldArray() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ClassFieldArray.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testClassFieldMultiArray() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ClassFieldMultiArray.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testLambdaArgument() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/LambdaArgumentType.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testLambdaReturnType() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/LambdaReturnType.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testLambdaReturnTypeNested() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/LambdaReturnTypeNested.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testTryCatch() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/TryCatch.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DummyException.class.getName()));
        }
    }

    @Test
    void testNested() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/DummyNested.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DummyNested.Inner.class.getName()));
        }
    }

    @Test
    void testExtend() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/Extend.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_NAME));
        }
    }

    @Test
    void testExtendNested() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ExtendNested.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DummyNested.Inner.class.getName()));
        }
    }

    @Test
    void ExtendNestedInner() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ExtendNestedInner$Inner.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DummyNested.Inner.class.getName()));
        }
    }

    @Test
    void testAnonymous() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/Anonymous.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains("io.github.mkoncek.classpathless.util.extract.Anonymous$1"));
        }
    }

    @Test
    void testAnnotationClass() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationClass.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    void testAnnotationMethod() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationMethod.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    void testAnnotationField() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationField.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    void testAnnotationReturnType() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationReturnType.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    void testAnnotationAnnotation() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationAnnotation.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    void testAnnotationConstructor() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationConstructor.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    void testAnnotationArgument() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationArgument.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    @Disabled(value = "Supposedly annotating local variables does not work, see JSR 308")
    void testAnnotationLocalVariable() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationLocalVariable.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    @Test
    @Disabled(value = "The annotation is not present in the compiled file")
    void testAnnotationTryCatch() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/AnnotationTryCatch.class")) {
            assertTrue(ExtractTypenames.extract(is.readAllBytes()).contains(DUMMY_ANNOTATION_NAME));
        }
    }

    private static class FSProvider implements ClassesProvider {
        @Override
        public Collection<IdentifiedBytecode> getClass(
                ClassIdentifier... names) {
            var result = new ArrayList<IdentifiedBytecode>();
            for (var name : names) {
                var filename = "target/test-classes/" + name.getFullName().replace('.', '/') + ".class";
                try (var is = new FileInputStream(filename)) {
                    result.add(new IdentifiedBytecode(name, is.readAllBytes()));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            return result;
        }

        @Override
        public List<String> getClassPathListing() {
            return null;
        }
    }

    @Test
    void testExtractNestedEmpty() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/Used.class")) {
            assertTrue(ExtractTypenames.extractNested(is.readAllBytes(), new FSProvider()).isEmpty());
        }
    }

    @Test
    void testExtractNested() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/ExtendNestedInner.class")) {
            var result = ExtractTypenames.extractNested(is.readAllBytes(), new FSProvider());
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.ExtendNestedInner$Inner"));
            assertTrue(result.size() == 1);
        }
    }

    @Test
    void testExtractMultiNested() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/MultiNested.class")) {
            var result = ExtractTypenames.extractNested(is.readAllBytes(), new FSProvider());
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.MultiNested$Nested1"));
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.MultiNested$Nested1$Nested11"));
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.MultiNested$Nested1$Nested11$Nested111"));
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.MultiNested$Nested1$Nested11$Nested112"));
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.MultiNested$Nested1$Nested12"));
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.MultiNested$Nested1$Nested12$Nested121"));
            assertTrue(result.contains("io.github.mkoncek.classpathless.util.extract.MultiNested$Nested1$Nested12$Nested121$Nested1211"));
            assertTrue(result.size() == 7);
        }
    }

    @Test
    void testExtractNestedAnonymous() throws IOException {
        try (var is = new FileInputStream("target/test-classes/io/github/mkoncek/classpathless/util/extract/Anonymous.class")) {
            assertTrue(ExtractTypenames.extractNested(is.readAllBytes(), new FSProvider()).contains(
                    "io.github.mkoncek.classpathless.util.extract.Anonymous$1"));
        }
    }
}
