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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
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
@GuestUser.Valid
public class GuestUser {
    private String appbaseUserId;

    private String idpGuestId;

    private String name;

    private String languageCode;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String stateProvince;

    private String postalCode;

    private String country;

    private String latitude;

    private String longitude;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime creationTimestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = AppbaseServiceConstants.DATE_TIME_SERIALIZATION_FORMAT)
    private LocalDateTime lastAccessTimestamp;

    public GuestUser() {
    }

    public GuestUser(
            final String appbaseUserId,
            final String idpGuestId,
            final String name,
            final String languageCode,
            final String addressLine1,
            final String addressLine2,
            final String city,
            final String stateProvince,
            final String postalCode,
            final String country,
            final String latitude,
            final String longitude,
            final LocalDateTime creationTimestamp,
            final LocalDateTime lastAccessTimestamp)
    {
        this.appbaseUserId = appbaseUserId;
        this.idpGuestId = idpGuestId;
        this.name = name;
        this.languageCode = languageCode;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.stateProvince = stateProvince;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.creationTimestamp = creationTimestamp;
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public String getAppbaseUserId() {
        return appbaseUserId;
    }

    public void setAppbaseUserId(final String appbaseUserId) {
        this.appbaseUserId = appbaseUserId;
    }

    public String getIdpGuestId() {
        return idpGuestId;
    }

    public void setIdpGuestId(final String idpGuestId) {
        this.idpGuestId = idpGuestId;
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

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(final LocalDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public LocalDateTime getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(final LocalDateTime lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GuestUser guestUser = (GuestUser) o;
        return Objects.equals(appbaseUserId, guestUser.appbaseUserId) &&
                Objects.equals(idpGuestId, guestUser.idpGuestId) &&
                Objects.equals(name, guestUser.name) &&
                Objects.equals(languageCode, guestUser.languageCode) &&
                Objects.equals(addressLine1, guestUser.addressLine1) &&
                Objects.equals(addressLine2, guestUser.addressLine2) &&
                Objects.equals(city, guestUser.city) &&
                Objects.equals(postalCode, guestUser.postalCode) &&
                Objects.equals(latitude, guestUser.latitude) &&
                Objects.equals(longitude, guestUser.longitude) &&
                Objects.equals(country, guestUser.country) &&
                Objects.equals(creationTimestamp, guestUser.creationTimestamp) &&
                Objects.equals(lastAccessTimestamp, guestUser.lastAccessTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appbaseUserId, idpGuestId, name, languageCode, creationTimestamp, lastAccessTimestamp);
    }

    @Override
    public String toString() {
        return "GuestUser{" +
                "appbaseUserId='" + appbaseUserId + '\'' +
                ", idpGuestId='" + idpGuestId + '\'' +
                ", name='" + name + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", addressLine1='" + addressLine1 + '\'' +
                ", addressLine2='" + addressLine2 + '\'' +
                ", city='" + city + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", creationTimestamp='" + creationTimestamp +
                ", lastAccessTimestamp='" + lastAccessTimestamp +
                '}';
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = GuestUser.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.account.data.GuestUser";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<GuestUser.Valid, GuestUser> {
        @Override
        public boolean isValid(final GuestUser value, final ConstraintValidatorContext context) {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getIdpGuestId());
        }
    }
}
