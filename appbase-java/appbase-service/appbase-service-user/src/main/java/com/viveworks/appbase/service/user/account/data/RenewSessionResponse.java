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
@RenewSessionResponse.Valid
public class RenewSessionResponse extends AbstractTraceable {
    private UserSession renewedUserSession;

    public RenewSessionResponse() {
    }

    public RenewSessionResponse(
            final String traceId,
            final UserSession renewedUserSession)
    {
        this.traceId = traceId;
        this.renewedUserSession = renewedUserSession;
    }

    public UserSession getRenewedUserSession() {
        return renewedUserSession;
    }

    public void setRenewedUserSession(final UserSession renewedUserSession) {
        this.renewedUserSession = renewedUserSession;
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

        final RenewSessionResponse that = (RenewSessionResponse) o;
        return Objects.equals(renewedUserSession, that.renewedUserSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), renewedUserSession);
    }

    @Override
    public String toString() {
        return "RenewSessionResponse{" +
                "renewedUserSession=" + renewedUserSession +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = RenewSessionResponse.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.RenewSessionResponse";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<RenewSessionResponse.Valid, RenewSessionResponse> {
        @Override
        public boolean isValid(final RenewSessionResponse value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotNull(value.getRenewedUserSession());
        }
    }
}
