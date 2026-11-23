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
import com.viveworks.appbase.service.user.session.data.UserSessionData;
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
@FirebaseAuthCreateRequest.Valid
public class FirebaseAuthCreateRequest extends AbstractTraceable {
    @Nullable
    private String appbaseRequesterId;
    private String name;
    private String email;
    private String password;
    private String languageCode;
    private String appName;
    private UserSessionData userSessionData;

    public FirebaseAuthCreateRequest() {
    }

    public FirebaseAuthCreateRequest(
            @Nullable final String traceId,
            @Nullable final String appbaseRequesterId,
            final String name,
            final String email,
            final String password,
            final String languageCode,
            final String appName,
            final UserSessionData userSessionData)
    {
        super(traceId);

        this.appbaseRequesterId = appbaseRequesterId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.languageCode = languageCode;
        this.appName = appName;
        this.userSessionData = userSessionData;
    }

    @Nullable
    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(@Nullable final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public UserSessionData getUserSessionData() {
        return userSessionData;
    }

    public void setUserSessionData(final UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
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

        final FirebaseAuthCreateRequest that = (FirebaseAuthCreateRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                Objects.equals(password, that.password) &&
                Objects.equals(languageCode, that.languageCode) &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(userSessionData, that.userSessionData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                appbaseRequesterId,
                name,
                email,
                password,
                languageCode,
                appName,
                userSessionData);
    }

    @Override
    public String toString() {
        return "FirebaseAuthCreateRequest{" +
                ", appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", displayName='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", appName='" + appName + '\'' +
                ", userSessionData='" + userSessionData + '\'' +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = FirebaseAuthCreateRequest.Validator.class)
    public @interface Valid {
        String message() default
                "com.viveworks.appbase.service.user.account.firebaseauth.FirebaseAuthCreateRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator
            <FirebaseAuthCreateRequest.Valid, FirebaseAuthCreateRequest>
    {
        @Override
        public boolean isValid(
                final FirebaseAuthCreateRequest value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value);
        }
    }
}
