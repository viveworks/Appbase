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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

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
@UserAccount.Valid
public class UserAccount {
    @ColumnName("appbaseUserId")
    private String appbaseUserId;

    @ColumnName("idpUserId")
    private String idpUserId;

    @ColumnName("email")
    private String email;

    @ColumnName("name")
    private String name;

    @ColumnName("passwordHashes")
    private PasswordHashes passwordHashes;

    @ColumnName("passwordResetEmailSent")
    private Boolean passwordResetEmailSent;

    @ColumnName("passwordResetCodeHash")
    private PasswordHash passwordResetCodeHash;

    @ColumnName("userTopic")
    private String userTopic;

    @ColumnName("userHomeDataCenter")
    private String userHomeDataCenter;

    @ColumnName("emailVerified")
    private Boolean emailVerified;

    @ColumnName("languageCode")
    private String languageCode;

    @ColumnName("addressLine1")
    private String addressLine1;

    @ColumnName("addressLine2")
    private String addressLine2;

    @ColumnName("city")
    private String city;

    @ColumnName("stateProvince")
    private String stateProvince;

    @ColumnName("postalCode")
    private String postalCode;

    @ColumnName("country")
    private String country;

    @ColumnName("latitude")
    private String latitude;

    @ColumnName("longitude")
    private String longitude;

    @ColumnName("deleted")
    private Boolean deleted;

