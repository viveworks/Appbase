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

package com.viveworks.appbase.service.user.account.firebaseauth;

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
@FirebaseAuthCheckUserTokenRequest.Valid
public class FirebaseAuthCheckUserTokenRequest extends AbstractTraceable {
    @Nullable
    private String appbaseRequesterId;

    private String uid;

    private String email;

    private String token;

    public FirebaseAuthCheckUserTokenRequest() {
    }

    public FirebaseAuthCheckUserTokenRequest(
            @Nullable final String traceId,
            @Nullable final String appbaseRequesterId,
            final String uid,
            final String email,
            final String token)
    {
        super(traceId);

        this.appbaseRequesterId = appbaseRequesterId;
        this.uid = uid;
        this.email = email;
        this.token = token;
    }

    @Nullable
    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(@Nullable final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(final String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
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

        final FirebaseAuthCheckUserTokenRequest that = (FirebaseAuthCheckUserTokenRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(uid, that.uid) &&
                Objects.equals(email, that.email) &&
                Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), appbaseRequesterId, uid, email, token);
    }

    @Override
    public String toString() {
        return "FirebaseAuthCheckUserTokenRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", token='" + AppbaseUtil.OBFUSCATE + '\'' +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = FirebaseAuthCheckUserTokenRequest.Validator.class)
    public @interface Valid {
        String message() default
                "com.viveworks.appbase.service.user.account.firebaseauth.FirebaseAuthCheckUserTokenRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator
            <FirebaseAuthCheckUserTokenRequest.Valid, FirebaseAuthCheckUserTokenRequest>
    {
        @Override
        public boolean isValid(
                final FirebaseAuthCheckUserTokenRequest value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getUid()) &&
                    AppbaseUtil.isNotBlank(value.getEmail()) &&
                    AppbaseUtil.isNotBlank(value.getToken());
        }
    }
}
