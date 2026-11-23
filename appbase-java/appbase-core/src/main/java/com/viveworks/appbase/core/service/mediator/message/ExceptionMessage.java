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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.Poolable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an throwableDetails message.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
public final class ExceptionMessage extends AbstractContextualMessage implements Poolable {
    private Class<?> sourceType;
    private String message;
    private String threadName;
    private Throwable throwable;

    public ExceptionMessage() {
    }

    public ExceptionMessage(
            final Class<?> sourceType,
            final String message,
            final String threadName,
            final Throwable throwable)
    {
        this.sourceType = sourceType;
        this.message = message;
        this.threadName = threadName;
        this.throwable = throwable;
    }

    public ExceptionMessage(
            final long timestamp,
            final Class<?> sourceType,
            final String message,
            final String threadName,
            final Throwable throwable)
    {
        init(timestamp, sourceType, message, threadName, throwable);
    }

    public ExceptionMessage(final ExceptionMessage exceptionMessage) {
        AppbaseUtil.notNull(exceptionMessage, "exceptionMessage");

        sourceType = exceptionMessage.sourceType;
        message = exceptionMessage.message;
        threadName = exceptionMessage.threadName;
        throwable = exceptionMessage.throwable;

        final Map<String, String> context = exceptionMessage.getContext();
        if (context != null) {
            addAllContext(context);
        }
    }

    /**
     * Set all fields.
     *
     * @param timestamp Exception timestamp
     * @param sourceType Source {@link Class}
     * @param message Message
     * @param threadName Thread name
     * @param throwable {@link Throwable}
     */
    public void init(final long timestamp,
                     final Class<?> sourceType,
                     final String message,
                     final String threadName,
                     final Throwable throwable)
    {
        setTimestamp(timestamp);
        setSourceType(sourceType);
        setMessage(message);
        setThreadName(threadName);
        setThrowable(throwable);
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public void setSourceType(final Class<?> sourceType) {
        AppbaseUtil.notNull(sourceType, "sourceType");

        this.sourceType = sourceType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        AppbaseUtil.notNull(message, "message");

        this.message = message;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(final String threadName) {
        AppbaseUtil.notNull(threadName, "threadName");

        this.threadName = threadName;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(final Throwable throwable) {
        AppbaseUtil.notNull(throwable, "throwable");

        this.throwable = throwable;
    }

    @Override
    public boolean isValid() {
        return sourceType != null && message != null && threadName != null && throwable != null && super.isValid();
    }

    @Override
    public boolean isClean() {
        return sourceType == null && message == null && threadName == null && throwable == null && super.isClean();
    }


    @Override
    public void clean() {
        sourceType = null;
        message = null;
        threadName = null;
        throwable = null;
        super.clean();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final ExceptionMessage that = (ExceptionMessage) o;
        return Objects.equals(sourceType, that.sourceType) &&
                Objects.equals(message, that.message) &&
                Objects.equals(threadName, that.threadName) &&
                Objects.equals(throwable, that.throwable) &&
                super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceType, message, throwable);
    }

    @Override
    public String toString() {
        return "ExceptionMessage{" +
                "sourceType=" + sourceType +
                ", message=" + message +
                ", threadName=" + threadName +
                ", throwableDetails=" + throwable +
                '}' + super.toString();
    }
}
