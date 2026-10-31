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

/**
 * Thrown when the user deliberately aborts the interactive configuration wizard.
 *
 * <p>This exception is raised by {@link FilterMenu#pick(java.util.List)} when the user types
 * {@code q} or {@code quit}, or when no configuration classes are available.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class UserAbortException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code UserAbortException} with the default abort message.
     */
    public UserAbortException() {
        super("User aborted the configuration wizard");
    }

    /**
     * Constructs a {@code UserAbortException} with the given detail message.
     *
     * @param message the detail message explaining why the wizard was aborted
     */
    public UserAbortException(final String message) {
        super(message);
    }
}
