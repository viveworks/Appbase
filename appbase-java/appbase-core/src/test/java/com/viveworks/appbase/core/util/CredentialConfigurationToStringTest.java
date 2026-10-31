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

package com.viveworks.appbase.core.util;

import com.viveworks.appbase.core.service.tracer.configuration.reporter.RemoteReporterConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslKeyManagerConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslServerContextConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies that core credential-bearing configurations render fail-closed.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class CredentialConfigurationToStringTest {
    @Test
    void remoteReporterRedactsAuthenticationSecrets() {
        final String token = "reporter-token-secret-2558";
        final String password = "reporter-password-secret-2558";
        final String rendered = RemoteReporterConfiguration.builder()
                .authToken(token)
                .authPassword(password)
                .build()
                .toString();

        Assertions.assertFalse(rendered.contains(token));
        Assertions.assertFalse(rendered.contains(password));
        Assertions.assertTrue(rendered.contains("authToken='********'"));
        Assertions.assertTrue(rendered.contains("authPassword='********'"));
    }

    @Test
    void sslKeyManagerRedactsPrivateKeyPassword() {
        final String secret = "key-manager-secret-2558";
        final String rendered = SslKeyManagerConfiguration.builder()
                .privateKeyFilePassword(secret)
                .build()
                .toString();

        Assertions.assertFalse(rendered.contains(secret));
        Assertions.assertTrue(rendered.contains("privateKeyFilePassword='********'"));
    }

    @Test
    void sslServerContextRedactsPrivateKeyPassword() {
        final String secret = "server-context-secret-2558";
        final String rendered = SslServerContextConfiguration.builder()
                .privateKeyFilePassword(secret)
                .build()
                .toString();

        Assertions.assertFalse(rendered.contains(secret));
        Assertions.assertTrue(rendered.contains("privateKeyFilePassword='********'"));
    }
}
