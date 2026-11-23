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

import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.user.session.data.UserSession;
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
@AfterIdpRefreshAuthenticationTokenRequest.Valid
public class AfterIdpRefreshAuthenticationTokenRequest extends AbstractTraceable {
    private String appbaseRequesterId;

    private UserSession currentUserSession;

    private String newIdpUserToken;

    public AfterIdpRefreshAuthenticationTokenRequest() {
    }

    public AfterIdpRefreshAuthenticationTokenRequest(
            final String traceId,
            final String appbaseRequesterId,
            final UserSession currentUserSession,
            final String newIdpUserToken)
    {
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.currentUserSession = currentUserSession;
        this.newIdpUserToken = newIdpUserToken;
    }

    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    public UserSession getCurrentUserSession() {
        return currentUserSession;
    }

    public void setCurrentUserSession(final UserSession currentUserSession) {
        this.currentUserSession = currentUserSession;
    }

    public String getNewIdpUserToken() {
        return newIdpUserToken;
    }

    public void setNewIdpUserToken(final String newIdpUserToken) {
        this.newIdpUserToken = newIdpUserToken;
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

        final AfterIdpRefreshAuthenticationTokenRequest that = (AfterIdpRefreshAuthenticationTokenRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(currentUserSession, that.currentUserSession) &&
                Objects.equals(newIdpUserToken, that.newIdpUserToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), appbaseRequesterId, currentUserSession, newIdpUserToken);
    }

    @Override
    public String toString() {
        return "AfterIdpRefreshAuthenticationTokenRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", currentUserSession=" + currentUserSession +
                ", newIdpUserToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = AfterIdpRefreshAuthenticationTokenRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.AfterIdpRefreshAuthenticationTokenRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<AfterIdpRefreshAuthenticationTokenRequest.Valid, AfterIdpRefreshAuthenticationTokenRequest>
    {
        @Override
        public boolean isValid(final AfterIdpRefreshAuthenticationTokenRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isValid(value.getCurrentUserSession(), new UserSession.Validator(), context) &&
                    AppbaseUtil.isNotBlank(value.getNewIdpUserToken());
        }
    }
}
