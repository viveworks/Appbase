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
@AfterIdpSignOutRequest.Valid
public class AfterIdpSignOutRequest extends AbstractTraceable {
    private String appbaseRequesterId;

    private String appbaseUserId;

    private String idpUserId;

    private String idpUserToken;

    private String appbaseSessionId;

    public AfterIdpSignOutRequest() {
    }

    public AfterIdpSignOutRequest(
            final String traceId,
            final String appbaseRequesterId,
            final String appbaseUserId,
            final String idpUserId,
            final String idpUserToken,
            final String appbaseSessionId)
    {
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.appbaseUserId = appbaseUserId;
        this.idpUserId = idpUserId;
        this.idpUserToken = idpUserToken;
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

    public String getIdpUserId() {
        return idpUserId;
    }

    public void setIdpUserId(String idpUserId) {
        this.idpUserId = idpUserId;
    }

    public String getIdpUserToken() {
        return idpUserToken;
    }

    public void setIdpUserToken(String idpUserToken) {
        this.idpUserToken = idpUserToken;
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

        final AfterIdpSignOutRequest that = (AfterIdpSignOutRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(appbaseUserId, that.appbaseUserId) &&
                Objects.equals(idpUserId, that.idpUserId) &&
                Objects.equals(idpUserToken, that.idpUserToken) &&
                Objects.equals(appbaseSessionId, that.appbaseSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(), appbaseRequesterId, appbaseUserId, idpUserId, idpUserToken, appbaseSessionId);
    }

    @Override
    public String toString() {
        return "AfterIdpSignOutRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", appbaseUserId='" + appbaseUserId + '\'' +
                ", idpUserId='" + idpUserId + '\'' +
                ", idpUserToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", appbaseSessionId='" + appbaseSessionId + '\'' +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = AfterIdpSignOutRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.AfterIdpSignOutRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<AfterIdpSignOutRequest.Valid, AfterIdpSignOutRequest> {
        @Override
        public boolean isValid(final AfterIdpSignOutRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseUserId()) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserId()) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserToken()) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseSessionId());
        }
    }
}
