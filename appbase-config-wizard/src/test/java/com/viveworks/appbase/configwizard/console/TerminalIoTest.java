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

package com.viveworks.appbase.configwizard.console;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for scoped raw-mode terminal reads.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class TerminalIoTest {

    private Terminal terminal;
    private NonBlockingReader reader;
    private Attributes savedAttributes;

    @BeforeEach
    void setUp() {
        terminal = mock(Terminal.class);
        reader = mock(NonBlockingReader.class);
        savedAttributes = mock(Attributes.class);
        when(terminal.enterRawMode()).thenReturn(savedAttributes);
        when(terminal.reader()).thenReturn(reader);
        when(terminal.writer()).thenReturn(mock(PrintWriter.class));
    }

    @Test
    void readChar_normalByte_restoresAttributes() throws Exception {
        when(reader.read()).thenReturn((int) '1');

        assertEquals((int) '1', TerminalIo.readChar(terminal));

        verify(terminal).setAttributes(savedAttributes);
    }

    @Test
    void readChar_eof_restoresAttributes() throws Exception {
        when(reader.read()).thenReturn(-1);

        assertEquals(-1, TerminalIo.readChar(terminal));

        verify(terminal).setAttributes(savedAttributes);
    }

    @Test
    void readChar_readFailure_restoresAttributes() throws Exception {
        IOException failure = new IOException("read failed");
        when(reader.read()).thenThrow(failure);

        assertEquals(failure,
                assertThrows(IOException.class, () -> TerminalIo.readChar(terminal)));
        verify(terminal).setAttributes(savedAttributes);
    }

    @Test
    void readChar_ctrlC_restoresAttributesAndInterrupts() throws Exception {
        when(reader.read()).thenReturn(3);

        assertThrows(InterruptedIOException.class, () -> TerminalIo.readChar(terminal));

        verify(terminal).setAttributes(savedAttributes);
    }

    @Test
    void readLine_normalInput_restoresAttributes() throws Exception {
        when(reader.read()).thenReturn((int) ' ', (int) 'a', (int) '\r');

        assertEquals("a", TerminalIo.readLine(terminal));

        verify(terminal).setAttributes(savedAttributes);
    }

    @Test
    void readLine_eof_restoresAttributes() throws Exception {
        when(reader.read()).thenReturn(-1);

        assertEquals("", TerminalIo.readLine(terminal));

        verify(terminal).setAttributes(savedAttributes);
    }

    @Test
    void readLine_readFailure_restoresAttributes() throws Exception {
        IOException failure = new IOException("read failed");
        when(reader.read()).thenThrow(failure);

        assertEquals(failure,
                assertThrows(IOException.class, () -> TerminalIo.readLine(terminal)));
        verify(terminal).setAttributes(savedAttributes);
    }

    @Test
    void readLine_ctrlC_restoresAttributesAndInterrupts() throws Exception {
        when(reader.read()).thenReturn(3);

        assertThrows(InterruptedIOException.class, () -> TerminalIo.readLine(terminal));

        verify(terminal).setAttributes(savedAttributes);
    }
}
