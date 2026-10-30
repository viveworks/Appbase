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

package com.viveworks.appbase.client.service.firebase.auth.accessor;

import com.google.firebase.auth.FirebaseAuth;

/**
 * A checked-exception-throwing command over a {@link FirebaseAuth} client, used by {@link
 * FirebaseAuthAccessor#command(String, FirebaseAuthCommand)}. Unlike {@link
 * java.util.function.Function}, its {@link #apply(FirebaseAuth)} declares {@code throws
 * Exception}, so callers can invoke the Admin SDK's blocking sync API (whose terminal calls throw
 * checked {@code FirebaseAuthException}) directly inside the lambda. Mirrors {@code
 * ZooKeeperCommand} in {@code appbase-client-zookeeper}.
 *
 * @param <T> Result type
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@FunctionalInterface
public interface FirebaseAuthCommand<T> {

    /**
     * Run the command against {@code auth}.
     *
     * @param auth Firebase Auth client
     * @return Result of the command
     * @throws Exception If the Firebase Auth call fails
     */
    T apply(FirebaseAuth auth) throws Exception;
}
