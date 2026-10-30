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

package com.viveworks.appbase.core;

import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.microprofile.AppbaseCoreMicroprofile;
import com.viveworks.appbase.core.microprofile.DefaultAppbaseCoreMicroprofile;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;

/**
 * Factory for creating instances of {@link AppbaseCore} and {@link AppbaseCoreMicroprofile}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AppbaseCoreFactory {
    private AppbaseCoreFactory() {
    }


    /**
     * Create an instance of {@link AppbaseCore}.
     *
     * @return AppbaseCore
     */
    public static AppbaseCore newAppbaseCore() {
        return new DefaultAppbaseCore();
    }


    /**
     * Create an instance of {@link AppbaseCore}.
     *
     * @param appbaseCoreBootstrap {@link AppbaseCoreBootstrap}
     * @return AppbaseCore
     */
    public static AppbaseCore newAppbaseCore(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap)
    {
        return new DefaultAppbaseCore(appbaseCoreBootstrap);
    }


    /**
     * Create an instance of {@link AppbaseCore}.
     *
     * @param appbaseCoreBootstrap {@link AppbaseCoreBootstrap}
     * @param appbaseCoreBootstrapConfiguration {@link AppbaseCoreBootstrapConfiguration}
     * @return AppbaseCore
     */
    public static AppbaseCore newAppbaseCore(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
            final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        return new DefaultAppbaseCore(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
    }


    /**
     * Create an instance of {@link AppbaseCoreMicroprofile}.
     *
     * @return AppbaseCoreMicroprofile
     */
    public static AppbaseCoreMicroprofile newAppbaseCoreMicroprofile() {
        return new DefaultAppbaseCoreMicroprofile();
    }


    /**
     * Create an instance of {@link AppbaseCoreMicroprofile}.
     *
     * @param appbaseCoreBootstrap {@link AppbaseCoreBootstrap}
     * @return AppbaseCoreMicroprofile
     */
    public static AppbaseCoreMicroprofile newAppbaseCoreMicroprofile(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap)
    {
        return new DefaultAppbaseCoreMicroprofile(appbaseCoreBootstrap);
    }


    /**
     * Create an instance of {@link AppbaseCoreMicroprofile}.
     *
     * @param appbaseCoreBootstrap {@link AppbaseCoreBootstrap}
     * @param appbaseCoreBootstrapConfiguration {@link AppbaseCoreBootstrapConfiguration}
     * @return AppbaseCoreMicroprofile
     */
    public static AppbaseCoreMicroprofile newAppbaseCoreMicroprofile(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
            final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        return new DefaultAppbaseCoreMicroprofile(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
    }


    /**
     * Create an instance of {@link AppbaseCoreMicroprofile}.
     *
     * @param appbaseCore {@link AppbaseCore}
     * @param microprofileConfigurationService {@link MicroprofileConfigurationService}
     * @param microprofileServiceRegistry {@link MicroprofileServiceRegistry}
     * @return AppbaseCoreMicroprofile
     */
    public static AppbaseCoreMicroprofile newAppbaseCoreMicroprofile(
            final AppbaseCore appbaseCore,
            final MicroprofileConfigurationService microprofileConfigurationService,
            final MicroprofileServiceRegistry microprofileServiceRegistry)
    {
        return new DefaultAppbaseCoreMicroprofile(
                appbaseCore, microprofileConfigurationService, microprofileServiceRegistry);
    }
}
