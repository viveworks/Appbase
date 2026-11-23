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
@CreateUnauthenticatedSessionRequest.Valid
public class CreateUnauthenticatedSessionRequest extends AbstractTraceable {
    private UserSessionData userSessionData;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime creationTimestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    public CreateUnauthenticatedSessionRequest() {
    }

    public CreateUnauthenticatedSessionRequest(
            final String traceId,
            final UserSessionData userSessionData,
            final LocalDateTime creationTimestamp,
            final LocalDateTime lastAccessTimestamp)
    {
        this.traceId = traceId;
        this.userSessionData = userSessionData;
        this.creationTimestamp = creationTimestamp;
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public UserSessionData getUserSessionData() {
        return userSessionData;
    }

    public void setUserSessionData(final UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(final LocalDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public LocalDateTime getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
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

        final CreateUnauthenticatedSessionRequest that = (CreateUnauthenticatedSessionRequest) o;
        return Objects.equals(userSessionData, that.userSessionData) &&
                Objects.equals(creationTimestamp, that.creationTimestamp) &&
                Objects.equals(lastAccessTimestamp, that.lastAccessTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userSessionData, creationTimestamp, lastAccessTimestamp);
    }

    @Override
    public String toString() {
        return "CreateUnauthenticatedSessionRequest{" +
                "userSessionData=" + userSessionData +
                ", creationTimestamp=" + creationTimestamp +
                ", lastAccessTimestamp=" + lastAccessTimestamp +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = CreateUnauthenticatedSessionRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.CreateUnauthenticatedSessionRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<CreateUnauthenticatedSessionRequest.Valid, CreateUnauthenticatedSessionRequest>
    {
        @Override
        public boolean isValid(
                final CreateUnauthenticatedSessionRequest value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotNull(value.getUserSessionData()) &&
                    AppbaseUtil.isNotNull(value.getCreationTimestamp()) &&
                    AppbaseUtil.isNotNull(value.getLastAccessTimestamp());
        }
    }
}
