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

package com.viveworks.appbase.service.user.session.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.user.account.data.GuestUser;
import com.viveworks.appbase.service.user.account.data.UserAccount;
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
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This class is a {@code DTO} that is used in other parts of the application is used to store
 * session information in a session store).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserSession {
    private String appbaseSessionId;

    private boolean authenticated;

    private String idpUserToken;

    private UserAccount userAccount;

    private GuestUser guestUser;

    private UserSessionData userSessionData;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime creationTimestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    private boolean expired;

    private boolean newSession;

    public UserSession() {
    }

    public UserSession(final UserSession copy) {
        AppbaseUtil.notNull(copy, "copy");

        setAppbaseSessionId(copy.appbaseSessionId);
        setAuthenticated(copy.authenticated);
        setIdpUserToken(copy.idpUserToken);
        setUserAccount(copy.userAccount);
        setGuestUser(copy.guestUser);
        setUserSessionData(copy.userSessionData);
        setCreationTimestamp(copy.creationTimestamp);
        setLastAccessTimestamp(copy.lastAccessTimestamp);
        setExpired(copy.expired);
        setNewSession(copy.newSession);
    }

    public UserSession(final UserSession copy, final UserAccount overrideUserAccount) {
        AppbaseUtil.notNull(copy, "copy");

        setAppbaseSessionId(copy.appbaseSessionId);
        setAuthenticated(copy.authenticated);
        setIdpUserToken(copy.idpUserToken);
        setUserAccount(overrideUserAccount);
        setGuestUser(copy.guestUser);
        setUserSessionData(copy.userSessionData);
        setCreationTimestamp(copy.creationTimestamp);
        setLastAccessTimestamp(copy.lastAccessTimestamp);
        setExpired(copy.expired);
        setNewSession(copy.newSession);
    }

    private UserSession(final Builder builder) {
        setAppbaseSessionId(builder.appbaseSessionId);
        setAuthenticated(builder.authenticated);
        setIdpUserToken(builder.idpUserToken);
        setUserAccount(builder.userAccount);
        setGuestUser(builder.guestUser);
        setUserSessionData(builder.userSessionData);
        setCreationTimestamp(builder.creationTimestamp);
        setLastAccessTimestamp(builder.lastAccessTimestamp);
        setExpired(builder.expired);
        setNewSession(builder.newSession);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAppbaseSessionId() {
        return appbaseSessionId;
    }

    public void setAppbaseSessionId(final String appbaseSessionId) {
        this.appbaseSessionId = appbaseSessionId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(final boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getIdpUserToken() {
        return idpUserToken;
    }

    public void setIdpUserToken(final String idpUserToken) {
        this.idpUserToken = idpUserToken;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(final UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public GuestUser getGuestUser() {
        return guestUser;
    }

    public void setGuestUser(final GuestUser guestUser) {
        this.guestUser = guestUser;
    }

    public UserSessionData getUserSessionData() {
        return userSessionData;
    }

    public void setUserSessionData(final UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(final LocalDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp != null ?
                AppbaseServiceUtil.truncateToSeconds(creationTimestamp) :
                null;
    }

    public LocalDateTime getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp != null ?
                AppbaseServiceUtil.truncateToSeconds(lastAccessTimestamp) :
                null;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(final boolean expired) {
        this.expired = expired;
    }

    public boolean isNewSession() {
        return newSession;
    }

    public void setNewSession(boolean newSession) {
        this.newSession = newSession;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UserSession that = (UserSession) o;
        return authenticated == that.authenticated &&
                expired == that.expired &&
                newSession == that.newSession &&
                Objects.equals(appbaseSessionId, that.appbaseSessionId) &&
                Objects.equals(idpUserToken, that.idpUserToken) &&
                Objects.equals(userAccount, that.userAccount) &&
                Objects.equals(guestUser, that.guestUser) &&
                Objects.equals(userSessionData, that.userSessionData) &&
                Objects.equals(creationTimestamp, that.creationTimestamp) &&
                Objects.equals(lastAccessTimestamp, that.lastAccessTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                appbaseSessionId,
                authenticated,
                idpUserToken,
                userAccount,
                guestUser,
                userSessionData,
                creationTimestamp,
                lastAccessTimestamp,
                expired,
                newSession);
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "appbaseSessionId='" + appbaseSessionId + '\'' +
                ", authenticated=" + authenticated +
                ", idpUserToken='" + idpUserToken + '\'' +
                ", userAccount=" + userAccount +
                ", guestUser=" + guestUser +
                ", userSessionData=" + userSessionData +
                ", creationTimestamp=" + creationTimestamp +
                ", lastAccessTimestamp=" + lastAccessTimestamp +
                ", expired=" + expired +
                ", newSession=" + newSession +
                '}';
    }

    public static final class Builder {
        private String appbaseSessionId;
        private boolean authenticated;
        private String idpUserToken;
        private UserAccount userAccount;
        private GuestUser guestUser;
        private UserSessionData userSessionData;
        private LocalDateTime creationTimestamp;
        private LocalDateTime lastAccessTimestamp;
        private Boolean expired;
        private boolean newSession;

        private Builder() {
        }

        public Builder appbaseSessionId(final String appbaseSessionId) {
            this.appbaseSessionId = appbaseSessionId;
            return this;
        }

        public Builder authenticated(final boolean authenticated) {
            this.authenticated = authenticated;
            return this;
        }

        public Builder idpUserToken(final String idpUserToken) {
            this.idpUserToken = idpUserToken;
            return this;
        }

        public Builder userAccount(final UserAccount userAccount) {
            this.userAccount = userAccount;
            return this;
        }

        public Builder guestUser(final GuestUser guestUser) {
            this.guestUser = guestUser;
            return this;
        }

        public Builder userSessionData(final UserSessionData userSessionData) {
            this.userSessionData = userSessionData;
            return this;
        }

        public Builder creationTimestamp(final LocalDateTime creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public Builder lastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
            this.lastAccessTimestamp = lastAccessTimestamp;
            return this;
        }

        public Builder expired(final Boolean expired) {
            this.expired = expired;
            return this;
        }

        public Builder newSession(final boolean newSession) {
            this.newSession = newSession;
            return this;
        }

        public UserSession build() {
            return new UserSession(this);
        }
    }

    @Target({ METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UserSession.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.session.data.UserSession";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<UserSession.Valid, UserSession> {
        @Override
        public boolean isValid(final UserSession value, final ConstraintValidatorContext context) {
            if (AppbaseUtil.isNull(value)) {
                return false;
            }

            if (value.getUserAccount() != null) {
                return AppbaseUtil.isNotBlank(value.getAppbaseSessionId()) &&
                        AppbaseUtil.isNotBlank(value.getIdpUserToken()) &&
                        AppbaseUtil.isValid(value.getUserAccount(), new UserAccount.Validator(), context) &&
                        AppbaseUtil.isValid(value.getUserSessionData(), new UserSessionData.Validator(), context) &&
                        AppbaseUtil.isNotNull(value.getCreationTimestamp()) &&
                        AppbaseUtil.isNotNull(value.getLastAccessTimestamp());
            }

            if (value.guestUser != null) {
                return AppbaseUtil.isNotBlank(value.getAppbaseSessionId()) &&
                        AppbaseUtil.isValid(value.getGuestUser(), new GuestUser.Validator(), context) &&
                        AppbaseUtil.isValid(value.getUserSessionData(), new UserSessionData.Validator(), context) &&
                        AppbaseUtil.isNotNull(value.getCreationTimestamp()) &&
                        AppbaseUtil.isNotNull(value.getLastAccessTimestamp());
            }

            return AppbaseUtil.isNotBlank(value.getAppbaseSessionId()) &&
                    AppbaseUtil.isValid(value.getUserSessionData(), new UserSessionData.Validator(), context) &&
                    AppbaseUtil.isNotNull(value.getCreationTimestamp()) &&
                    AppbaseUtil.isNotNull(value.getLastAccessTimestamp());
        }
    }
}
