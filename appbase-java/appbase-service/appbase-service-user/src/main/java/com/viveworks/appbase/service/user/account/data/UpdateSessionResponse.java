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
@UpdateSessionResponse.Valid
public class UpdateSessionResponse extends AbstractTraceable {
    private UserSession userSession;

    public UpdateSessionResponse() {
    }

    public UpdateSessionResponse(
            final String traceId,
            final UserSession userSession)
    {
        this.traceId = traceId;
        this.userSession = userSession;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(final UserSession userSession) {
        this.userSession = userSession;
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

        final UpdateSessionResponse that = (UpdateSessionResponse) o;
        return Objects.equals(userSession, that.userSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userSession);
    }

    @Override
    public String toString() {
        return "UpdateSessionResponse{" +
                "userSession=" + userSession +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UpdateSessionResponse.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.UpdateSessionResponse";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<UpdateSessionResponse.Valid, UpdateSessionResponse>
    {
        @Override
        public boolean isValid(final UpdateSessionResponse value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isValid(value.getUserSession(), new UserSession.Validator(), context);
        }
    }
}
