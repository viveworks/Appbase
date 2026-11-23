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

import com.viveworks.appbase.configwizard.ConsoleColors;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * JLine3 readline helpers for per-field prompting.
 *
 * <p>The caller is responsible for constructing the full prompt string (including any
 * {@code [default]} annotation) before calling a {@code read*} method. Each method:
 * <ol>
 *   <li>Displays the supplied prompt via the terminal's {@link LineReader}</li>
 *   <li>Returns the default when the user enters blank input</li>
 *   <li>Coerces the input to the target type; re-prompts on parse error</li>
 * </ol>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class PromptUtil {

    private final LineReader reader;
    private final PrintWriter out;
    private final Terminal terminal;

    /**
     * Constructs a {@code PromptUtil} that creates a {@link LineReader} from the given terminal.
     *
     * @param terminal the JLine3 terminal for I/O; caller owns lifecycle
     */
    public PromptUtil(final Terminal terminal) {
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder().terminal(terminal).build();
        LineReaderInterrupts.bindCtrlC(reader);
        this.out = terminal.writer();
    }

    /**
     * Prompts for a string value; blank input returns {@code defaultValue} (or {@code null}).
     *
     * @param prompt       full prompt string to display
     * @param defaultValue default to return on blank input; {@code null} means no default
     * @return the entered string, or {@code defaultValue} when input is blank
     * @throws IOException on terminal I/O failure
     */
    @Nullable
    public String readString(final String prompt, @Nullable final String defaultValue)
            throws IOException {
        String line = readLine(prompt);
        if (line.isEmpty()) {
            return defaultValue;
        }
        return line;
    }

    /**
     * Prompts for an integer value; blank input returns {@code defaultValue}.
     *
     * <p>Re-prompts on non-integer input.
     *
     * @param prompt       full prompt string to display
     * @param defaultValue default integer returned on blank input
     * @return the entered or default integer value
     * @throws IOException on terminal I/O failure
     */
    public int readInt(final String prompt, final int defaultValue) throws IOException {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                out.println(ConsoleColors.red("  (must be an integer — try again)", terminal));
                out.flush();
            }
        }
    }

    /**
     * Prompts for a boolean value; blank input returns {@code defaultValue}.
     *
     * <p>Accepts true/false/yes/no/y/n (case-insensitive). Re-prompts on unrecognised input.
     *
     * @param prompt       full prompt string to display
     * @param defaultValue default boolean returned on blank input
     * @return the entered or default boolean value
     * @throws IOException on terminal I/O failure
     */
    public boolean readBoolean(final String prompt, final boolean defaultValue) throws IOException {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return defaultValue;
            }
            switch (line.toLowerCase()) {
                case "true", "yes", "y" -> { return true; }
                case "false", "no", "n" -> { return false; }
                default -> {
                    out.println(ConsoleColors.red("  (enter true/false, yes/no, or y/n)", terminal));
                    out.flush();
                }
            }
        }
    }

    /**
     * Prompts for an enum constant by showing a numbered list of choices.
     *
     * <p>The user can enter the choice number (1-based) or the constant name
     * (case-insensitive). Re-prompts on invalid input.
     *
     * @param prompt       full prompt string to display before the choice list
     * @param type         the enum class whose constants are offered as choices
     * @param defaultValue default constant returned on blank input; {@code null} if no default
     * @param <E>          the enum type
     * @return the selected enum constant (never {@code null})
     * @throws IOException on terminal I/O failure
     */
    public <E extends Enum<E>> E readEnum(final String prompt, final Class<E> type,
            @Nullable final E defaultValue) throws IOException {
        E[] constants = type.getEnumConstants();
        out.println(ConsoleColors.cyan(prompt + " choices:", terminal));
        for (int i = 0; i < constants.length; i++) {
            String marker = constants[i].equals(defaultValue)
                    ? " " + ConsoleColors.green("✓", terminal) : "";
            out.println("    " + (i + 1) + ". " + ConsoleColors.bold(constants[i].name(), terminal) + marker);
        }
        out.flush();

        String defaultName = defaultValue != null ? defaultValue.name() : null;
        String readPrompt = prompt + (defaultName != null ? " [" + defaultName + "]" : "") + ": ";
        while (true) {
            String line = readLine(readPrompt);
            if (line.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                int num = Integer.parseInt(line);
                if (num >= 1 && num <= constants.length) {
                    return constants[num - 1];
                }
            } catch (NumberFormatException ignored) {
                // fall through to name match
            }
            for (E constant : constants) {
                if (constant.name().equalsIgnoreCase(line)) {
                    return constant;
                }
            }
            out.println(ConsoleColors.red("  (invalid — enter a number 1-" + constants.length + " or a valid name)", terminal));
            out.flush();
        }
    }

    /**
     * Writes a message line to the terminal output.
     *
     * @param message the message to print followed by a newline
     */
    void println(final String message) {
        out.println(message);
        out.flush();
    }

    /**
     * Reads a line from the terminal; maps JLine3 interrupts to {@link IOException}.
     *
     * @param prompt the prompt string to display before reading
     * @return trimmed input line (never null; empty string on EOF or interrupt)
     * @throws IOException when the terminal raises an I/O error
     */
    private String readLine(final String prompt) throws IOException {
        try {
            String line = LineReaderInterrupts.readLine(reader, terminal, prompt);
            return line == null ? "" : line.trim();
        } catch (UserInterruptException | EndOfFileException e) {
            throw new IOException("Terminal input interrupted", e);
        }
    }
}
