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

package com.viveworks.appbase.service.user.session.store;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.service.user.session.data.UserSession;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Natively non-blocking, read-only reactive twin of {@link UserSessionStore}. Mirrors the blocking
 * store's read surface 1:1; a missing row completes empty ({@link Mono#empty()}), never emits
 * {@code null}. Create/update/delete stay blocking-only — this is a read-only twin.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ReactiveUserSessionStore extends AppbaseService {
    Mono<Boolean> existsSessionReactive(String appbaseUserId, String appbaseSessionId);

    Mono<Boolean> existsUnauthenticatedSessionReactive(String appbaseSessionId);

    Mono<Boolean> existsSessionsReactive(String appbaseUserId);

    /**
     * @return {@link Mono} of the {@link UserSession}, or empty if absent
     */
    Mono<UserSession> readSessionReactive(String appbaseUserId, String appbaseSessionId);

    /**
     * @return {@link Mono} of the {@link UserSession}, or empty if absent
     */
    Mono<UserSession> readUnauthenticatedSessionReactive(String appbaseSessionId);

    /**
     * @return {@link Mono} of the {@link UserSession}s for {@code appbaseUserId}, or empty if none
     */
    Mono<Collection<UserSession>> readAllSessionsReactive(String appbaseUserId);
}
