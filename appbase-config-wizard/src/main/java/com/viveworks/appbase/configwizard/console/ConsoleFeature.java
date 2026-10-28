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

import org.jline.terminal.Terminal;

import java.nio.file.Path;

/**
 * Contract for a pluggable console feature.
 *
 * <p>Each feature appears as a numbered entry in the main menu. When the user
 * selects the entry the feature's {@link #run} method is invoked with the active
 * terminal and the session-level output directory.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ConsoleFeature {

    /**
     * Short label shown in the main menu (e.g. {@code "Configurations"}).
     *
     * @return the human-readable menu label; never {@code null}
     */
    String menuLabel();

    /**
     * Entry point for the feature. Called when the user selects this feature from the menu.
     *
     * <p>The feature may show its own sub-menu and loop until the user chooses to go back.
     * The terminal's lifecycle is owned by the caller; do not close it inside this method.
     *
     * @param terminal         the JLine3 terminal for I/O; caller owns lifecycle
     * @param sessionOutputDir the directory (or file path) to which YAML files should be
     *                         written by default during this session
     * @throws Exception on terminal I/O error, YAML write failure, or any other error
     */
    void run(Terminal terminal, Path sessionOutputDir) throws Exception;
}
