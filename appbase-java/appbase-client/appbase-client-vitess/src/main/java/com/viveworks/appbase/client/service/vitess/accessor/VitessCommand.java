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

package com.viveworks.appbase.client.service.vitess.accessor;

import io.vitess.client.Context;
import io.vitess.client.VTGateBlockingConnection;
import io.vitess.client.VTSession;

import java.sql.SQLException;

/**
 * A driver call run against the real blocking {@link VTGateBlockingConnection} -- the
 * synchronous counterpart of {@code VitessAsyncCommand} in the {@code accessor.reactive}
 * package, which runs the same shape of call against the async {@code VTGateConnection} and
 * returns an {@code SQLFuture} instead. {@link VTGateBlockingConnection}'s driver calls throw a
 * checked {@link SQLException} directly (there is no future to bridge), so this functional
 * interface declares it.
 *
 * @param <T> Result type of the driver call
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@FunctionalInterface
public interface VitessCommand<T> {

    /**
     * Run the driver call.
     *
     * @param conn Borrowed blocking Vitess connection
     * @param ctx Vitess context to use for the call
     * @param session Vitess session to use for the call
     * @return the driver call's result
     * @throws SQLException the raw driver failure, if the call fails
     */
    T apply(VTGateBlockingConnection conn, Context ctx, VTSession session) throws SQLException;
}
