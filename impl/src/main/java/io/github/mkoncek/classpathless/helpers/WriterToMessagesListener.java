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

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;

import io.github.mkoncek.classpathless.api.MessagesListener;

/**
 * This is used in exceptional situations like runtime exceptions thrown
 * from the compiler.
 */
public class WriterToMessagesListener extends Writer {
    private MessagesListener listener;

    public WriterToMessagesListener(MessagesListener listener) {
        this.listener = listener;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        var message = new String(cbuf, off, len);
        if (message.endsWith(System.lineSeparator())) {
            message = message.substring(0, message.length() - System.lineSeparator().length());
        }
        if (!message.isBlank()) {
            listener.addMessage(Level.SEVERE, message);
        }
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}