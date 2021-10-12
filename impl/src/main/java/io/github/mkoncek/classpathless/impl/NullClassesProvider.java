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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import io.github.mkoncek.classpathless.api.ClassIdentifier;
import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.api.IdentifiedBytecode;

public class NullClassesProvider implements ClassesProvider {
    private Map<String, IdentifiedBytecode> nameToBytecode;

    public NullClassesProvider(Map<String, IdentifiedBytecode> initialMapping) {
        this.nameToBytecode = initialMapping;

        try {
            var fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, StandardCharsets.UTF_8);

            Location baseLocation = null;

            for (var set : fm.listLocationsForModules(StandardLocation.SYSTEM_MODULES)) {
                for (var loc : set) {
                    if (loc.getName().equals("SYSTEM_MODULES[java.base]")) {
                        baseLocation = loc;
                        break;
                    }
                }
            }

            if (baseLocation == null) {
                throw new IllegalStateException();
            }

            /// Ad-hoc imports necessary to pass tests
            for (var importName : Arrays.asList("java.lang", "java.io", "java.lang.invoke", "java.lang.annotation")) {
                for (var entry : fm.list(baseLocation, importName, Set.of(Kind.SOURCE, Kind.CLASS, Kind.HTML, Kind.OTHER), false)) {
                    var name = fm.inferBinaryName(baseLocation, entry);
                    byte[] content;
                    try (var is = entry.openInputStream()) {
                        content = is.readAllBytes();
                    }
                    nameToBytecode.put(name, new IdentifiedBytecode(new ClassIdentifier(name), content));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public NullClassesProvider() {
        this(new TreeMap<>());
    }

    @Override
    public List<String> getClassPathListing() {
        return new ArrayList<>(nameToBytecode.keySet());
    }

    @Override
    public Collection<IdentifiedBytecode> getClass(ClassIdentifier... names) {
        var result = new ArrayList<IdentifiedBytecode>();

        for (var name : names) {
            var bytecode = nameToBytecode.get(name.getFullName());
            if (bytecode != null) {
                if (bytecode.getClassIdentifier().equals(name)) {
                    result.add(bytecode);
                }
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        var fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, StandardCharsets.UTF_8);

        // for (var loc : fm.list(StandardLocation.SYSTEM_MODULES, "", Set.of(Kind.SOURCE, Kind.CLASS, Kind.HTML, Kind.OTHER), true)) {
        for (var set : fm.listLocationsForModules(StandardLocation.SYSTEM_MODULES)) {
            for (var loc : set) {
                if (loc.getName().equals("SYSTEM_MODULES[java.base]")) {
                    int size = 0;
                    var objs = fm.list(StandardLocation.CLASS_PATH, "", Set.of(Kind.CLASS), true);
                    for (var obj : objs) {
                        System.out.println(obj);
                        ++size;
                    }
                    System.out.println(size);
                }

                // System.out.println(loc.getName());
                var jf = fm.getJavaFileForInput(loc, "module-info", Kind.CLASS);
                try (var is = jf.openInputStream()) {
                    // System.out.println(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
    }
}
