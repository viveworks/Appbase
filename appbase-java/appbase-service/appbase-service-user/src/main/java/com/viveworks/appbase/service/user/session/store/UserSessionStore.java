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
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.session.data.UserSession;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface UserSessionStore extends AppbaseService {
    LocalDateTime currentTime();

    String generateSessionId();

    boolean isExpired(UserSession userSession);

    boolean isNotExpired(UserSession userSession);


    void createSession(UserSession userSession) throws UserSessionStoreException;

    void updateSession(UserSession userSession) throws UserSessionStoreException;

    void createOrUpdateSession(UserSession userSession) throws UserSessionStoreException;


    boolean existsSession(String appbaseUserId, String appbaseSessionId) throws UserSessionStoreException;

    boolean existsUnauthenticatedSession(String appbaseSessionId) throws UserSessionStoreException;

    boolean existsSessions(String appbaseUserId) throws UserSessionStoreException;


    Optional<UserSession> readUnauthenticatedSession(String appbaseSessionId) throws UserSessionStoreException;

    Optional<UserSession> readSession(String appbaseUserId, String appbaseSessionId) throws UserSessionStoreException;

    Optional<Collection<UserSession>> readAllSessions(String appbaseUserId) throws UserSessionStoreException;


    boolean deleteSession(String appbaseUserId, String appbaseSessionId) throws UserSessionStoreException;

    boolean deleteUnauthenticatedSession(String appbaseSessionId) throws UserSessionStoreException;

    boolean deleteAllSessions(String appbaseUserId) throws UserSessionStoreException;
}
