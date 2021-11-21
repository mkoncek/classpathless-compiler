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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import org.junit.jupiter.api.Test;

import io.github.mkoncek.classpathless.api.ClasspathlessCompiler;

public class InMemoryFileManagerTest {
    private static InMemoryFileManager initFM(Collection<String> classNames) {
        var fm = new InMemoryFileManager(null);
        fm.setArguments(new ClasspathlessCompiler.Arguments());
        fm.setAvailableClasses(new TreeSet<String>(classNames));
        return fm;
    }

    @Test
    void testListNamedPackage() throws IOException {
        var fm = initFM(Arrays.asList("a$a", "a.a", "a.a.a", "a.b$a", "a.b.a", "a.b.b", "a.ba", "aa"));

        {
            var it = fm.list(StandardLocation.CLASS_PATH, "a", Set.of(Kind.CLASS), false).iterator();
            assertEquals("/a.a", it.next().getName());
            assertEquals("/a.b$a", it.next().getName());
            assertEquals("/a.ba", it.next().getName());
            assertFalse(it.hasNext());
        }
        {
            var it = fm.list(StandardLocation.CLASS_PATH, "a", Set.of(Kind.CLASS), true).iterator();
            assertEquals("/a.a", it.next().getName());
            assertEquals("/a.a.a", it.next().getName());
            assertEquals("/a.b$a", it.next().getName());
            assertEquals("/a.b.a", it.next().getName());
            assertEquals("/a.b.b", it.next().getName());
            assertEquals("/a.ba", it.next().getName());
            assertFalse(it.hasNext());
        }

        {
            var it = fm.list(StandardLocation.CLASS_PATH, "a.b", Set.of(Kind.CLASS), false).iterator();
            assertEquals("/a.b.a", it.next().getName());
            assertEquals("/a.b.b", it.next().getName());
            assertFalse(it.hasNext());
        }
        {
            var it = fm.list(StandardLocation.CLASS_PATH, "a.b", Set.of(Kind.CLASS), true).iterator();
            assertEquals("/a.b.a", it.next().getName());
            assertEquals("/a.b.b", it.next().getName());
            assertFalse(it.hasNext());
        }
    }

    @Test
    void testListUnnamedPackage() throws IOException {
        var fm = initFM(Arrays.asList("a", "a$a", "aa.a", "aa.a$a", "b", "c.a", "d"));

        {
            var it = fm.list(StandardLocation.CLASS_PATH, "", Set.of(Kind.CLASS), false).iterator();
            assertEquals("/a", it.next().getName());
            assertEquals("/a$a", it.next().getName());
            assertEquals("/b", it.next().getName());
            assertEquals("/d", it.next().getName());
            assertFalse(it.hasNext());
        }
        {
            var it = fm.list(StandardLocation.CLASS_PATH, "", Set.of(Kind.CLASS), true).iterator();
            assertEquals("/a", it.next().getName());
            assertEquals("/a$a", it.next().getName());
            assertEquals("/aa.a", it.next().getName());
            assertEquals("/aa.a$a", it.next().getName());
            assertEquals("/b", it.next().getName());
            assertEquals("/c.a", it.next().getName());
            assertEquals("/d", it.next().getName());
            assertFalse(it.hasNext());
        }
    }
}
