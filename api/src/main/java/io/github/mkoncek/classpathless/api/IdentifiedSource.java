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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Java source file with its fully qualified name.
 */
@SuppressFBWarnings(value = {"EQ_DOESNT_OVERRIDE_EQUALS"},
    justification = "intentionally using equals from superclass")
public class IdentifiedSource extends IdentifiedFile {
    private final Charset charset;

    /**
     * @param classIdentifier fully qualified name of class.
     * @param file java source code of class.
     * @param charset Character set of source which was used to get the byte[].
     * Default encoding is used if not provided.
     */
    public IdentifiedSource(ClassIdentifier classIdentifier, byte[] file, Charset charset) {
        super(classIdentifier, file);
        this.charset = charset;
    }

    public IdentifiedSource(ClassIdentifier classIdentifier, byte[] file) {
        this(classIdentifier, file, StandardCharsets.UTF_8);
    }

    public String getSourceCode() {
        return new String(getFile(), charset);
    }
}
