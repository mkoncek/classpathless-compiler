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
package io.github.mkoncek.classpathless.api;

import java.util.Collection;
import java.util.List;

public interface ClassesProvider {
    /**
     * Callback for compiler, which provides, on demand, the dependencies the compiler is missing.
     *
     * @param names Names of classes the provider should return
     * @return The bytecode of all found classes
     */
    Collection<IdentifiedBytecode> getClass(ClassIdentifier... names);

    /**
     * Warning: may include lambdas and will include inner classes with $notations.
     * Intentionally not using ClassIdentifier, but may change to it
     *
     * @deprecated This function should be replaced by more intelligent class
     * file scanning and requesting only needed classes.
     * @return all fully qualified classes visible from classpath
     */
    @Deprecated
    List<String> getClassPathListing();
}
