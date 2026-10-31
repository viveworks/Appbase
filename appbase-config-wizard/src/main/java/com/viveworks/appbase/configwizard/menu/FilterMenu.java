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

import com.viveworks.appbase.configwizard.ConsoleColors;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigClassInfo;
import org.jline.keymap.KeyMap;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp;

import java.io.PrintWriter;
import java.util.List;

/**
 * JLine3 filter-and-pick menu for selecting a config class.
 *
 * <p>Presents a numbered list of caller-supplied configuration classes.
 * The user can type a filter string to narrow the list; the list re-prints
 * with matching entries only. Tab-completion is available on module or class
 * name fragments. Entering a number selects that entry.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class FilterMenu {

    private final Terminal terminal;
    private final PrintWriter out;

    /**
     * Constructs a {@code FilterMenu} backed by the given terminal.
     *
     * @param terminal the JLine3 terminal for I/O; caller owns lifecycle
     */
    public FilterMenu(final Terminal terminal) {
        this.terminal = terminal;
        this.out = terminal.writer();
    }

    /**
     * Displays the filterable list and blocks until the user makes a selection.
     *
     * <p>Interaction:
     * <ul>
     *   <li>Blank input — resets the filter and shows all entries</li>
     *   <li>Text fragment — filters by class name, module, or FQCN; tab-completes</li>
     *   <li>Number — selects the entry with that display index</li>
     *   <li>{@code q} / {@code quit} — throws {@link UserAbortException}</li>
     * </ul>
     *
     * @param configs the pre-fetched list of available configuration classes
     * @return the selected {@link ConfigClassInfo}
     * @throws UserAbortException when the user types {@code q}/{@code quit} or the list is empty
     */
    @SuppressWarnings("unchecked")
    public ConfigClassInfo pick(final List<ConfigClassInfo> configs) throws UserAbortException {
        if (configs.isEmpty()) {
            out.println(ConsoleColors.red("No configuration classes found on classpath.", terminal));
            out.flush();
            throw new UserAbortException("No configurations available");
        }

        // Mutable cursor state shared with arrow-key widgets
        int[] cursor = {0};
        List<ConfigClassInfo>[] filteredRef = new List[]{configs};

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        LineReaderInterrupts.bindCtrlC(reader);

        // Widget that re-renders the list with the cursor highlight above the readline prompt
        Runnable redraw = () -> {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            for (int i = 0; i < filteredRef[0].size(); i++) {
                ConfigClassInfo c = filteredRef[0].get(i);
                if (i == cursor[0]) {
                    sb.append(ConsoleColors.cyan("  → " + (i + 1) + ". ", terminal))
                      .append(ConsoleColors.yellow("[" + c.module() + "]", terminal))
                      .append(" ")
                      .append(ConsoleColors.bold(c.simpleName(), terminal));
                } else {
                    sb.append("    " + (i + 1) + ". ")
                      .append("[" + c.module() + "] ")
                      .append(c.simpleName());
                }
                sb.append("\n");
            }
            reader.printAbove(sb.toAnsi(terminal));
        };

        reader.getWidgets().put("nav-up", () -> {
            if (cursor[0] > 0) {
                cursor[0]--;
                redraw.run();
            }
            return true;
        });
        reader.getWidgets().put("nav-down", () -> {
            if (cursor[0] < filteredRef[0].size() - 1) {
                cursor[0]++;
                redraw.run();
            }
            return true;
        });

        try {
            reader.getKeyMaps().get(LineReader.MAIN).bind(
                    new Reference("nav-up"),
                    KeyMap.key(terminal, InfoCmp.Capability.key_up));
            reader.getKeyMaps().get(LineReader.MAIN).bind(
                    new Reference("nav-down"),
                    KeyMap.key(terminal, InfoCmp.Capability.key_down));
        } catch (Exception ignored) {
            // Key binding failed (e.g. dumb terminal) — arrow nav silently unavailable
        }

        while (true) {
            out.println();
            out.println(ConsoleColors.cyan("  Available configurations (" + filteredRef[0].size() + " shown):", terminal));
            printList(filteredRef[0]);
            String input;
            try {
                input = LineReaderInterrupts.readLine(reader, terminal,
                        "Filter [Enter=select highlighted, number=pick, q=quit]: ");
            } catch (UserInterruptException | EndOfFileException e) {
                throw new UserAbortException("User interrupted selection");
            }
            if (input == null) input = "";
            input = input.trim();

            if (input.isEmpty()) {
                // Select the cursor item
                return filteredRef[0].get(cursor[0]);
            }
            if ("q".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                throw new UserAbortException("User quit the selection menu");
            }

            try {
                int num = Integer.parseInt(input);
                if (num >= 1 && num <= filteredRef[0].size()) {
                    return filteredRef[0].get(num - 1);
                }
                out.println(ConsoleColors.red("  (number out of range — enter 1-" + filteredRef[0].size() + ")", terminal));
                out.flush();
            } catch (NumberFormatException ignored) {
                List<ConfigClassInfo> narrowed = filterByQuery(configs, input);
                filteredRef[0] = narrowed;
                cursor[0] = 0;
            }
        }
    }

    /**
     * Filters the full list to entries whose simple name, module, or FQCN contain the query.
     *
     * <p>The comparison is case-insensitive. When no entries match, the full list is
     * returned unchanged and a message is printed to the terminal.
     *
     * @param all   the complete unfiltered list
     * @param query the substring to match against
     * @return filtered sublist, or {@code all} when nothing matches
     */
    private List<ConfigClassInfo> filterByQuery(final List<ConfigClassInfo> all, final String query) {
        String lower = query.toLowerCase();
        List<ConfigClassInfo> narrowed = all.stream()
                .filter(c -> c.simpleName().toLowerCase().contains(lower)
                        || c.module().toLowerCase().contains(lower)
                        || c.fqcn().toLowerCase().contains(lower))
                .toList();
        if (narrowed.isEmpty()) {
            out.println(ConsoleColors.red("  (no matches for '" + query + "' — showing all)", terminal));
            out.flush();
            return all;
        }
        return narrowed;
    }

    /**
     * Prints the current filtered list of configuration classes to the terminal.
     *
     * @param items the current list to display (may be a filtered subset)
     */
    private void printList(final List<ConfigClassInfo> items) {
        out.println();
        out.println(ConsoleColors.cyan("  Available configurations (" + items.size() + " shown):", terminal));
        for (int i = 0; i < items.size(); i++) {
            ConfigClassInfo c = items.get(i);
            String line = String.format("  %3d. ", i + 1)
                    + ConsoleColors.yellow("[" + c.module() + "]", terminal)
                    + " "
                    + ConsoleColors.bold(c.simpleName(), terminal);
            out.println(line);
        }
        out.println();
        out.flush();
    }
}
