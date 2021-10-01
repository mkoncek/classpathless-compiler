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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;
import java.util.logging.Level;

import io.github.mkoncek.classpathless.api.ClassesProvider;
import io.github.mkoncek.classpathless.impl.InMemoryJavaClassFileObject;
import io.github.mkoncek.classpathless.impl.LoggingSwitch;

public class LoadClasses {
    public static Collection<InMemoryJavaClassFileObject> loadClasses(SortedSet<String> availableClasses,
            String packageName, boolean recurse, ClassesProvider classprovider,
            LoggingSwitch loggingSwitch) throws IOException {
        var result = new ArrayList<InMemoryJavaClassFileObject>();

        for (var availableClassName : availableClasses.tailSet(packageName)) {
            if (!availableClassName.startsWith(packageName)) {
                break;
            }

            if (availableClassName.length() > packageName.length() + 1) {
                if (availableClassName.substring(packageName.length() + 1).contains(".") && !recurse) {
                    loggingSwitch.logln(Level.FINE, "Skipping over class from a subpackage from ClassProvider: \"{0}\"", availableClassName);
                    continue;
                }
            }

            loggingSwitch.logln(Level.FINE, "Loading class from ClassProvider: \"{0}\"", availableClassName);

            result.add(new InMemoryJavaClassFileObject(availableClassName, classprovider));
        }

        return result;
    }
}
