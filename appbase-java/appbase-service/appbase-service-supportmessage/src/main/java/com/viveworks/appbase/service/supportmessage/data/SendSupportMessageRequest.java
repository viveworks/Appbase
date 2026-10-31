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
package com.viveworks.appbase.service.supportmessage.data;

import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import javax.annotation.Nullable;
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
@SendSupportMessageRequest.Valid
public class SendSupportMessageRequest extends AbstractTraceable {
    private String email;

    private String message;

    private String status;

    @Nullable private String appbaseUserId;

    public SendSupportMessageRequest() {
    }

    public SendSupportMessageRequest(
            final String email,
            final String message,
            final String status,
            @Nullable String appbaseUserId,
            @Nullable final String traceId)
    {
        super(traceId);

        this.email = email;
        this.message = message;
        this.status = status;
        this.appbaseUserId = appbaseUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @Nullable
    public String getAppbaseUserId() {
        return appbaseUserId;
    }

    public void setAppbaseUserId(@Nullable final String appbaseUserId) {
        this.appbaseUserId = appbaseUserId;
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

        final SendSupportMessageRequest that = (SendSupportMessageRequest) o;
        return Objects.equals(email, that.email) &&
                Objects.equals(message, that.message) &&
                Objects.equals(status, that.status) &&
                Objects.equals(appbaseUserId, that.appbaseUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), email, message, appbaseUserId);
    }

    @Override
    public String toString() {
        return "SendSupportMessageRequest{" +
                "email='" + email + '\'' +
                ", message='" + message + '\'' +
                ", status='" + status + '\'' +
                ", appbaseUserId='" + appbaseUserId + '\'' +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = SendSupportMessageRequest.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.service.supportmessage.SendSupportMessageRequest}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator
            implements ConstraintValidator<SendSupportMessageRequest.Valid, SendSupportMessageRequest>
    {
        @Override
        public boolean isValid(final SendSupportMessageRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getEmail()) &&
                    AppbaseUtil.isNotBlank(value.getMessage()) &&
                    AppbaseUtil.isNotBlank(value.getStatus());
        }
    }
}
