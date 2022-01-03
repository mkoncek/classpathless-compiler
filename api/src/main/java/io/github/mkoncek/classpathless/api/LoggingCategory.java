/*-
 * Copyright (c) 2022 Marián Konček
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

public enum LoggingCategory {
    /**
     * Generic messages like: "starting compilation task of...",
     * "compilation result..."
     */
    INFO,

    /**
     * Messages from bytecode extraction procedure split into phases
     */
    BYTECODE_EXTRACTION_DETAILED,

    /**
     * Summary of classes found during the bytecode extraction phase
     */
    BYTECODE_EXTRACTION_SUMMARY,

    /**
     * Very extensive listing of ignored lambda / array types which are ignored
     * in classpath listing
     */
    IGNORING_NON_TYPES,

    /**
     * Listing of all available class names found after the bytedcode extraction
     * procedure
     */
    AVAILABLE_TYPE_NAMES,

    /**
     * Very extensive listing of classes (one by one) exposed to the compiler
     * from available class listing during the compilation phase
     */
    EXPOSING_CLASS_FROM_PROVIDER,

    /**
     * Very extensive listing of classes (on by one) not exposed because they
     * belong to subpackages during the compilation phase
     */
    SKIPPING_SUBPACKAGE,

    /**
     * Messages about skipping file objects that do not look like the
     * standard .class file objects (e.g. .dat files present inside system
     * modules)
     */
    SKIPPING_NON_CLASS_OBJECT,

    /**
     * Messages reporting when the actual bytecode was requested by the compiler
     * and provided by the ClassesProvider
     */
    LOADING_BYTECODE,

    /**
     * Messages categorized as internal compiler errors, i.e. errors encountered
     * by the compiler implementation
     */
    COMPILER_CRITICAL,

    /**
     * Error diagnostics reported by the compiler.
     */
    COMPILER_DIAGNOSTICS,

    /**
     * Warning messages reported by the compiler.
     */
    COMPILER_DIAGNOSTICS_WARNING,

    /**
     * Note messages reported by the compiler.
     */
    COMPILER_DIAGNOSTICS_NOTE,
}
