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
package io.github.mkoncek.classpathless.impl.postprocessors;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import io.github.mkoncek.classpathless.api.CompilationError;
import io.github.mkoncek.classpathless.api.IdentifiedSource;
import io.github.mkoncek.classpathless.impl.SourcePostprocessorImpl;

public class AddAbstract extends SourcePostprocessorImpl {
    @Override
    public Result postprocess(IdentifiedSource source,
            Collection<CompilationError> compilationErrors) {
        var result = new Result(source, false);

        for (var ce : sortBackwards(compilationErrors)) {
            if (ce.errorCode.equals("compiler.err.does.not.override.abstract")) {
                var modifiedSource = makeAbstract(result.source.getSourceCode(), ce.lineNum, ce.columnNum);
                result = new Result(new IdentifiedSource(result.source.getClassIdentifier(),
                        modifiedSource.getBytes(StandardCharsets.UTF_8)), true);
            }
        }

        return result;
    }
}
