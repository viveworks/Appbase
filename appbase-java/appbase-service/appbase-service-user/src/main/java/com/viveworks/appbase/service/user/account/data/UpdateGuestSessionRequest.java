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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.user.session.data.UserSessionData;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
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
@UpdateGuestSessionRequest.Valid
public class UpdateGuestSessionRequest extends AbstractTraceable {
    private String appbaseRequesterId;

    private String appbaseUserId;

    private String appbaseSessionId;

    private String idpUserToken;

    private String name;

    private String languageCode;

    private UserSessionData userSessionData;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    /**
     * If this is true, then it is a [read+update] operation by the client. So, update all the fields as given.
     *
     * If this is false, then it is not a [read+update] operation by the client. It's just an update operation and
     * only the non-null properties must be updated. The client cares to update only the non-null properties.
     */
    private boolean readAndUpdate;

    public UpdateGuestSessionRequest() {
    }

    public UpdateGuestSessionRequest(
            final String traceId,
            final String appbaseRequesterId,
            final String appbaseUserId,
            final String appbaseSessionId,
            final String idpUserToken,
            final String name,
            final String languageCode,
            final UserSessionData userSessionData,
            final LocalDateTime lastAccessTimestamp,
            final boolean readAndUpdate)
    {
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.appbaseUserId = appbaseUserId;
        this.appbaseSessionId = appbaseSessionId;
        this.idpUserToken = idpUserToken;
        this.name = name;
        this.languageCode = languageCode;
        this.userSessionData = userSessionData;
        this.lastAccessTimestamp = lastAccessTimestamp;
        this.readAndUpdate = readAndUpdate;
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

    public String getIdpUserToken() {
        return idpUserToken;
    }

    public void setIdpUserToken(final String idpUserToken) {
        this.idpUserToken = idpUserToken;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    public UserSessionData getUserSessionData() {
        return userSessionData;
    }

    public void setUserSessionData(final UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    public LocalDateTime getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public boolean isReadAndUpdate() {
        return readAndUpdate;
    }

    public void setReadAndUpdate(final boolean readAndUpdate) {
        this.readAndUpdate = readAndUpdate;
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

        final UpdateGuestSessionRequest that = (UpdateGuestSessionRequest) o;
        return readAndUpdate == that.readAndUpdate &&
                Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(appbaseUserId, that.appbaseUserId) &&
                Objects.equals(appbaseSessionId, that.appbaseSessionId) &&
                Objects.equals(idpUserToken, that.idpUserToken) &&
                Objects.equals(name, that.name) &&
                Objects.equals(languageCode, that.languageCode) &&
                Objects.equals(userSessionData, that.userSessionData) &&
                Objects.equals(lastAccessTimestamp, that.lastAccessTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                appbaseRequesterId,
                appbaseUserId,
                appbaseSessionId,
                idpUserToken,
                name,
                languageCode,
                userSessionData,
                lastAccessTimestamp,
                readAndUpdate);
    }

    @Override
    public String toString() {
        return "UpdateGuestSessionRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", appbaseUserId='" + appbaseUserId + '\'' +
                ", appbaseSessionId='" + appbaseSessionId + '\'' +
                ", idpUserToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", name='" + name + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", userSessionData=" + userSessionData +
                ", lastAccessTimestamp=" + lastAccessTimestamp +
                ", readAndUpdate=" + readAndUpdate +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UpdateGuestSessionRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.UpdateGuestSessionRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<UpdateGuestSessionRequest.Valid, UpdateGuestSessionRequest>
    {
        @Override
        public boolean isValid(final UpdateGuestSessionRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseUserId()) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseSessionId());
        }
    }
}
