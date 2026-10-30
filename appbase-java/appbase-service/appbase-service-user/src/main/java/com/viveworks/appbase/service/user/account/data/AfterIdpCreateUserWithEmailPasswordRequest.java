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
@AfterIdpCreateUserWithEmailPasswordRequest.Valid
public class AfterIdpCreateUserWithEmailPasswordRequest extends AbstractTraceable {
    private String appbaseRequesterId;
    private String idpUserId;
    private String idpUserToken;
    private String name;
    private String email;
    private PasswordHash clientPasswordHash;
    private String languageCode;
    private String appName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    private String latitude;
    private String longitude;
    private UserSessionData userSessionData;

    public AfterIdpCreateUserWithEmailPasswordRequest() {
    }

    public AfterIdpCreateUserWithEmailPasswordRequest(
            final String traceId,
            final String appbaseRequesterId,
            final String idpUserId,
            final String idpUserToken,
            final String name,
            final String email,
            final PasswordHash clientPasswordHash,
            final String languageCode,
            final String appName,
            final UserSessionData userSessionData)
    {
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.idpUserId = idpUserId;
        this.idpUserToken = idpUserToken;
        this.name = name;
        this.email = email;
        this.clientPasswordHash = clientPasswordHash;
        this.languageCode = languageCode;
        this.appName = appName;
        this.userSessionData = userSessionData;
    }

    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
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

    public PasswordHash getClientPasswordHash() {
        return clientPasswordHash;
    }

    public void setClientPasswordHash(final PasswordHash clientPasswordHash) {
        this.clientPasswordHash = clientPasswordHash;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
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

        final AfterIdpCreateUserWithEmailPasswordRequest that = (AfterIdpCreateUserWithEmailPasswordRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(idpUserId, that.idpUserId) &&
                Objects.equals(idpUserToken, that.idpUserToken) &&
                Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                Objects.equals(clientPasswordHash, that.clientPasswordHash) &&
                Objects.equals(languageCode, that.languageCode) &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(addressLine1, that.addressLine1) &&
                Objects.equals(addressLine2, that.addressLine2) &&
                Objects.equals(city, that.city) &&
                Objects.equals(stateProvince, that.stateProvince) &&
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
                idpUserId,
                idpUserToken,
                name,
                email,
                clientPasswordHash,
                languageCode,
                appName,
                addressLine1,
                addressLine2,
                city,
                stateProvince,
                country,
                postalCode,
                latitude,
                longitude,
                userSessionData);
    }

    @Override
    public String toString() {
        return "AfterIdpCreateUserWithEmailPasswordRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", idpUserId='" + idpUserId + '\'' +
                ", idpUserToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", clientPasswordHash='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", appName='" + appName + '\'' +
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
    @Constraint(validatedBy = AfterIdpCreateUserWithEmailPasswordRequest.Validator.class)
    public @interface Valid {
        String message() default
                "com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator
            <AfterIdpCreateUserWithEmailPasswordRequest.Valid, AfterIdpCreateUserWithEmailPasswordRequest>
    {
        @Override
        public boolean isValid(
                final AfterIdpCreateUserWithEmailPasswordRequest value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserId()) &&
                    AppbaseUtil.isNotBlank(value.getIdpUserToken()) &&
                    AppbaseUtil.isNotBlank(value.getName()) &&
                    AppbaseUtil.isNotBlank(value.getEmail()) &&
                    AppbaseUtil.isValid(value.getClientPasswordHash(), new PasswordHash.Validator(), context) &&
                    AppbaseUtil.isNotBlank(value.getLanguageCode()) &&
                    AppbaseUtil.isNotBlank(value.getAppName());
        }
    }
}
