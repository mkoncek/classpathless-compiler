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
package io.github.mkoncek.classpathless.helpers;

import java.util.Locale;
import java.util.logging.Level;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mkoncek.classpathless.api.MessagesListener;

public class DiagnosticToMessagesListener implements DiagnosticListener<JavaFileObject> {
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "listener is safe to share")
    private MessagesListener listener;

    public DiagnosticToMessagesListener(MessagesListener listener) {
        this.listener = listener;
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        var msg = diagnostic.getMessage(Locale.ENGLISH);
        var source = diagnostic.getSource();

        if (listener != null) {
            var severity = Level.SEVERE;
            var errCode = diagnostic.getCode();
            if (errCode != null) {
                if (errCode.startsWith("compiler.warn")) {
                    severity = Level.WARNING;
                } else if (errCode.startsWith("compiler.note")) {
                    severity = Level.WARNING;
                }
            }

            listener.addMessage(severity, "Compiler diagnostic at {5}[{0}, {1}]: {2}{3}(code: {4})",
                    diagnostic.getLineNumber(), diagnostic.getColumnNumber(), msg,
                    System.lineSeparator(), errCode,
                    (source != null ? "(" + source.getName() + ") " : " "));
        }
    }
}