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

package com.viveworks.appbase.configwizard.console;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Shared terminal I/O helpers used across console commands and features.
 *
 * <p>Centralises low-level character-by-character line reading so that
 * {@link ConfigWizardConsoleApp} collaborators do not each carry duplicate implementations.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class TerminalIo {

    private static final int END_OF_TEXT = 3;

    private TerminalIo() {}

    /**
     * Reads one byte without requiring Enter, then restores the terminal mode.
     *
     * <p>Raw mode is deliberately scoped to this read. Keeping the terminal raw while a
     * JLine {@code LineReader} is active prevents its Ctrl-C binding from receiving the
     * interrupt. A literal ETX received during this raw read is therefore mapped to an
     * interrupted I/O operation.
     *
     * @param terminal the JLine3 terminal
     * @return the byte read, or {@code -1} on EOF
     * @throws IOException on terminal read failure or Ctrl-C
     */
    public static int readChar(final Terminal terminal) throws IOException {
        final Attributes savedAttributes = terminal.enterRawMode();
        try {
            int ch = terminal.reader().read();
            if (ch == END_OF_TEXT) {
                throw new InterruptedIOException("Terminal input interrupted");
            }
            terminal.writer().println();
            terminal.writer().flush();
            return ch;
        } finally {
            terminal.setAttributes(savedAttributes);
        }
    }

    /**
     * Reads a single line from the terminal, echoing each character.
     *
     * <p>Supports backspace (ASCII 8 and DEL 127). Terminates on newline, CR, or EOF.
     *
     * @param terminal the JLine3 terminal
     * @return the typed line with leading/trailing whitespace stripped
     * @throws IOException on terminal read failure
     */
    public static String readLine(final Terminal terminal) throws IOException {
        final Attributes savedAttributes = terminal.enterRawMode();
        try {
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = terminal.reader().read()) != -1) {
                if (ch == END_OF_TEXT) {
                    throw new InterruptedIOException("Terminal input interrupted");
                }
                if (ch == '\n' || ch == '\r') {
                    break;
                }
                if ((ch == 8 || ch == 127) && sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                    terminal.writer().print("\b \b");
                } else if (ch >= 32) {
                    sb.append((char) ch);
                    terminal.writer().print((char) ch);
                }
                terminal.writer().flush();
            }
            terminal.writer().println();
            terminal.writer().flush();
            return sb.toString().strip();
        } finally {
            terminal.setAttributes(savedAttributes);
        }
    }
}
