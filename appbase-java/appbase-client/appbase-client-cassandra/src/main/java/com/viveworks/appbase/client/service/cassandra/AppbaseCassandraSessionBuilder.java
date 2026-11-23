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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.api.core.session.SessionBuilder;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Subclass of {@link SessionBuilder} that is used to create {@link AppbaseCassandraDriverContext}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseCassandraSessionBuilder extends SessionBuilder<AppbaseCassandraSessionBuilder, CqlSession> {
    private final AppbaseClientCore appbaseClientCore;

    public AppbaseCassandraSessionBuilder(final AppbaseClientCore appbaseClientCore) {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");

        this.appbaseClientCore = appbaseClientCore;
    }

    @Override
    protected DriverContext buildContext(
            final DriverConfigLoader configLoader, final ProgrammaticArguments programmaticArguments)
    {
        return new AppbaseCassandraDriverContext(appbaseClientCore, configLoader, programmaticArguments);
    }

    @Override
    protected CqlSession wrap(@NonNull final CqlSession defaultSession) {
        return defaultSession;
    }
}
