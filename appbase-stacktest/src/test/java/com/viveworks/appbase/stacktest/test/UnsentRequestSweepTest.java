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

package com.viveworks.appbase.stacktest.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * internal work item 1329 (N02) repeatable guard: fails if any {@code *EndpointTestImpl} arm BUILDS a request
 * URL/object but never SENDS it over the wire.
 *
 * <p>This is the class of defect that left
 * {@code StatusEndpointTestImpl.getOverallStatusTestImpl} reading the service registry in-process
 * and constructing {@code url} without ever issuing an HTTP request — a stack-test arm that
 * exercised nothing over the wire while appearing to pass. The sweep is static (no live server), so
 * it runs as a plain surefire unit test — an architectural fitness guard that complements (does not
 * replace) the arm's own behavioural assertions, catching a regression the moment the send is dropped
 * from the source.
 *
 * <p><b>Heuristic (per {@code *TestImpl()} method):</b> a method that BUILDS a request — assigns a
 * local {@code url}/{@code finalUrl}/{@code uri}, or constructs a {@code new Http*} — but makes NO
 * send call ({@code httpXxx(...)} helper family, {@code sendHttpRequest(...)}, or
 * {@code httpClient.execute(...)}) is a hollow arm. Arms that send via a derived {@code finalUrl}
 * (e.g. {@code CustomerStatistics}/{@code CustomerPhoto}) pass, because they still contain a send
 * call. Comments and string/char-literal contents are stripped before matching so that braces or
 * URL-like text inside literals cannot skew method extraction or the build/send signals.
 *
 * <p><b>Bounds (a source-level guard, not proof of execution):</b> it proves the arm's source
 * contains both build and send signals; it does not prove their ordering or association, that the arm
 * is wired to a {@code @Test}, reachable, or that the request is issued at runtime — that behaviour is
 * verified by the arm's own over-the-wire assertions (e.g. GET&rarr;200, HEAD&rarr;405 in
 * {@code getOverallStatusTestImpl}). The
 * build signal is a fixed set ({@code url}/{@code finalUrl}/{@code uri}/{@code new Http*}) and the send
 * signal is any {@code httpXxx(...)}/{@code sendHttpRequest(...)}/{@code httpClient.execute(...)} call,
 * so a URL built into a differently-named variable, or a future non-sending helper named
 * {@code httpXxx(...)}, could evade it; the set covers every current arm and is widened when a new
 * shape appears.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class UnsentRequestSweepTest {

    /** Main sources, resolved from the surefire module basedir (appbase-stacktest/). */
    private static final Path SRC = Paths.get("src", "main", "java");

    private static final Pattern METHOD =
        Pattern.compile("public\\s+void\\s+(\\w+TestImpl)\\s*\\([^)]*\\)\\s*(?:throws[^{]*)?\\{");

    private static final Pattern BUILDS =
        Pattern.compile("\\b(url|finalUrl|uri)\\b\\s*=(?!=)|new\\s+Http(Get|Post|Put|Head|Delete)\\b");

    private static final Pattern SENDS =
        Pattern.compile("\\bhttp[A-Z]\\w*\\s*\\(|\\bsendHttpRequest\\s*\\(|httpClient\\s*\\.\\s*execute\\s*\\(");

    @Test
    void noEndpointTestImplBuildsARequestItNeverSends() throws IOException {
        final List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SRC)) {
            walk.filter(p -> p.getFileName().toString().endsWith("EndpointTestImpl.java"))
                .forEach(files::add);
        }
        // Guard against a vacuous pass: if the working dir is wrong or the package moved, the sweep
        // would scan nothing and "pass". Require the full arm set (15 today; floor of 10).
        assertTrue(files.size() >= 10,
            "sweep found only " + files.size() + " *EndpointTestImpl.java under "
            + SRC.toAbsolutePath() + " — expected the full arm set; a wrong working dir or a moved "
            + "package would make this guard vacuous");

        final List<String> hollow = new ArrayList<>();
        for (final Path f : files) {
            final String src = sanitize(Files.readString(f));
            for (final String[] method : methods(src)) {
                final String name = method[0];
                final String body = method[1];
                if (BUILDS.matcher(body).find() && !SENDS.matcher(body).find()) {
                    hollow.add(f.getFileName().toString() + "#" + name);
                }
            }
        }
        assertTrue(hollow.isEmpty(),
            "arm(s) build a request URL/object but never send it over the wire: " + hollow
            + " — wire the arm to an httpXxx(...)/httpCodeOfXxx(...) send, or it exercises nothing");
    }

    @Test
    void extractsANoThrowsMethodAndRejectsItsMissingSend() {
        final String source = "public void hollowEndpointTestImpl() { String url = baseUrl; }";
        final List<String[]> extracted = methods(sanitize(source));
        assertTrue(extracted.size() == 1, "a no-throws TestImpl method must be extracted");
        assertTrue(BUILDS.matcher(extracted.get(0)[1]).find()
                && !SENDS.matcher(extracted.get(0)[1]).find(),
            "the no-throws negative control must be classified as hollow");
    }

    /** Each {@code *TestImpl()} method as {name, body}, extracted by brace matching. */
    private static List<String[]> methods(final String src) {
        final List<String[]> out = new ArrayList<>();
        final Matcher m = METHOD.matcher(src);
        while (m.find()) {
            int depth = 1;
            int i = m.end();
            final int start = i;
            while (i < src.length() && depth > 0) {
                final char c = src.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                i++;
            }
            out.add(new String[] {m.group(1), src.substring(start, Math.max(start, i - 1))});
        }
        return out;
    }

    /**
     * Blank out line/block comments and the CONTENTS of string/char literals (keeping the quotes and
     * overall length/structure), so braces or URL-like text inside them cannot skew the scan.
     */
    private static String sanitize(final String src) {
        final StringBuilder sb = new StringBuilder(src.length());
        int i = 0;
        final int n = src.length();
        while (i < n) {
            final char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                while (i < n && src.charAt(i) != '\n') {
                    sb.append(' ');
                    i++;
                }
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                sb.append("  ");
                i += 2;
                while (i < n && !(src.charAt(i) == '*' && i + 1 < n && src.charAt(i + 1) == '/')) {
                    sb.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    sb.append("  ");
                    i += 2;
                }
            } else if (c == '"' || c == '\'') {
                final char quote = c;
                sb.append(quote);
                i++;
                while (i < n && src.charAt(i) != quote) {
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        sb.append("  ");
                        i += 2;
                    } else {
                        sb.append(' ');
                        i++;
                    }
                }
                if (i < n) {
                    sb.append(quote);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
