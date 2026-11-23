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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import javax.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Represents {@link com.viveworks.appbase.core.service.mediator.message.ExceptionMessage} details stored by an
 * {@link ExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Entity
@Table(
        indexes = {
                @Index(name = "Index_ExceptionMessageDetails_discriminator", columnList = "discriminator"),
                @Index(name = "Index_ExceptionMessageDetails_message", columnList = "message"),
                @Index(name = "Index_ExceptionMessageDetails_throwableTypeName", columnList = "throwableTypeName"),
                @Index(name = "Index_ExceptionMessageDetails_stackTraceHash", columnList = "stackTraceHash"),
                @Index(name = "Index_ExceptionMessageDetails_sourceTypeName", columnList = "sourceTypeName"),
                @Index(name = "Index_ExceptionMessageDetails_threadName", columnList = "threadName"),
                @Index(name = "Index_ExceptionMessageDetails_timestamp", columnList = "timestamp"),
        }
)
public class ExceptionMessageDetails {
    private static final int MAX_LENGTH = 1024;

    @NotNull(groups = ReadChecks.class)
    @Positive(groups = ReadChecks.class)
    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String discriminator;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String message;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String throwableTypeName;

    @NotNull(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false)
    private Integer stackTraceHash;

    @Nullable
    @Basic
    @Column(length = MAX_LENGTH)
    private String throwableMessage;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String stackTrace;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String sourceTypeName;

    @NotBlank(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false, length = MAX_LENGTH)
    private String threadName;

    @Nullable
    @Basic
    @Column(length = MAX_LENGTH)
    private String context;

    @NotNull(groups = {ReadChecks.class, WriteChecks.class})
    @Positive(groups = {ReadChecks.class, WriteChecks.class})
    @Basic(optional = false)
    @Column(nullable = false)
    private Long timestamp;

    public ExceptionMessageDetails() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(final String discriminator) {
        this.discriminator = discriminator;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getThrowableTypeName() {
        return throwableTypeName;
    }

    public void setThrowableTypeName(final String throwableTypeName) {
        this.throwableTypeName = throwableTypeName;
    }

    public Integer getStackTraceHash() {
        return stackTraceHash;
    }

    public void setStackTraceHash(final Integer stackTraceHash) {
        this.stackTraceHash = stackTraceHash;
    }

    @Nullable
    public String getThrowableMessage() {
        return throwableMessage;
    }

    public void setThrowableMessage(@Nullable final String throwableMessage) {
        this.throwableMessage = throwableMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(final String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getSourceTypeName() {
        return sourceTypeName;
    }

    public void setSourceTypeName(final String sourceTypeName) {
        this.sourceTypeName = sourceTypeName;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(final String threadName) {
        this.threadName = threadName;
    }

    @Nullable
    public String getContext() {
        return context;
    }

    public void setContext(@Nullable final String context) {
        this.context = context;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ExceptionMessageDetails that = (ExceptionMessageDetails) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(discriminator, that.discriminator) &&
                Objects.equals(message, that.message) &&
                Objects.equals(throwableTypeName, that.throwableTypeName) &&
                Objects.equals(stackTraceHash, that.stackTraceHash) &&
                Objects.equals(throwableMessage, that.throwableMessage) &&
                Objects.equals(stackTrace, that.stackTrace) &&
                Objects.equals(sourceTypeName, that.sourceTypeName) &&
                Objects.equals(threadName, that.threadName) &&
                Objects.equals(context, that.context) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                discriminator,
                message,
                throwableTypeName,
                stackTraceHash,
                throwableMessage,
                stackTrace,
                sourceTypeName,
                threadName,
                context,
                timestamp);
    }

    @Override
    public String toString() {
        return "ExceptionMessageDetails{" +
                "id=" + id +
                ", discriminator='" + discriminator + '\'' +
                ", message='" + message + '\'' +
                ", throwableTypeName='" + throwableTypeName + '\'' +
                ", stackTraceHash=" + stackTraceHash +
                ", throwableMessage='" + throwableMessage + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                ", sourceTypeName='" + sourceTypeName + '\'' +
                ", threadName='" + threadName + '\'' +
                ", context='" + context + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }


    /**
     * Used for validation checks during read operations.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public interface ReadChecks {
    }


    /**
     * Used for validation checks during write operations.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public interface WriteChecks {
    }
}
