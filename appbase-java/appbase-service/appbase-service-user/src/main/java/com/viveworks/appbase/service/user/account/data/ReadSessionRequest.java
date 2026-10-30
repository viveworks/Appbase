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

package com.viveworks.appbase.service.user.account.data;

import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
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
@ReadSessionRequest.Valid
public class ReadSessionRequest extends AbstractTraceable {
    private String appbaseRequesterId;

    private String appbaseUserId;

    private String appbaseSessionId;

    public ReadSessionRequest() {
    }

    public ReadSessionRequest(
            final String traceId,
            final String appbaseRequesterId,
            final String appbaseUserId,
            final String appbaseSessionId)
    {
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.appbaseUserId = appbaseUserId;
        this.appbaseSessionId = appbaseSessionId;
    }

    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    public String getAppbaseUserId() {
        return appbaseUserId;
    }

    public void setAppbaseUserId(final String appbaseUserId) {
        this.appbaseUserId = appbaseUserId;
    }

    public String getAppbaseSessionId() {
        return appbaseSessionId;
    }

    public void setAppbaseSessionId(final String appbaseSessionId) {
        this.appbaseSessionId = appbaseSessionId;
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

        final ReadSessionRequest that = (ReadSessionRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(appbaseUserId, that.appbaseUserId) &&
                Objects.equals(appbaseSessionId, that.appbaseSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), appbaseRequesterId, appbaseUserId, appbaseSessionId);
    }

    @Override
    public String toString() {
        return "ReadSessionRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", appbaseUserId='" + appbaseUserId + '\'' +
                ", appbaseSessionId='" + appbaseSessionId + '\'' +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = ReadSessionRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.ReadSessionRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<ReadSessionRequest.Valid, ReadSessionRequest> {
        @Override
        public boolean isValid(final ReadSessionRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseUserId()) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseSessionId());
        }
    }
}
