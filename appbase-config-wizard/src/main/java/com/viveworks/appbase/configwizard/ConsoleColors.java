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

package com.viveworks.appbase.configwizard;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * ANSI colour helpers for console output.
 *
 * <p>All methods delegate to {@link AttributedStringBuilder#toAnsi(Terminal)} which
 * automatically strips escape codes when the terminal is not colour-capable (e.g. dumb
 * terminals, test fakes). Callers never need to guard for non-ANSI environments.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ConsoleColors {

    private static final String BANNER_LINE =
            "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    private ConsoleColors() { }

    /**
     * Returns the text styled cyan and bold.
     *
     * @param text text to style
     * @param t    terminal used to resolve ANSI capability
     * @return ANSI-escaped string (or plain text on dumb terminals)
     */
    public static String cyan(final String text, final Terminal t) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold())
                .append(text)
                .style(AttributedStyle.DEFAULT)
                .toAnsi(t);
    }

    /**
     * Returns the text styled yellow.
     *
     * @param text text to style
     * @param t    terminal used to resolve ANSI capability
     * @return ANSI-escaped string (or plain text on dumb terminals)
     */
    public static String yellow(final String text, final Terminal t) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                .append(text)
                .style(AttributedStyle.DEFAULT)
                .toAnsi(t);
    }

    /**
     * Returns the text styled green and bold.
     *
     * @param text text to style
     * @param t    terminal used to resolve ANSI capability
     * @return ANSI-escaped string (or plain text on dumb terminals)
     */
    public static String green(final String text, final Terminal t) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold())
                .append(text)
                .style(AttributedStyle.DEFAULT)
                .toAnsi(t);
    }

    /**
     * Returns the text styled red.
     *
     * @param text text to style
     * @param t    terminal used to resolve ANSI capability
     * @return ANSI-escaped string (or plain text on dumb terminals)
     */
    public static String red(final String text, final Terminal t) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                .append(text)
                .style(AttributedStyle.DEFAULT)
                .toAnsi(t);
    }

    /**
     * Returns the text styled bold.
     *
     * @param text text to style
     * @param t    terminal used to resolve ANSI capability
     * @return ANSI-escaped string (or plain text on dumb terminals)
     */
    public static String bold(final String text, final Terminal t) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.BOLD)
                .append(text)
                .style(AttributedStyle.DEFAULT)
                .toAnsi(t);
    }

    /**
     * Prints a two-line banner to the terminal writer.
     *
     * <pre>
     *   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     *     Appbase Config Wizard
     *   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     * </pre>
     *
     * @param t terminal used to resolve ANSI capability and to write to
     */
    public static void printBanner(final Terminal t) {
        java.io.PrintWriter w = t.writer();
        w.println(cyan(BANNER_LINE, t));
        w.println(cyan("    Appbase Config Wizard", t));
        w.println(cyan(BANNER_LINE, t));
        w.println();
        w.flush();
    }
}
