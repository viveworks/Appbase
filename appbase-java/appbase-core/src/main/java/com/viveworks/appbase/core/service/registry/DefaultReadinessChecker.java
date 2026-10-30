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

package com.viveworks.appbase.core.service.registry;

import com.viveworks.appbase.core.service.ServiceStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link ReadinessChecker}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultReadinessChecker implements ReadinessChecker {
    @Override
    public Optional<UnreadyAppbaseServiceEntries> checkReady(
            final Collection<AppbaseServiceEntry> appbaseServiceEntries)
    {
        final List<AppbaseServiceEntry> unreadyServiceEntries = new ArrayList<>();

        for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServiceEntries) {
            if (!appbaseServiceEntry.getServiceStatus().equals(ServiceStatus.STARTED)) {
                unreadyServiceEntries.add(appbaseServiceEntry);
            }
        }

        if (unreadyServiceEntries.isEmpty()) {
            return Optional.empty();
        }

        final UnreadyAppbaseServiceEntries unreadyAppbaseServiceEntries =
                new UnreadyAppbaseServiceEntries(
                        Collections.unmodifiableCollection(unreadyServiceEntries));
        return Optional.of(unreadyAppbaseServiceEntries);
    }
}
