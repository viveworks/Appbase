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
 * Signed-in account-maintenance callback for recording reset-email state. It requires an IdP
 * token for the target account; it is not a signed-out forgot-password send operation. The legacy
 * four-argument constructor remains for source compatibility, but callers must set the token
 * before dispatch.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@AfterIdpPasswordResetEmailSentRequest.Valid
public class AfterIdpPasswordResetEmailSentRequest extends AbstractTraceable {
    @Nullable
    private String appbaseRequesterId;
    private String email;
    private String idpUserToken;
    private Boolean passwordResetEmailSent;

    public AfterIdpPasswordResetEmailSentRequest() {
    }

    public AfterIdpPasswordResetEmailSentRequest(
            @Nullable final String traceId,
            @Nullable final String appbaseRequesterId,
            final String email,
            final Boolean passwordResetEmailSent)
    {
        super(traceId);
        this.appbaseRequesterId = appbaseRequesterId;
        this.email = email;
        this.passwordResetEmailSent = passwordResetEmailSent;
    }

    public AfterIdpPasswordResetEmailSentRequest(
            @Nullable final String traceId,
            @Nullable final String appbaseRequesterId,
            final String email,
            final String idpUserToken,
            final Boolean passwordResetEmailSent)
    {
        this(traceId, appbaseRequesterId, email, passwordResetEmailSent);
        this.idpUserToken = idpUserToken;
    }

    @Nullable
    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(@Nullable final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getIdpUserToken() {
        return idpUserToken;
    }

    public void setIdpUserToken(final String idpUserToken) {
        this.idpUserToken = idpUserToken;
    }

    public Boolean getPasswordResetEmailSent() {
        return passwordResetEmailSent;
    }

    public void setPasswordResetEmailSent(final Boolean passwordResetEmailSent) {
        this.passwordResetEmailSent = passwordResetEmailSent;
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

        final AfterIdpPasswordResetEmailSentRequest that = (AfterIdpPasswordResetEmailSentRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(idpUserToken, that.idpUserToken) &&
                Objects.equals(passwordResetEmailSent, that.passwordResetEmailSent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), appbaseRequesterId, email, idpUserToken, passwordResetEmailSent);
    }

    @Override
    public String toString() {
        return "AfterIdpPasswordResetEmailSentRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", email='" + email + '\'' +
                ", passwordResetEmailSent=" + passwordResetEmailSent +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = AfterIdpPasswordResetEmailSentRequest.Validator.class)
    public @interface Valid {
        String message() default
                "com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetEmailSentRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator
            <AfterIdpPasswordResetEmailSentRequest.Valid, AfterIdpPasswordResetEmailSentRequest>
    {
        @Override
        public boolean isValid(
                final AfterIdpPasswordResetEmailSentRequest value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getEmail()) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserToken()) &&
                    AppbaseUtil.isNotNull(value.getPasswordResetEmailSent());
        }
    }
}
