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
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.mkoncek.classpathless.api.MessagesListener;

public class LoggingSwitch {
    PrintStream printer;
    MessagesListener listener;
    private boolean tracing = false;
    private java.util.logging.Level logLevel = Level.OFF;

    public static class Null extends LoggingSwitch {
        public Null() {
            super(new PrintStream(PrintStream.nullOutputStream(), false, StandardCharsets.UTF_8));
        }
    }

    public LoggingSwitch(PrintStream printer) {
        this.printer = printer;
    }

    public LoggingSwitch() {
        var logging = System.getProperty("io.github.mkoncek.cplc.logging");
        if (logging == null) {
            printer = new PrintStream(PrintStream.nullOutputStream(), false, StandardCharsets.UTF_8);
        } else {
            if (logging.isEmpty()) {
                printer = System.err;
            } else {
                // DO NOT USE try-with-resources, we do not want to close the stream
                FileOutputStream os;
                try {
                    // TODO not closed if any exception happens
                    os = new FileOutputStream(Paths.get(logging).toFile(), true);
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
        this.listener = listener;
    }

    public void setTracing(boolean value) {
        this.tracing = value;
    }

    public void setLogLevel(java.util.logging.Level value) {
        this.logLevel = value;
    }

    public void trace(Object struct, String name, Object... args) {
        if (tracing) {
            logln(true, Level.FINEST, "[TRACE] invoking {0}::{1}({2})", struct.getClass().getName(), name,
                    Stream.of(args).map(arg -> arg == null ? "<null>" : arg.toString())
                    .collect(Collectors.joining(", ")));
        }
    }

    public void trace(Object result) {
        if (tracing) {
            logln(true, Level.FINEST, "[TRACE] returning {0}", result == null ? "<null>" : result.toString());
        }
    }

    public void log(boolean traced, java.util.logging.Level level, String format, Object... args) {
        // TODO contact JRD
        /*
        if (!traced && listener != null) {
            listener.addMessage(level, format, args);
        }
         */

        // TODO delete this line, this is here just to silence spotbugs
        {
            var ls = listener;
            listener = null;
        }

        if (logLevel != Level.OFF && level.intValue() >= logLevel.intValue()) {
            var message = "[CPLC.LOG] " + MessageFormat.format(format, args);
            printer.print(message);
        }
    }

    public void logln(boolean traced, java.util.logging.Level level, String format, Object... args) {
        log(traced, level, format + System.lineSeparator(), args);
    }

    public void logln(java.util.logging.Level level, String format, Object... args) {
        logln(false, level, format, args);
    }
}
