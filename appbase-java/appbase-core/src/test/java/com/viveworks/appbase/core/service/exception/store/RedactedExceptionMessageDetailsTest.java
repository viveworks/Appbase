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

package com.viveworks.appbase.core.service.exception.store;

import com.viveworks.appbase.core.util.jackson.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lens 4 / L4-04 (CWE-200, CWE-209): {@link ExceptionMessageDetails} carries free-text fields
 * ({@code throwableMessage}, {@code stackTrace}, {@code context}) that can embed arbitrary
 * caller-controlled content -- paths, tokens, PII -- yet the HTTP exception endpoints (native,
 * offloaded, servlet) serialised it unchanged. {@link RedactedExceptionMessageDetails} is the
 * transport-facing view that omits exactly those three fields while keeping the non-sensitive
 * summary (type, hash, thread, timestamp) that makes the endpoint still useful for diagnostics.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class RedactedExceptionMessageDetailsTest {

    private static final String SENTINEL_STACK_TRACE = "at com.example.Secret.leak(/private/sentinel-stack)";
    private static final String SENTINEL_THROWABLE_MESSAGE = "sentinel-throwable-message-/private/secret";
    private static final String SENTINEL_CONTEXT = "sentinel-context-token-abc123";

    private static ExceptionMessageDetails source() {
        final ExceptionMessageDetails details = new ExceptionMessageDetails();
        details.setId(7);
        details.setDiscriminator("discriminator");
        details.setMessage("a normal summary message");
        details.setThrowableTypeName("java.lang.NullPointerException");
        details.setStackTraceHash(123);
        details.setThrowableMessage(SENTINEL_THROWABLE_MESSAGE);
        details.setStackTrace(SENTINEL_STACK_TRACE);
        details.setSourceTypeName("com.example.Foo");
        details.setThreadName("main");
        details.setContext(SENTINEL_CONTEXT);
        details.setTimestamp(1000L);
        return details;
    }

    @Test
    void from_keepsTheNonSensitiveSummaryFields() {
        final ExceptionMessageDetails source = source();
        final RedactedExceptionMessageDetails redacted = RedactedExceptionMessageDetails.from(source);

        assertEquals(source.getId(), redacted.getId());
        assertEquals(source.getDiscriminator(), redacted.getDiscriminator());
        assertEquals(source.getMessage(), redacted.getMessage());
        assertEquals(source.getThrowableTypeName(), redacted.getThrowableTypeName());
        assertEquals(source.getStackTraceHash(), redacted.getStackTraceHash());
        assertEquals(source.getSourceTypeName(), redacted.getSourceTypeName());
        assertEquals(source.getThreadName(), redacted.getThreadName());
        assertEquals(source.getTimestamp(), redacted.getTimestamp());
    }

    @Test
    void from_null_returnsNull() {
        assertEquals(null, RedactedExceptionMessageDetails.from(null));
    }

    @Test
    void jsonSerialization_doesNotContainAnySentinel() throws Exception {
        final String json = JsonUtil.getJsonMapper()
                .writerFor(RedactedExceptionMessageDetails.class)
                .writeValueAsString(RedactedExceptionMessageDetails.from(source()));

        assertFalse(json.contains(SENTINEL_STACK_TRACE), "stack trace sentinel must not be serialized");
        assertFalse(json.contains(SENTINEL_THROWABLE_MESSAGE), "throwable-message sentinel must not be serialized");
        assertFalse(json.contains(SENTINEL_CONTEXT), "context sentinel must not be serialized");
        // "stackTrace" alone would also match the (intentionally retained) "stackTraceHash" key,
        // so the check is for the quoted key exactly.
        assertFalse(json.contains("\"stackTrace\""), "the stackTrace key itself must be absent, not just its value");
        assertFalse(json.contains("\"throwableMessage\""), "the throwableMessage key itself must be absent");
        assertFalse(json.contains("\"context\""), "the context key itself must be absent");
        assertTrue(json.contains("\"stackTraceHash\""), "the (non-sensitive) stack-trace hash must remain");
        assertTrue(json.contains("NullPointerException"), "the (non-sensitive) throwable type must remain");
    }
}