    @ColumnName("creationTimestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime creationTimestamp;

    @ColumnName("lastAccessTimestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    public UserAccount() {
    }

    @JsonCreator
    public UserAccount(
            @JsonProperty("appbaseUserId")
            final String appbaseUserId,
            @JsonProperty("idpUserId")
            final String idpUserId,
            @JsonProperty("email")
            final String email,
            @JsonProperty("name")
            final String name,
            @JsonProperty("passwordHashes")
            final PasswordHashes passwordHashes,
            @JsonProperty("passwordResetEmailSent")
            final Boolean passwordResetEmailSent,
            @JsonProperty("passwordResetCodeHash")
            final PasswordHash passwordResetCodeHash,
            @JsonProperty("userTopic")
            final String userTopic,
            @JsonProperty("userHomeDataCenter")
            final String userHomeDataCenter,
            @JsonProperty("emailVerified")
            final Boolean emailVerified,
            @JsonProperty("languageCode")
            final String languageCode,
            @JsonProperty("addressLine1")
            final String addressLine1,
            @JsonProperty("addressLine2")
            final String addressLine2,
            @JsonProperty("city")
            final String city,
            @JsonProperty("stateProvince")
            final String stateProvince,
            @JsonProperty("postalCode")
            final String postalCode,
            @JsonProperty("country")
            final String country,
            @JsonProperty("latitude")
            final String latitude,
            @JsonProperty("longitude")
            final String longitude,
            @JsonProperty("deleted")
            final Boolean deleted,
            @JsonProperty("creationTimestamp")
            final LocalDateTime creationTimestamp,
            @JsonProperty("lastAccessTimestamp")
            final LocalDateTime lastAccessTimestamp)
    {
        this.appbaseUserId = appbaseUserId;
        this.idpUserId = idpUserId;
        this.email = email;
        this.name = name;
        this.passwordHashes = passwordHashes;
        this.passwordResetEmailSent = passwordResetEmailSent;
        this.passwordResetCodeHash = passwordResetCodeHash;
        this.userTopic = userTopic;
        this.userHomeDataCenter = userHomeDataCenter;
        this.emailVerified = emailVerified;
        this.languageCode = languageCode;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.stateProvince = stateProvince;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deleted = deleted;
        this.creationTimestamp = creationTimestamp;
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public UserAccount(final UserAccount copy) {
        AppbaseUtil.notNull(copy, "copy");

        setAppbaseUserId(copy.appbaseUserId);
        setIdpUserId(copy.idpUserId);
        setEmail(copy.email);
        setName(copy.name);
        setPasswordHashes(copy.passwordHashes);
        setPasswordResetEmailSent(copy.passwordResetEmailSent);
        setPasswordResetCodeHash(copy.passwordResetCodeHash);
        setUserTopic(copy.userTopic);
        setUserHomeDataCenter(copy.userHomeDataCenter);
        setEmailVerified(copy.emailVerified);
        setLanguageCode(copy.languageCode);
        setAddressLine1(copy.addressLine1);
        setAddressLine2(copy.addressLine2);
        setCity(copy.city);
        setStateProvince(copy.stateProvince);
        setPostalCode(copy.postalCode);
        setCountry(copy.country);
        setLatitude(copy.latitude);
        setLongitude(copy.longitude);
        setDeleted(copy.deleted);
        setCreationTimestamp(copy.creationTimestamp);
        setLastAccessTimestamp(copy.lastAccessTimestamp);
    }

    public UserAccount(final UserAccount copy, final PasswordHashes overridePasswordHashes) {
        AppbaseUtil.notNull(copy, "copy");

        setAppbaseUserId(copy.appbaseUserId);
        setIdpUserId(copy.idpUserId);
        setEmail(copy.email);
        setName(copy.name);
        setPasswordHashes(overridePasswordHashes);
        setPasswordResetEmailSent(copy.passwordResetEmailSent);
        setPasswordResetCodeHash(copy.passwordResetCodeHash);
        setUserTopic(copy.userTopic);
        setUserHomeDataCenter(copy.userHomeDataCenter);
        setEmailVerified(copy.emailVerified);
        setAddressLine1(copy.addressLine1);
        setAddressLine2(copy.addressLine2);
        setCity(copy.city);
        setStateProvince(copy.stateProvince);
        setPostalCode(copy.postalCode);
        setCountry(copy.country);
        setLatitude(copy.latitude);
        setLongitude(copy.longitude);
        setLanguageCode(copy.languageCode);
        setDeleted(copy.deleted);
        setCreationTimestamp(copy.creationTimestamp);
        setLastAccessTimestamp(copy.lastAccessTimestamp);
    }

    public UserAccount(
            final UserAccount copy,
            final Boolean passwordResetEmailSent,
            final PasswordHash passwordResetCodeHash,
            final PasswordHashes newPasswordHashes)
    {
        AppbaseUtil.notNull(copy, "copy");

        setAppbaseUserId(copy.appbaseUserId);
        setIdpUserId(copy.idpUserId);
        setEmail(copy.email);
        setName(copy.name);
        setPasswordHashes(newPasswordHashes);
        setPasswordResetEmailSent(passwordResetEmailSent);
        setPasswordResetCodeHash(passwordResetCodeHash);
        setUserTopic(copy.userTopic);
        setUserHomeDataCenter(copy.userHomeDataCenter);
        setEmailVerified(copy.emailVerified);
        setLanguageCode(copy.languageCode);
        setAddressLine1(copy.addressLine1);
        setAddressLine2(copy.addressLine2);
        setCity(copy.city);
        setStateProvince(copy.stateProvince);
        setPostalCode(copy.postalCode);
        setCountry(copy.country);
        setLatitude(copy.latitude);
        setLongitude(copy.longitude);
        setDeleted(copy.deleted);
        setCreationTimestamp(copy.creationTimestamp);
        setLastAccessTimestamp(copy.lastAccessTimestamp);
    }

    private UserAccount(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        setAppbaseUserId(builder.appbaseUserId);
        setIdpUserId(builder.idpUserId);
        setEmail(builder.email);
        setName(builder.name);
        setPasswordHashes(builder.passwordHashes);
        setPasswordResetEmailSent(builder.passwordResetEmailSent);
        setPasswordResetCodeHash(builder.passwordResetCodeHash);
        setUserTopic(builder.userTopic);
        setUserHomeDataCenter(builder.userHomeDataCenter);
        setEmailVerified(builder.emailVerified);
        setLanguageCode(builder.languageCode);
        setAddressLine1(builder.addressLine1);
        setAddressLine2(builder.addressLine2);
        setCity(builder.city);
        setStateProvince(builder.stateProvince);
        setPostalCode(builder.postalCode);
        setCountry(builder.country);
        setLatitude(builder.latitude);
        setLongitude(builder.longitude);
        setDeleted(builder.deleted);
        setCreationTimestamp(builder.creationTimestamp);
        setLastAccessTimestamp(builder.lastAccessTimestamp);
    }

    public static Builder builder() {
        return new Builder();
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

    public void setIdpUserId(final String idpUserId) {
        this.idpUserId = idpUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public PasswordHashes getPasswordHashes() {
        return passwordHashes;
    }

    public void setPasswordHashes(final PasswordHashes passwordHashes) {
        this.passwordHashes = passwordHashes;
    }

    public Boolean getPasswordResetEmailSent() {
        return passwordResetEmailSent;
    }

    public void setPasswordResetEmailSent(final Boolean passwordResetEmailSent) {
        this.passwordResetEmailSent = passwordResetEmailSent;
    }

    public PasswordHash getPasswordResetCodeHash() {
        return passwordResetCodeHash;
    }

    public void setPasswordResetCodeHash(final PasswordHash passwordResetCodeHash) {
        this.passwordResetCodeHash = passwordResetCodeHash;
    }

    public String getUserTopic() {
        return userTopic;
    }

    public void setUserTopic(final String userTopic) {
        this.userTopic = userTopic;
    }

    public String getUserHomeDataCenter() {
        return userHomeDataCenter;
    }

    public void setUserHomeDataCenter(final String userHomeDataCenter) {
        this.userHomeDataCenter = userHomeDataCenter;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(final Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(final String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(final String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(final String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(final String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(final String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(final String longitude) {
        this.longitude = longitude;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UserAccount userAccount = (UserAccount) o;
        return Objects.equals(appbaseUserId, userAccount.appbaseUserId) &&
                Objects.equals(idpUserId, userAccount.idpUserId) &&
                Objects.equals(email, userAccount.email) &&
                Objects.equals(name, userAccount.name) &&
                Objects.equals(passwordHashes, userAccount.passwordHashes) &&
                Objects.equals(passwordResetEmailSent, userAccount.passwordResetEmailSent) &&
                Objects.equals(passwordResetCodeHash, userAccount.passwordResetCodeHash) &&
                Objects.equals(userTopic, userAccount.userTopic) &&
                Objects.equals(userHomeDataCenter, userAccount.userHomeDataCenter) &&
                Objects.equals(emailVerified, userAccount.emailVerified) &&
                Objects.equals(languageCode, userAccount.languageCode) &&
                Objects.equals(addressLine1, userAccount.addressLine1) &&
                Objects.equals(addressLine2, userAccount.addressLine2) &&
                Objects.equals(city, userAccount.city) &&
                Objects.equals(stateProvince, userAccount.stateProvince) &&
                Objects.equals(postalCode, userAccount.postalCode) &&
                Objects.equals(country, userAccount.country) &&
                Objects.equals(latitude, userAccount.latitude) &&
                Objects.equals(longitude, userAccount.longitude) &&
                Objects.equals(deleted, userAccount.deleted) &&
                Objects.equals(creationTimestamp, userAccount.creationTimestamp) &&
                Objects.equals(lastAccessTimestamp, userAccount.lastAccessTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                appbaseUserId,
                idpUserId,
                email,
                name,
                passwordHashes,
                passwordResetEmailSent,
                passwordResetCodeHash,
                userTopic,
                userHomeDataCenter,
                emailVerified,
                languageCode,
                addressLine1,
                addressLine2,
                city,
                stateProvince,
                postalCode,
                country,
                latitude,
                longitude,
                deleted,
                creationTimestamp,
                lastAccessTimestamp);
    }

    @Override
    public String toString() {
        return "UserAccount{" +
                "appbaseUserId='" + appbaseUserId + '\'' +
                ", idpUserId='" + idpUserId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", passwordHashes=" + AppbaseUtil.OBFUSCATE +
                ", passwordResetEmailSent=" + passwordResetEmailSent +
                ", passwordResetCodeHash=" + AppbaseUtil.OBFUSCATE +
                ", userTopic='" + userTopic + '\'' +
                ", userHomeDataCenter='" + userHomeDataCenter + '\'' +
                ", emailVerified=" + emailVerified +
                ", languageCode='" + languageCode + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", city='" + city + '\'' +
                ", stateProvince='" + stateProvince + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude'=" + longitude + '\'' +
                ", deleted=" + deleted +
                ", creationTimestamp=" + creationTimestamp +
                ", lastAccessTimestamp=" + lastAccessTimestamp +
                '}';
    }

    public static final class Builder {
        private String appbaseUserId;
        private String idpUserId;
        private String email;
        private String name;
        private PasswordHashes passwordHashes;
        private Boolean passwordResetEmailSent;
        private PasswordHash passwordResetCodeHash;
        private String userTopic;
        private String userHomeDataCenter;
        private Boolean emailVerified;
        private String languageCode;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String stateProvince;
        private String postalCode;
        private String country;
        private String latitude;
        private String longitude;
        private Boolean deleted;
        private LocalDateTime creationTimestamp;
        private LocalDateTime lastAccessTimestamp;

        private Builder() {
        }

        public Builder appbaseUserId(final String appbaseUserId) {
            this.appbaseUserId = appbaseUserId;
            return this;
        }

        public Builder idpUserId(final String idpUserId) {
            this.idpUserId = idpUserId;
            return this;
        }

        public Builder email(final String email) {
            this.email = email;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder passwordHashes(final PasswordHashes passwordHashes) {
            this.passwordHashes = passwordHashes;
            return this;
        }

        public Builder passwordResetEmailSent(final Boolean passwordResetEmailSent) {
            this.passwordResetEmailSent = passwordResetEmailSent;
            return this;
        }

        public Builder passwordResetCode(final PasswordHash passwordResetCodeHash) {
            this.passwordResetCodeHash = passwordResetCodeHash;
            return this;
        }

        public Builder userTopic(final String userTopic) {
            this.userTopic = userTopic;
            return this;
        }

        public Builder userHomeDataCenter(final String userHomeDataCenter) {
            this.userHomeDataCenter = userHomeDataCenter;
            return this;
        }

        public Builder emailVerified(final Boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Builder languageCode(final String languageCode) {
            this.languageCode = languageCode;
            return this;
        }

        public Builder addressLine1(final String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        public Builder addressLine2(final String addressLine2) {
            this.addressLine2 = addressLine2;
            return this;
        }

        public Builder city(final String city) {
            this.city = city;
            return this;
        }

        public Builder stateProvince(final String stateProvince) {
            this.stateProvince = stateProvince;
            return this;
        }

        public Builder postalCode(final String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder country(final String country) {
            this.country = country;
            return this;
        }

        public Builder latitude(final String latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(final String longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder deleted(final Boolean deleted) {
            this.deleted = deleted;
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

        public UserAccount build() {
            return new UserAccount(this);
        }
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UserAccount.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.UserAccount";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<UserAccount.Valid, UserAccount> {
        @Override
        public boolean isValid(
                final UserAccount userAccount,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(userAccount) &&
                    AppbaseUtil.isNotBlank(userAccount.getAppbaseUserId()) &&
                    AppbaseUtil.isNotBlank(userAccount.getIdpUserId()) &&
                    AppbaseUtil.isNotBlank(userAccount.getEmail()) &&
                    AppbaseUtil.isNotNull(userAccount.getPasswordResetEmailSent()) &&
                    AppbaseUtil.isNotBlank(userAccount.getName()) &&
                    AppbaseUtil.isValid(userAccount.getPasswordHashes(), new PasswordHashes.Validator(), context) &&
                    AppbaseUtil.isNotBlank(userAccount.getLanguageCode()) &&
                    AppbaseUtil.isNotNull(userAccount.getCreationTimestamp()) &&
                    AppbaseUtil.isNotNull(userAccount.getLastAccessTimestamp()) &&
                    AppbaseUtil.isNotNull(userAccount.getDeleted()) &&
                    AppbaseUtil.isNotNull(userAccount.getEmailVerified()) &&
                    AppbaseUtil.isNotNull(userAccount.getPasswordResetEmailSent()) &&
                    AppbaseUtil.isNotNull(userAccount.getUserHomeDataCenter()) &&
                    AppbaseUtil.isNotNull(userAccount.getUserTopic());
        }
    }
}
