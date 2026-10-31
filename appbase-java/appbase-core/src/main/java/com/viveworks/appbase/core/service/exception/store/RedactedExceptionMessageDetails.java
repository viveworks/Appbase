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

package com.viveworks.appbase.core.service.exception.store;

import javax.annotation.Nullable;

/**
 * The transport-facing (HTTP JSON) view of {@link ExceptionMessageDetails}, used by every
 * {@code /exception} serving face (native reactor-netty, offloaded JAX-RS, servlet) in place of
 * the entity itself (Lens 4 / L4-04, CWE-200, CWE-209; OWASP A01:2021).
 *
 * <p>{@code throwableMessage}, {@code stackTrace}, and {@code context} are free-text fields that
 * can embed whatever a caller or a downstream library put into an exception -- filesystem paths,
 * tokens, PII, internal class/field names -- so serialising {@link ExceptionMessageDetails}
 * unchanged let any caller of the (potentially unauthenticated) exception-reporting endpoint read
 * all of it back. This view omits exactly those three fields and keeps the rest: the throwable's
 * type name, a hash of its stack trace (useful for grouping/dedup without disclosing content),
 * the reporting source type and thread, and the timestamp -- enough to triage *that* something
 * broke and roughly where. <strong>This is narrower than "discloses nothing beyond safe summary
 * metadata": {@code message} is also kept, and at least one caller
 * ({@code DefaultConfigurationService}'s configuration-event listener logging) formats it from
 * {@code exception.toString()}, the same caller/downstream-controlled free text this view exists
 * to drop from the other three fields. That exposure predates this view (the unredacted entity
 * already carried it) and is not changed by this class in either direction; recorded here so the
 * class's own contract is not read as stronger than what it actually guarantees.</strong>
 *
 * <p>There is deliberately no "privileged diagnostic" expansion path back to the full entity here;
 * if an authenticated, elevated caller ever needs the raw fields, that is a separate, explicitly
 * gated feature to design later, not a default this endpoint falls back to.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class RedactedExceptionMessageDetails {

    @Nullable
    private final Integer id;
    @Nullable
    private final String discriminator;
    @Nullable
    private final String message;
    @Nullable
    private final String throwableTypeName;
    @Nullable
    private final Integer stackTraceHash;
    @Nullable
    private final String sourceTypeName;
    @Nullable
    private final String threadName;
    @Nullable
    private final Long timestamp;

    private RedactedExceptionMessageDetails(final ExceptionMessageDetails source) {
        this.id = source.getId();
        this.discriminator = source.getDiscriminator();
        this.message = source.getMessage();
        this.throwableTypeName = source.getThrowableTypeName();
        this.stackTraceHash = source.getStackTraceHash();
        this.sourceTypeName = source.getSourceTypeName();
        this.threadName = source.getThreadName();
        this.timestamp = source.getTimestamp();
    }

    /**
     * Builds the redacted view of {@code source}.
     *
     * @param source the full entity, or {@code null}
     * @return the redacted view, or {@code null} when {@code source} is {@code null}
     */
    @Nullable
    public static RedactedExceptionMessageDetails from(@Nullable final ExceptionMessageDetails source) {
        return source == null ? null : new RedactedExceptionMessageDetails(source);
    }

    @Nullable
    public Integer getId() {
        return id;
    }

    @Nullable
    public String getDiscriminator() {
        return discriminator;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getThrowableTypeName() {
        return throwableTypeName;
    }

    @Nullable
    public Integer getStackTraceHash() {
        return stackTraceHash;
    }

    @Nullable
    public String getSourceTypeName() {
        return sourceTypeName;
    }

    @Nullable
    public String getThreadName() {
        return threadName;
    }

    @Nullable
    public Long getTimestamp() {
        return timestamp;
    }

    // Deliberately no throwableMessage/stackTrace/context getters: Jackson's default
    // getter-based serialization only emits properties with a getter, so omitting them here
    // means the JSON has no such keys at all -- not even a disclosed-empty "null" placeholder.
}
