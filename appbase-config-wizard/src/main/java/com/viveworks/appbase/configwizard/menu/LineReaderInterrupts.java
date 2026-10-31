/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.configwizard.menu;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;

/**
 * Applies fail-closed interrupt bindings to JLine readers.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class LineReaderInterrupts {

    private static final String INTERRUPT_WIDGET = "appbase-interrupt";

    private LineReaderInterrupts() {}

    /**
     * Registers an Appbase widget that throws {@link UserInterruptException}, then binds
     * keyboard Ctrl-C to it in every configured keymap.
     *
     * @param reader reader whose keymaps receive the binding
     */
    static void bindCtrlC(final LineReader reader) {
        reader.getWidgets().put(INTERRUPT_WIDGET, () -> {
            throw new UserInterruptException(reader.getBuffer().toString());
        });
        Reference interrupt = new Reference(INTERRUPT_WIDGET);
        String ctrlC = KeyMap.ctrl('C');
        for (KeyMap<Binding> keyMap : reader.getKeyMaps().values()) {
            if (keyMap != null) {
                keyMap.bind(interrupt, ctrlC);
            }
        }
    }

    /**
     * Reads a line while allowing JLine to receive the Ctrl-C byte.
     *
     * <p>The terminal driver normally converts its VINTR byte into a signal before JLine's
     * keymap can observe it. This method disables {@code ISIG} only for the synchronous
     * read and restores an immutable snapshot of the preceding attributes afterward.
     *
     * @param reader reader that performs the line edit
     * @param terminal terminal whose attributes are scoped
     * @param prompt prompt shown by JLine
     * @return the line returned by JLine
     */
    static String readLine(final LineReader reader, final Terminal terminal,
            final String prompt) {
        Attributes restoreAttributes = new Attributes(terminal.getAttributes());
        Attributes readAttributes = new Attributes(restoreAttributes);
        readAttributes.setLocalFlag(Attributes.LocalFlag.ISIG, false);
        try {
            terminal.setAttributes(readAttributes);
            return reader.readLine(prompt);
        } finally {
            terminal.setAttributes(restoreAttributes);
        }
    }
}
