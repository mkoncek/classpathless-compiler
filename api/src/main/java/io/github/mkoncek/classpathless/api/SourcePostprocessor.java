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

public interface SourcePostprocessor {
    static final class Result {
        public final IdentifiedSource source;
        public final boolean changed;

        public Result(IdentifiedSource source, boolean changed) {
            this.source = source;
            this.changed = changed;
        }
    }

    Result postprocess(IdentifiedSource source, Collection<CompilationError> compilationErrors);

    static class Null implements SourcePostprocessor {
        @Override
        public Result postprocess(IdentifiedSource source, Collection<CompilationError> compilationErrors) {
            return new Result(source, false);
        }
    }
}
