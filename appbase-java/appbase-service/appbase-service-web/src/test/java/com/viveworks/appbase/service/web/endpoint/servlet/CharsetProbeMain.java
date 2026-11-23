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

package com.viveworks.appbase.service.web.endpoint.servlet;

import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;

import java.util.Base64;

/**
 * Out-of-process probe for AUD-20: launched as a separate JVM (with its OWN {@code
 * -Dfile.encoding}) by {@link AbstractAppbaseSyncServletTest}, so the test can observe whether
 * {@link AbstractAppbaseSyncServlet#readString(HttpServletRequest)} actually decodes the SAME
 * bytes into the SAME string across different platform-default charsets, which a single-process
 * JUnit test cannot force (the JVM's default charset is fixed for the life of the process).
 * Prints the decoded string's UTF-16 code units as a comma-separated list on stdout, so the
 * result survives the pipe without being re-interpreted through yet another console encoding.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class CharsetProbeMain {
    private CharsetProbeMain() {
    }

    public static void main(final String[] args) throws Exception {
        final byte[] bytes = Base64.getDecoder().decode(args[0]);

        final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpServletRequest.getInputStream()).thenReturn(new ByteArrayServletInputStream(bytes));

        final AbstractAppbaseSyncServlet servlet = new AbstractAppbaseSyncServlet() { };
        final String result = servlet.readString(httpServletRequest);

        final StringBuilder codeUnits = new StringBuilder();
        for (int i = 0; i < result.length(); i++) {
            if (i > 0) {
                codeUnits.append(',');
            }
            codeUnits.append((int) result.charAt(i));
        }
        System.out.print(codeUnits);
    }
}
