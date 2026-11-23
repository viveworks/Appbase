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

package com.viveworks.appbase.service.user.account.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@UpdateUnauthenticatedSessionRequest.Valid
public class UpdateUnauthenticatedSessionRequest extends AbstractTraceable {
    private String appbaseSessionId;

    private UserSessionData userSessionData;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    /**
     * If this is true, then it is a [read+update] operation by the client. So, update all the fields as given.
     *
     * If this is false, then it is not a [read+update] operation by the client. It's just an update operation and
     * only the non-null properties must be updated. The client cares to update only the non-null properties.
     */
    private boolean readAndUpdate;

    public UpdateUnauthenticatedSessionRequest() {
    }

    public UpdateUnauthenticatedSessionRequest(
            final String traceId,
            final String appbaseSessionId,
            final UserSessionData userSessionData,
            final LocalDateTime lastAccessTimestamp,
            final boolean readAndUpdate)
    {
        this.traceId = traceId;
        this.appbaseSessionId = appbaseSessionId;
        this.userSessionData = userSessionData;
        this.lastAccessTimestamp = lastAccessTimestamp;
        this.readAndUpdate = readAndUpdate;
    }

    public String getAppbaseSessionId() {
        return appbaseSessionId;
    }

    public void setAppbaseSessionId(final String appbaseSessionId) {
        this.appbaseSessionId = appbaseSessionId;
    }

    public UserSessionData getUserSessionData() {
        return userSessionData;
    }

    public void setUserSessionData(final UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    public LocalDateTime getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public boolean isReadAndUpdate() {
        return readAndUpdate;
    }

    public void setReadAndUpdate(final boolean readAndUpdate) {
        this.readAndUpdate = readAndUpdate;
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

        final UpdateUnauthenticatedSessionRequest that = (UpdateUnauthenticatedSessionRequest) o;
        return readAndUpdate == that.readAndUpdate &&
                Objects.equals(appbaseSessionId, that.appbaseSessionId) &&
                Objects.equals(userSessionData, that.userSessionData) &&
                Objects.equals(lastAccessTimestamp, that.lastAccessTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(), appbaseSessionId, userSessionData, lastAccessTimestamp, readAndUpdate);
    }

    @Override
    public String toString() {
        return "UpdateUnauthenticatedSessionRequest{" +
                "appbaseSessionId='" + appbaseSessionId + '\'' +
                ", userSessionData=" + userSessionData +
                ", lastAccessTimestamp=" + lastAccessTimestamp +
                ", readAndUpdate=" + readAndUpdate +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UpdateUnauthenticatedSessionRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.UpdateUnauthenticatedSessionRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<UpdateUnauthenticatedSessionRequest.Valid, UpdateUnauthenticatedSessionRequest>
    {
        @Override
        public boolean isValid(
                final UpdateUnauthenticatedSessionRequest value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseSessionId());
        }
    }
}
