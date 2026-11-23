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

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.internal.core.context.DefaultDriverContext;
import com.datastax.oss.driver.internal.core.context.NettyOptions;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Subclass of {@link DefaultDriverContext} that uses shared {@code appbase} resources and services in the context.
 *
 * <p>
 *     TODO Phase-2: There are many more options that can be configured and custom-built by overriding their respective
 *     build() methods.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseCassandraDriverContext extends DefaultDriverContext {
    private final AppbaseClientCore appbaseClientCore;

    // TODO Phase-2: DefaultCassandraClientIntegrationServiceConfiguration as argument and use it to build more options,
    // delegate to super-class when no customization is necessary.
    public AppbaseCassandraDriverContext(
            final AppbaseClientCore appbaseClientCore,
            final DriverConfigLoader configLoader,
            final ProgrammaticArguments programmaticArguments)
    {
        super(configLoader, programmaticArguments);

        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");

        this.appbaseClientCore = appbaseClientCore;
    }

    @Override
    protected NettyOptions buildNettyOptions() {
        return new AppbaseCassandraNettyOptions(this, appbaseClientCore);
    }
}
