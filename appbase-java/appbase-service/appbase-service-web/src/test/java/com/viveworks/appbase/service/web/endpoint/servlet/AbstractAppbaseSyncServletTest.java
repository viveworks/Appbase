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

package com.viveworks.appbase.service.web.endpoint.servlet;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * AUD-20 (CWE-838): {@code AbstractAppbaseSyncServlet.readString} decoded the request body with
 * {@code new String(bytes)} -- the JVM's PLATFORM DEFAULT charset ({@code file.encoding}), an
 * environment property, not a wire contract. The same bytes could decode to a different string
 * depending on that environment property. A single JVM's default charset is fixed for its whole
 * lifetime, so this is verified across TWO separate JVMs, one per {@code file.encoding}, each
 * actually calling the real {@link AbstractAppbaseSyncServlet#readString} method (via {@link
 * CharsetProbeMain}) rather than re-deriving the decode logic here.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractAppbaseSyncServletTest {

    @Test
    void authenticationFailureReturnsUnauthorizedWithoutExposingExceptionDetail() throws Exception {
        final Counter counter = mock(Counter.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() {
            @Override
            protected Counter counter(final String name) {
                return counter;
            }
        };
        servlet.logger = mock(Logger.class);

        servlet.handleAuthenticationServiceException("POST", "token validation detail", response);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        verify(response).flushBuffer();
    }

    @Test
    void readStringDecodesTheSameBytesIdenticallyUnderDifferentPlatformDefaultCharsets() throws Exception {
        // "café" in UTF-8 is 5 bytes: 'c','a','f', 0xC3,0xA9 (the two-byte encoding of 'é'). Under
        // ISO-8859-1, those same 5 bytes decode byte-for-byte into 5 DIFFERENT characters --
        // exactly the divergence this fix must eliminate.
        final byte[] bodyBytes = "café".getBytes(StandardCharsets.UTF_8);
        final String base64Body = Base64.getEncoder().encodeToString(bodyBytes);

        final String decodedUnderUtf8 = runProbe(base64Body, "UTF-8");
        final String decodedUnderIso88591 = runProbe(base64Body, "ISO-8859-1");

        Assertions.assertEquals(decodedUnderUtf8, decodedUnderIso88591,
                "the same request body bytes must decode to the same string regardless of the " +
                        "JVM's platform-default charset (file.encoding)");
        Assertions.assertEquals(codePointCsv("café"), decodedUnderUtf8,
                "the decoded string must actually be the UTF-8 interpretation of the bytes, not " +
                        "merely consistent");
    }

    /**
     * Runs {@link CharsetProbeMain} in a fresh JVM with the given {@code file.encoding}, on the
     * same classpath this very test is already running under, and returns the probe's reported
     * code-unit CSV.
     */
    private static String runProbe(final String base64Body, final String fileEncoding) throws Exception {
        final String javaBinary = System.getProperty("java.home") + "/bin/java";
        final String classpath = System.getProperty("java.class.path");

        final Process process = new ProcessBuilder(
                javaBinary,
                "-Dfile.encoding=" + fileEncoding,
                "-cp", classpath,
                CharsetProbeMain.class.getName(),
                base64Body)
                .start();

        final String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        final String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.US_ASCII);
        final int exitCode = process.waitFor();

        Assertions.assertEquals(0, exitCode, "probe process (file.encoding=" + fileEncoding + ") failed: " + stderr);
        return stdout.trim();
    }

    private static String codePointCsv(final String value) {
        final StringBuilder codeUnits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (i > 0) {
                codeUnits.append(',');
            }
            codeUnits.append((int) value.charAt(i));
        }
        return codeUnits.toString();
    }
}
