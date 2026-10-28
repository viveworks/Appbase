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

package com.viveworks.appbase.client.service.vitess.accessor.reactive;

import io.vitess.client.Context;
import io.vitess.client.SQLFuture;
import io.vitess.client.VTGateConnection;
import io.vitess.client.VTSession;

import java.sql.SQLException;

/**
 * A {@link VTGateConnection} driver call: given a connection, context, and session, returns the
 * driver's {@link SQLFuture}. Modeled as a throwing functional interface (rather than a plain
 * {@code Function}) because every {@link VTGateConnection} call ({@code execute},
 * {@code executeBatch}, {@code streamExecute}) declares a checked {@link SQLException}, thrown
 * synchronously for argument/connection-state failures before the future is even returned --
 * distinct from a failure surfacing asynchronously through the returned {@link SQLFuture} itself.
 *
 * @param <T> Value type produced by the driver call
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@FunctionalInterface
public interface VitessAsyncCommand<T> {

    /**
     * Invoke the driver call.
     *
     * @param conn Vitess gRPC connection
     * @param ctx Vitess call context
     * @param session Vitess session (fresh or caller-supplied, depending on the calling accessor
     *     operation)
     * @return the driver's {@link SQLFuture}
     * @throws SQLException if the call fails synchronously before the future is returned
     */
    SQLFuture<T> apply(VTGateConnection conn, Context ctx, VTSession session) throws SQLException;
}
