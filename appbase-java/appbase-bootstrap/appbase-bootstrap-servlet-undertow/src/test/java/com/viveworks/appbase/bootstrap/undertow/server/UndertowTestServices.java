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
package com.viveworks.appbase.bootstrap.undertow.server;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;

/**
 * Test helper that builds a minimal {@link AppbaseServices} from the test config YAML.
 *
 * <p>The YAML ({@code classpath:///appbase-test-config.yaml}) disables all optional
 * heavyweight services (JPA, Lucene, JMX) so the container starts without external
 * infrastructure.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
final class UndertowTestServices {

    private static final String CONFIG = "classpath:///appbase-test-config.yaml";

    private UndertowTestServices() {}

    /**
     * Builds and returns a minimal {@link AppbaseServices} from the bundled test YAML.
     *
     * <p>Caller is responsible for shutting the returned container down (via
     * {@link AppbaseServices#shutdown()}) after use.
     *
     * @return a fully-initialized services container
     */
    static AppbaseServices minimal() {
        final AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        final AppbaseClientServices clients = AppbaseClientServices.builder()
            .from(core)
            .build();
        return AppbaseServices.builder()
            .from(core)
            .withClientServices(clients)
            .build();
    }
}
