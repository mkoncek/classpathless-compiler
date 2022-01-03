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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.mkoncek.classpathless.api.MessagesListener;
import io.github.mkoncek.classpathless.api.MessagesListener.Category;

public class LoggingSwitch implements AutoCloseable {
    private PrintStream printer;
    private MessagesListener listener;
    private boolean tracing = false;
    private java.util.logging.Level logLevel = Level.OFF;

    @Override
    public void close() throws Exception {
        printer.flush();
        if (printer != System.err) {
            printer.close();
        }
    }

    private static PrintStream newNullPrintStream() {
        return new PrintStream(PrintStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    }

    public static class Null extends LoggingSwitch {
        public Null() {
            super(newNullPrintStream());
        }
    }

    private LoggingSwitch(PrintStream printer) {
        this.printer = printer;
    }

    public LoggingSwitch() {
        var logging = System.getProperty("io.github.mkoncek.cplc.logging");
        if (logging == null) {
            printer = newNullPrintStream();
        } else {
            if (logging.isEmpty()) {
                printer = System.err;
            } else {
                // DO NOT USE try-with-resources, we do not want to close the stream
                try {
                    var os = new FileOutputStream(Paths.get(logging).toFile(), true);
                    printer = new PrintStream(os, true, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }

        var level = Level.OFF;
        var loglevel = System.getProperty("io.github.mkoncek.cplc.loglevel");
        if (loglevel != null) {
            boolean validValue = false;

            for (var innerLevel : new Level[] {
                    Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG,
                    Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF}) {
                if (innerLevel.toString().equalsIgnoreCase(loglevel)) {
                    level = innerLevel;
                    validValue = true;
                    break;
                }
            }

            if (!validValue) {
                throw new IllegalArgumentException("Unrecognized logging level: \"" + loglevel + "\"");
            }
        }

        setLogLevel(level);

        if (System.getProperty("io.github.mkoncek.cplc.tracing") != null) {
            setTracing(true);
        }
    }

    public void setMessagesListener(MessagesListener listener) {
        if (System.getProperty("io.github.mkoncek.cplc.log-to-provider") != null) {
            this.listener = listener;
        }
    }

    public void setTracing(boolean value) {
        this.tracing = value;
    }

    public void setLogLevel(java.util.logging.Level value) {
        this.logLevel = value;
    }

    private static String joinArgs(Object... args) {
        return Stream.of(args).map(arg -> arg == null ? "<null>" : arg.toString())
                .collect(Collectors.joining(", "));
    }

    public void trace(Object struct, String name, Object... args) {
        if (tracing) {
            logln(true, Category.INFO, "[TRACE] invoking {0}::{1}({2})",
                    struct.getClass().getName(), name, joinArgs(args));
        }
    }

    public void traceThis(Object struct, String self, String name, Object... args) {
        if (tracing) {
            logln(true, Category.INFO, "[TRACE] invoking {0}::{1}({2}) [this = {3}]",
                    struct.getClass().getName(), name, joinArgs(args), self);
        }
    }

    public void trace(Object result) {
        if (tracing) {
            logln(true, Category.INFO, "[TRACE] returning {0}", result == null ? "<null>" : result.toString());
        }
    }

    private void log(boolean traced, Category category, String format, Object... args) {
        if (!traced && listener != null) {
            listener.addMessage(category, format, args);
        }

        /*
         * OFF
         *
        if (logLevel != Level.OFF && level.intValue() >= logLevel.intValue()) {
            var message = "[CPLC.LOG] " + MessageFormat.format(format, args);
            printer.print(message);
        }
         */
    }

    private void logln(boolean traced, Category category, String format, Object... args) {
        log(traced, category, format + System.lineSeparator(), args);
    }

    public void logln(Category category, String format, Object... args) {
        logln(false, category, format, args);
    }
}
