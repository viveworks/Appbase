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
@SyncIdpUserRequest.Valid
public class SyncIdpUserRequest extends AbstractTraceable {
    private String appbaseRequesterId;
    private String email;
    private PasswordHash clientPasswordHash;
    private String idpUserId;
    private String idpUserToken;
    private String appName;
    private String languageCode;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    private String latitude;
    private String longitude;
    private UserSessionData userSessionData;

    public SyncIdpUserRequest() {
    }

    public SyncIdpUserRequest(
            @Nullable final String traceId,
            final String appbaseRequesterId,
            final String email,
            final PasswordHash clientPasswordHash,
            final String idpUserId,
            final String idpUserToken,
            final String appName,
            final String languageCode,
            final String addressLine1,
            final String addressLine2,
            final String city,
            final String stateProvince,
            final String postalCode,
            final String country,
            final String latitude,
            final String longitude,
            final UserSessionData userSessionData)
    {
        super(traceId);

        this.appbaseRequesterId = appbaseRequesterId;
        this.email = email;
        this.clientPasswordHash = clientPasswordHash;
        this.idpUserId = idpUserId;
        this.idpUserToken = idpUserToken;
        this.appName = appName;
        this.languageCode = languageCode;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.stateProvince = stateProvince;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public UserSessionData getUserSessionData() {
        return userSessionData;
    }

    public void setUserSessionData(final UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        final SyncIdpUserRequest that = (SyncIdpUserRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(clientPasswordHash, that.clientPasswordHash) &&
                Objects.equals(idpUserId, that.idpUserId) &&
                Objects.equals(idpUserToken, that.idpUserToken) &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(languageCode, that.languageCode) &&
                Objects.equals(addressLine1, that.addressLine1) &&
                Objects.equals(addressLine2, that.addressLine2) &&
                Objects.equals(city, that.city) &&
                Objects.equals(country, that.country) &&
                Objects.equals(postalCode, that.postalCode) &&
                Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude) &&
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
                appName,
                languageCode,
                addressLine1,
                addressLine2,
                city,
                country,
                stateProvince,
                postalCode,
                latitude,
                longitude,
                userSessionData);
    }

    @Override
    public String toString() {
        return "SyncIdpUserRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", email='" + email + '\'' +
                ", clientPasswordHash=" + AppbaseUtil.OBFUSCATE +
                ", idpUserId='" + idpUserId + '\'' +
                ", idpUserToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", appName='" + appName + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", city='" + city + '\'' +
                ", stateProvince='" + stateProvince + '\'' +
                ", country='" + country + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", userSessionData=" + userSessionData +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = SyncIdpUserRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.SyncIdpUserRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements
            ConstraintValidator<SyncIdpUserRequest.Valid, SyncIdpUserRequest>
    {
        @Override
        public boolean isValid(final SyncIdpUserRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getEmail()) &&
                    AppbaseUtil.isValid(value.getClientPasswordHash(), new PasswordHash.Validator(), context) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserId()) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserToken()) &&
                    AppbaseUtil.isNotBlank(value.getAppName()) &&
                    AppbaseUtil.isNotBlank(value.getLanguageCode());
        }
    }
}
