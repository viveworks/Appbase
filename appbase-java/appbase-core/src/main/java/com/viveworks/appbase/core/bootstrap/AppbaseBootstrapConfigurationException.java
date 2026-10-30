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

package com.viveworks.appbase.core.bootstrap;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Thrown when an explicitly selected Appbase configuration file cannot be loaded or decoded.
 * The core refuses to start instead of silently falling back to built-in defaults
 * (fail closed, Lens 3 finding L3-03 / CWE-636).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_EQUALS")
public class AppbaseBootstrapConfigurationException extends AppbaseRuntimeException {
    private static final long serialVersionUID = 1760046100000000003L;

    public AppbaseBootstrapConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public AppbaseBootstrapConfigurationException(final String message) {
        super(message);
    }
}
