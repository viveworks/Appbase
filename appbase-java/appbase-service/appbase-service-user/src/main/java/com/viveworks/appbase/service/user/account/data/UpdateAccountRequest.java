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
import com.viveworks.appbase.service.password.PasswordHash;
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
@UpdateAccountRequest.Valid
public class UpdateAccountRequest extends AbstractTraceable {
    private String appbaseRequesterId;

    private String appbaseUserId;

    private String appbaseSessionId;

    private String email;

    private String name;

    private PasswordHash clientPasswordHash;

    private Boolean passwordResetEmailSent;

    private PasswordHash passwordResetCodeHash;

    private Boolean emailVerified;

    private String languageCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    private Boolean deleted;

    private String userTopic;

    private String userHomeDataCenter;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String stateProvince;

    private String postalCode;

    private String country;

    private String latitude;

    private String longitude;

    /**
     * If this is true, then it is a [read+update] operation by the client. So, update all the fields as given.
     *
     * If this is false, then it is *not* a [read+update] operation by the client. It's just an update operation and
     * only the non-null properties must be updated. The client cares to update only the non-null properties.
     */
    private boolean readAndUpdate;

    public UpdateAccountRequest() {
    }

    public UpdateAccountRequest(
            final String traceId,
            final String appbaseRequesterId,
            final String appbaseUserId,
            final String email,
            final String appbaseSessionId,
            final String name,
            final PasswordHash clientPasswordHash,
            final Boolean passwordResetEmailSent,
            final PasswordHash passwordResetCodeHash,
            final Boolean emailVerified,
            final String languageCode,
            final LocalDateTime lastAccessTimestamp,
            final Boolean deleted,
            final String userTopic,
            final String userHomeDataCenter,
            final boolean readAndUpdate)
    {
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.appbaseUserId = appbaseUserId;
        this.email = email;
        this.appbaseSessionId = appbaseSessionId;
        this.name = name;
        this.clientPasswordHash = clientPasswordHash;
        this.passwordResetEmailSent = passwordResetEmailSent;
        this.passwordResetCodeHash = passwordResetCodeHash;
        this.emailVerified = emailVerified;
        this.languageCode = languageCode;
        this.lastAccessTimestamp = lastAccessTimestamp;
        this.deleted = deleted;
        this.userTopic = userTopic;
        this.userHomeDataCenter = userHomeDataCenter;
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

    public PasswordHash getClientPasswordHash() {
        return clientPasswordHash;
    }

    public void setClientPasswordHash(PasswordHash clientPasswordHash) {
        this.clientPasswordHash = clientPasswordHash;
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

    public void setPasswordResetCodeHash(PasswordHash passwordResetCodeHash) {
        this.passwordResetCodeHash = passwordResetCodeHash;
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

    public LocalDateTime getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
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

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
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

        final UpdateAccountRequest that = (UpdateAccountRequest) o;
        return readAndUpdate == that.readAndUpdate &&
                Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(appbaseUserId, that.appbaseUserId) &&
                Objects.equals(appbaseSessionId, that.appbaseSessionId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(name, that.name) &&
                Objects.equals(clientPasswordHash, that.clientPasswordHash) &&
                Objects.equals(passwordResetEmailSent, that.passwordResetEmailSent) &&
                Objects.equals(passwordResetCodeHash, that.passwordResetCodeHash) &&
                Objects.equals(emailVerified, that.emailVerified) &&
                Objects.equals(languageCode, that.languageCode) &&
                Objects.equals(lastAccessTimestamp, that.lastAccessTimestamp) &&
                Objects.equals(deleted, that.deleted) &&
                Objects.equals(userTopic, that.userTopic) &&
                Objects.equals(userHomeDataCenter, that.userHomeDataCenter) &&
                Objects.equals(addressLine1, that.addressLine1) &&
                Objects.equals(addressLine2, that.addressLine2) &&
                Objects.equals(city, that.city) &&
                Objects.equals(stateProvince, that.stateProvince) &&
                Objects.equals(country, that.country) &&
                Objects.equals(postalCode, that.postalCode) &&
                Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                appbaseRequesterId,
                appbaseUserId,
                appbaseSessionId,
                email,
                name,
                clientPasswordHash,
                passwordResetEmailSent,
                passwordResetCodeHash,
                emailVerified,
                languageCode,
                lastAccessTimestamp,
                deleted,
                userTopic,
                userHomeDataCenter,
                addressLine1,
                addressLine2,
                city,
                stateProvince,
                country,
                postalCode,
                latitude,
                longitude,
                readAndUpdate);
    }

    @Override
    public String toString() {
        return "UpdateAccountRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", appbaseUserId='" + appbaseUserId + '\'' +
                ", appbaseSessionId='" + appbaseSessionId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", clientPasswordHash=" + AppbaseUtil.OBFUSCATE +
                ", passwordResetEmailSent=" + passwordResetEmailSent +
                ", passwordResetCode='" + passwordResetCodeHash + '\'' +
                ", emailVerified=" + emailVerified +
                ", languageCode='" + languageCode + '\'' +
                ", lastAccessTimestamp=" + lastAccessTimestamp +
                ", deleted=" + deleted +
                ", userTopic='" + userTopic + '\'' +
                ", userHomeDataCenter='" + userHomeDataCenter + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", city='" + city + '\'' +
                ", stateProvince='" + stateProvince + '\'' +
                ", country='" + country + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", readAndUpdate=" + readAndUpdate +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UpdateAccountRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.UpdateAccountRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<UpdateAccountRequest.Valid, UpdateAccountRequest>
    {
        @Override
        public boolean isValid(final UpdateAccountRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseUserId());
        }
    }
}
