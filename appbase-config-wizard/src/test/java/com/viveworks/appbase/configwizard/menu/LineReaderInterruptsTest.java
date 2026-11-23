/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
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
import org.jline.reader.Buffer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for explicit keyboard-interrupt bindings.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class LineReaderInterruptsTest {

    @Test
    void bindCtrlC_appliesAppbaseInterruptToEveryKeyMap() {
        LineReader reader = mock(LineReader.class);
        Map<String, KeyMap<Binding>> keyMaps = new LinkedHashMap<>();
        Map<String, Widget> widgets = new LinkedHashMap<>();
        keyMaps.put(LineReader.MAIN, new KeyMap<>());
        keyMaps.put(LineReader.VIINS, new KeyMap<>());
        when(reader.getKeyMaps()).thenReturn(keyMaps);
        when(reader.getWidgets()).thenReturn(widgets);

        LineReaderInterrupts.bindCtrlC(reader);

        for (KeyMap<Binding> keyMap : keyMaps.values()) {
            Reference binding = assertInstanceOf(
                    Reference.class, keyMap.getBound(KeyMap.ctrl('C')));
            assertEquals("appbase-interrupt", binding.name());
        }
    }

    @Test
    void bindCtrlC_widgetThrowsUserInterruptWithPartialLine() {
        LineReader reader = mock(LineReader.class);
        Buffer buffer = mock(Buffer.class);
        Map<String, KeyMap<Binding>> keyMaps = new LinkedHashMap<>();
        Map<String, Widget> widgets = new LinkedHashMap<>();
        keyMaps.put(LineReader.MAIN, new KeyMap<>());
        when(reader.getKeyMaps()).thenReturn(keyMaps);
        when(reader.getWidgets()).thenReturn(widgets);
        when(reader.getBuffer()).thenReturn(buffer);
        when(buffer.toString()).thenReturn("partial input");
        LineReaderInterrupts.bindCtrlC(reader);

        UserInterruptException failure = assertThrows(UserInterruptException.class,
                () -> widgets.get("appbase-interrupt").apply());

        assertEquals("partial input", failure.getPartialLine());
    }

    @Test
    void readLine_disablesIsigDuringReadAndRestoresIt() {
        LineReader reader = mock(LineReader.class);
        Terminal terminal = mock(Terminal.class);
        AtomicReference<Attributes> current = terminalAttributes(true);
        when(terminal.getAttributes()).thenReturn(current.get());
        doAnswer(invocation -> {
            current.set(new Attributes(invocation.getArgument(0)));
            return null;
        }).when(terminal).setAttributes(any(Attributes.class));
        when(reader.readLine("prompt")).thenAnswer(invocation -> {
            assertFalse(current.get().getLocalFlag(Attributes.LocalFlag.ISIG));
            return "value";
        });

        assertEquals("value", LineReaderInterrupts.readLine(reader, terminal, "prompt"));

        assertTrue(current.get().getLocalFlag(Attributes.LocalFlag.ISIG));
        assertTrue(current.get().getLocalFlag(Attributes.LocalFlag.ECHO));
    }

    @Test
    void readLine_restoresIsigAfterUserInterrupt() {
        assertRestoredAfter(new UserInterruptException("partial"));
    }

    @Test
    void readLine_restoresIsigAfterEndOfFile() {
        assertRestoredAfter(new EndOfFileException());
    }

    @Test
    void readLine_restoresIsigAfterUnexpectedFailure() {
        assertRestoredAfter(new IllegalStateException("failed"));
    }

    @Test
    void readLine_restoresSnapshotWhenApplyingReadAttributesFails() {
        LineReader reader = mock(LineReader.class);
        Terminal terminal = mock(Terminal.class);
        AtomicReference<Attributes> current = terminalAttributes(true);
        AtomicInteger calls = new AtomicInteger();
        when(terminal.getAttributes()).thenReturn(current.get());
        doAnswer(invocation -> {
            current.set(new Attributes(invocation.getArgument(0)));
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("apply failed");
            }
            return null;
        }).when(terminal).setAttributes(any(Attributes.class));

        assertThrows(IllegalStateException.class,
                () -> LineReaderInterrupts.readLine(reader, terminal, "prompt"));

        assertEquals(2, calls.get());
        assertTrue(current.get().getLocalFlag(Attributes.LocalFlag.ISIG));
        assertTrue(current.get().getLocalFlag(Attributes.LocalFlag.ECHO));
    }

    private static void assertRestoredAfter(final RuntimeException failure) {
        LineReader reader = mock(LineReader.class);
        Terminal terminal = mock(Terminal.class);
        AtomicReference<Attributes> current = terminalAttributes(true);
        when(terminal.getAttributes()).thenReturn(current.get());
        doAnswer(invocation -> {
            current.set(new Attributes(invocation.getArgument(0)));
            return null;
        }).when(terminal).setAttributes(any(Attributes.class));
        when(reader.readLine("prompt")).thenAnswer(invocation -> {
            assertFalse(current.get().getLocalFlag(Attributes.LocalFlag.ISIG));
            throw failure;
        });

        assertEquals(failure, assertThrows(failure.getClass(),
                () -> LineReaderInterrupts.readLine(reader, terminal, "prompt")));
        assertTrue(current.get().getLocalFlag(Attributes.LocalFlag.ISIG));
        assertTrue(current.get().getLocalFlag(Attributes.LocalFlag.ECHO));
    }

    private static AtomicReference<Attributes> terminalAttributes(final boolean isig) {
        Attributes attributes = new Attributes();
        attributes.setLocalFlag(Attributes.LocalFlag.ISIG, isig);
        attributes.setLocalFlag(Attributes.LocalFlag.ECHO, true);
        return new AtomicReference<>(attributes);
    }
}
