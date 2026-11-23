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
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
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
@AfterIdpEmailPasswordSignInRequest.Valid
public class AfterIdpEmailPasswordSignInRequest extends AbstractTraceable {
    private String appbaseRequesterId;
    private String email;
    private PasswordHash clientPasswordHash;
    private String idpUserId;
    private String idpUserToken;
    private String languageCode;
    private String appName;
    private UserSessionData userSessionData;

    public AfterIdpEmailPasswordSignInRequest() {
    }

    public AfterIdpEmailPasswordSignInRequest(
            final String traceId,
            final String appbaseRequesterId,
            final String email,
            final PasswordHash clientPasswordHash,
            final String idpUserId,
            final String idpUserToken,
            final String appName,
            final String languageCode,
            final UserSessionData userSessionData)
    {
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.email = email;
        this.clientPasswordHash = clientPasswordHash;
        this.idpUserId = idpUserId;
        this.idpUserToken = idpUserToken;
        this.appName = appName;
        this.languageCode = languageCode;
        this.userSessionData = userSessionData;
    }

    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public PasswordHash getClientPasswordHash() {
        return clientPasswordHash;
    }

    public void setClientPasswordHash(final PasswordHash clientPasswordHash) {
        this.clientPasswordHash = clientPasswordHash;
    }

    public String getIdpUserId() {
        return idpUserId;
    }

    public void setIdpUserId(final String idpUserId) {
        this.idpUserId = idpUserId;
    }

    public String getIdpUserToken() {
        return idpUserToken;
    }

    public void setIdpUserToken(final String idpUserToken) {
        this.idpUserToken = idpUserToken;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(final String appName) {
        this.appName = appName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
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

        final AfterIdpEmailPasswordSignInRequest that = (AfterIdpEmailPasswordSignInRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(clientPasswordHash, that.clientPasswordHash) &&
                Objects.equals(idpUserId, that.idpUserId) &&
                Objects.equals(idpUserToken, that.idpUserToken) &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(languageCode, that.languageCode) &&
                Objects.equals(userSessionData, that.userSessionData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                appbaseRequesterId,
                email,
                clientPasswordHash,
                idpUserId,
                idpUserToken,
                languageCode,
                appName,
                userSessionData);
    }

    @Override
    public String toString() {
        return "AfterIdpEmailPasswordSignInRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", email='" + email + '\'' +
                ", clientPasswordHash='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", idpUserId='" + idpUserId + '\'' +
                ", idpUserToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", appName='" + appName + '\'' +
                ", userSessionData=" + userSessionData +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = AfterIdpEmailPasswordSignInRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<AfterIdpEmailPasswordSignInRequest.Valid, AfterIdpEmailPasswordSignInRequest>
    {
        @Override
        public boolean isValid(final AfterIdpEmailPasswordSignInRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getEmail()) &&
                    AppbaseUtil.isValid(value.getClientPasswordHash(), new PasswordHash.Validator(), context) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserId()) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserToken()) &&
                    AppbaseUtil.isNotBlank(value.getLanguageCode()) &&
                    AppbaseUtil.isNotBlank(value.getAppName());
        }
    }
}
