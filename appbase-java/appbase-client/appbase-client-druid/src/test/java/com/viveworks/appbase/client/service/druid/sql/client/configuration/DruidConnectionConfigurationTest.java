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

package com.viveworks.appbase.client.service.druid.sql.client.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AUD-34 / L4-06 (CWE-532, CWE-200): {@code toString()} must not disclose the connection
 * password, nor any other field that can carry a credential or arbitrary caller-supplied
 * content — {@code url} and {@code authentication} may themselves embed credentials (e.g. JDBC
 * userinfo), and {@code clientInfo}/{@code additionalProperties} are open-ended maps. Mirrors the
 * broader redaction this same class (and its AWS-proxy sibling) was required to land under PR
 * internal work item 2558 (T2553) after review found that redacting only {@code password} was insufficient.
 * {@code trustStorePassword}, {@code keystorePassword} and {@code keyPassword} already redacted
 * correctly before this fix.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DruidConnectionConfigurationTest {

    @Test
    void toString_doesNotDiscloseThePassword() {
        final String sentinel = "sentinel-druid-password-AUD34";
        final DruidConnectionConfiguration configuration = DruidConnectionConfiguration.builder()
                .url("jdbc:mysql://localhost:3306/test")
                .password(sentinel)
                .build();

        final String result = configuration.toString();

        assertFalse(result.contains(sentinel), "toString() must not contain the raw password");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the password");
    }

    @Test
    void toString_doesNotDiscloseUrlOrAuthentication() {
        final String urlSentinel = "sentinel-druid-url-AUD34";
        final String authSentinel = "sentinel-druid-authentication-AUD34";
        final DruidConnectionConfiguration configuration = DruidConnectionConfiguration.builder()
                .url("jdbc:avatica:remote:url=https://user:" + urlSentinel + "@druid.example")
                .authentication(authSentinel)
                .build();

        final String result = configuration.toString();

        assertFalse(result.contains(urlSentinel), "toString() must not contain the raw url");
        assertFalse(result.contains(authSentinel), "toString() must not contain the raw authentication value");
    }

    @Test
    void toString_doesNotDiscloseClientInfoOrAdditionalProperties() {
        final String clientInfoSentinel = "sentinel-druid-clientinfo-AUD34";
        final String additionalPropertySentinel = "sentinel-druid-additionalprop-AUD34";
        final DruidConnectionConfiguration configuration = DruidConnectionConfiguration.builder()
                .url("jdbc:mysql://localhost:3306/test")
                .clientInfo(Map.of("arbitrary-client-info", clientInfoSentinel))
                .additionalProperties(Map.of("arbitrary-property", additionalPropertySentinel))
                .build();

        final String result = configuration.toString();

        assertFalse(result.contains(clientInfoSentinel), "toString() must not contain clientInfo's content");
        assertFalse(result.contains(additionalPropertySentinel),
                "toString() must not contain additionalProperties' content");
    }

    @Test
    void getPassword_stillReturnsTheRealValue() {
        final String sentinel = "sentinel-druid-password-AUD34";
        final DruidConnectionConfiguration configuration = DruidConnectionConfiguration.builder()
                .url("jdbc:mysql://localhost:3306/test")
                .password(sentinel)
                .build();

        assertTrue(sentinel.equals(configuration.getPassword()),
                "obfuscating toString() must not obfuscate the real getter value");
    }

    @Test
    void getUrl_stillReturnsTheRealValue() {
        final String url = "jdbc:mysql://localhost:3306/test";
        final DruidConnectionConfiguration configuration = DruidConnectionConfiguration.builder()
                .url(url)
                .build();

        assertTrue(url.equals(configuration.getUrl()),
                "obfuscating toString() must not obfuscate the real getter value");
    }
}
