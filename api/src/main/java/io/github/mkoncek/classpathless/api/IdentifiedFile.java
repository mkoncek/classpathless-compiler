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

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class IdentifiedFile {
    private final ClassIdentifier classIdentifier;
    private final byte[] file;

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "pure wrapper class")
    public IdentifiedFile(ClassIdentifier classIdentifier, byte[] file) {
        this.classIdentifier = classIdentifier;
        this.file = file;
    }

    public ClassIdentifier getClassIdentifier() {
        return classIdentifier;
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "pure wrapper class")
    public byte[] getFile() {
        return file;
    }

    @Override
    public String toString() {
        return classIdentifier + "*" + file.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentifiedFile that = (IdentifiedFile) o;
        return Objects.equals(classIdentifier, that.classIdentifier);
    }

    @Override
    public int hashCode() {
        return classIdentifier.hashCode();
    }
}
