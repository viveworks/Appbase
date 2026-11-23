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

package com.viveworks.appbase.configwizard.security;

/**
 * Signals that an import scan exceeded one of {@link ImportScanLimits}' bounds — entry count,
 * per-file bytes, or aggregate bytes (Lens 2 / L2-11, CWE-400).
 *
 * <p>The message is returned directly to HTTP callers. It MUST NOT echo the scanned directory,
 * file names, counts, or sizes — only the fixed, disclosure-free text below.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ImportScanTooLargeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Constructs the exception with the fixed, disclosure-free message. */
    public ImportScanTooLargeException() {
        super("Import scan refused: the directory exceeds the import resource limits.");
    }
}
