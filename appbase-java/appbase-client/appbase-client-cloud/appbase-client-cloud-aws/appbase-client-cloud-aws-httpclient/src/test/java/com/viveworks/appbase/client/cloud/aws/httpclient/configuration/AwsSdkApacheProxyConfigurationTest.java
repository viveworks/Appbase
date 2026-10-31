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

package com.viveworks.appbase.client.cloud.aws.httpclient.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AUD-34 / L4-06 (CWE-532, CWE-200): {@code toString()} must not disclose the proxy password, nor
 * any other field that can carry a credential or arbitrary caller-supplied content — {@code
 * endpoint} (a proxy URI may embed {@code user:password@host} userinfo) and {@code nonProxyHosts}
 * (an open-ended collection, same reasoning as a map field). This mirrors the broader redaction
 * {@code appbase-service-core}'s companion fix (PR internal work item 2558, {@code T2553}) was required to land after
 * review found that redacting only {@code password} was insufficient for this same class.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AwsSdkApacheProxyConfigurationTest {

    @Test
    void toString_doesNotDiscloseThePassword() {
        final String sentinel = "sentinel-proxy-password-AUD34";
        final AwsSdkApacheProxyConfiguration configuration = AwsSdkApacheProxyConfiguration.builder()
                .password(sentinel)
                .build();

        final String result = configuration.toString();

        assertFalse(result.contains(sentinel), "toString() must not contain the raw password");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the password");
    }

    @Test
    void toString_doesNotDiscloseEndpointUserInfo() {
        final String sentinel = "sentinel-proxy-endpoint-AUD34";
        final AwsSdkApacheProxyConfiguration configuration = AwsSdkApacheProxyConfiguration.builder()
                .endpoint(URI.create("https://user:" + sentinel + "@proxy.example"))
                .build();

        final String result = configuration.toString();

        assertFalse(result.contains(sentinel), "toString() must not contain the endpoint's userinfo");
        assertTrue(result.contains("endpoint=" + AppbaseUtil.OBFUSCATE),
                "toString() must obfuscate the endpoint field");
    }

    @Test
    void toString_doesNotDiscloseNonProxyHosts() {
        final String sentinel = "sentinel-non-proxy-host-AUD34";
        final AwsSdkApacheProxyConfiguration configuration = AwsSdkApacheProxyConfiguration.builder()
                .nonProxyHosts(Set.of(sentinel))
                .build();

        final String result = configuration.toString();

        assertFalse(result.contains(sentinel), "toString() must not contain nonProxyHosts' content");
    }

    @Test
    void getPassword_stillReturnsTheRealValue() {
        final String sentinel = "sentinel-proxy-password-AUD34";
        final AwsSdkApacheProxyConfiguration configuration = AwsSdkApacheProxyConfiguration.builder()
                .password(sentinel)
                .build();

        assertTrue(sentinel.equals(configuration.getPassword()),
                "obfuscating toString() must not obfuscate the real getter value");
    }

    @Test
    void getEndpoint_stillReturnsTheRealValue() {
        final URI endpoint = URI.create("https://user:secret@proxy.example");
        final AwsSdkApacheProxyConfiguration configuration = AwsSdkApacheProxyConfiguration.builder()
                .endpoint(endpoint)
                .build();

        assertTrue(endpoint.equals(configuration.getEndpoint()),
                "obfuscating toString() must not obfuscate the real getter value");
    }
}
