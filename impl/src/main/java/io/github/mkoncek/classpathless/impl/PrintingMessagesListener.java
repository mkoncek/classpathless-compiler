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

import java.io.PrintStream;
import java.util.logging.Level;

import io.github.mkoncek.classpathless.api.MessagesListener;

public class PrintingMessagesListener implements MessagesListener {
    private PrintStream printer;

    public PrintingMessagesListener(PrintStream printer) {
        this.printer = printer;
    }

    public PrintingMessagesListener() {
        this(System.err);
    }

    @Override
    public void addMessage(Level level, String message) {
        printer.println(message);
    }
}
