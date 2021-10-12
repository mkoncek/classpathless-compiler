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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

public class APIBytecodeVersionTest {
    private static short getBytecodeVersion(InputStream is) throws IOException {
        is.skip(6);
        byte[] bytecode = new byte[2];
        is.read(bytecode);
        short version = 0;
        version += bytecode[0] << 8;
        version += bytecode[1];
        return version;
    }

    @Test
    public void testBytecodeVersion() throws Exception {
        for (File file : new File("target/classes/io/github/mkoncek/classpathless/api/").listFiles()) {
            try (InputStream is = new FileInputStream(file)) {
                assertEquals(getBytecodeVersion(is), 52, "The bytecode version of API is not equivalent to Java 8");
            }
        }
    }
}
