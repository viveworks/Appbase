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
 * Signals that a caller-supplied path resolved outside the permitted root.
 *
 * <p>⚠️ The message is returned to HTTP callers. It MUST NOT name the configured root, the
 * requested path, or whether the target exists — an unauthenticated caller learns nothing about
 * the filesystem from a refusal.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class PathOutsideRootException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Constructs the exception with the fixed, disclosure-free message. */
    public PathOutsideRootException() {
        super("The requested path is outside the permitted root.");
    }
}
