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

package com.viveworks.appbase.core.service.registry;

import java.util.Collection;
import java.util.Optional;

/**
 * Evaluates if the app is ready operationally.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ReadinessChecker {
    /**
     * Checks if the app is ready to start operating.
     *
     * @param appbaseServiceEntries All appbase services registered with {@link ServiceRegistry}.
     * @return Unready appbase service entries or nothing if all ready, optionally
     */
    Optional<UnreadyAppbaseServiceEntries> checkReady(Collection<AppbaseServiceEntry> appbaseServiceEntries);
}
