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
@AfterIdpGuestSignInRequest.Valid
public class AfterIdpGuestSignInRequest extends AbstractTraceable {
    private String appbaseRequesterId;

    private String idpGuestId;

    private String idpGuestUserToken;

    private String name;

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

    public AfterIdpGuestSignInRequest() {
    }

    public AfterIdpGuestSignInRequest(
            final String traceId,
            final String appbaseRequesterId,
            final String idpGuestId,
            final String idpGuestUserToken,
            final String name,
            final String languageCode,
            final String appName,
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
        this.traceId = traceId;
        this.appbaseRequesterId = appbaseRequesterId;
        this.idpGuestId = idpGuestId;
        this.idpGuestUserToken = idpGuestUserToken;
        this.name = name;
        this.languageCode = languageCode;
        this.appName = appName;
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

    public String getIdpGuestId() {
        return idpGuestId;
    }

    public void setIdpGuestId(final String idpGuestId) {
        this.idpGuestId = idpGuestId;
    }

    public String getIdpGuestUserToken() {
        return idpGuestUserToken;
    }

    public void setIdpGuestUserToken(final String idpGuestUserToken) {
        this.idpGuestUserToken = idpGuestUserToken;
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

    public String getAppName() {
        return appName;
    }

    public void setAppName(final String appName) {
        this.appName = appName;
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

        final AfterIdpGuestSignInRequest that = (AfterIdpGuestSignInRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(idpGuestId, that.idpGuestId) &&
                Objects.equals(idpGuestUserToken, that.idpGuestUserToken) &&
                Objects.equals(name, that.name) &&
                Objects.equals(languageCode, that.languageCode) &&
                Objects.equals(appName, that.appName) &&
                Objects.equals(addressLine1, that.addressLine1) &&
                Objects.equals(addressLine2, that.addressLine2) &&
                Objects.equals(city, that.city) &&
                Objects.equals(stateProvince, that.stateProvince) &&
                Objects.equals(postalCode, that.postalCode) &&
                Objects.equals(country, that.country) &&
                Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude) &&
                Objects.equals(userSessionData, that.userSessionData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                appbaseRequesterId,
                idpGuestId,
                idpGuestUserToken,
                name,
                languageCode,
                appName,
                addressLine1,
                addressLine2,
                city,
                stateProvince,
                postalCode,
                country,
                latitude,
                longitude,
                userSessionData);
    }

    @Override
    public String toString() {
        return "AfterIdpGuestSignInRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", idpGuestId='" + idpGuestId + '\'' +
                ", idpGuestUserToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", name='" + name + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", appName='" + appName + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", city='" + city + '\'' +
                ", stateProvince='" + stateProvince + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", userSessionData=" + userSessionData +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = AfterIdpGuestSignInRequest.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.AfterIdpGuestSignInRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<AfterIdpGuestSignInRequest.Valid, AfterIdpGuestSignInRequest> {
        @Override
        public boolean isValid(final AfterIdpGuestSignInRequest value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getAppbaseRequesterId()) &&
                    AppbaseUtil.isNotBlank(value.getIdpGuestId());
        }
    }
}
