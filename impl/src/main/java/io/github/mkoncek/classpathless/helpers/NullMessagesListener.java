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

import io.github.mkoncek.classpathless.api.LoggingCategory;
import io.github.mkoncek.classpathless.api.MessagesListener;

public class NullMessagesListener implements MessagesListener {
    private NullMessagesListener() {
    }

    public static final MessagesListener INSTANCE = new NullMessagesListener();

    @Override
    public void addMessage(LoggingCategory category, String message) {
    }

    @Override
    public void addMessage(LoggingCategory category, String format, Object... args) {
    }
}
