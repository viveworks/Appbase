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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserSessionData {
    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private Map<String, String> dataMap;

    public UserSessionData() {
    }

    public UserSessionData(final Map<String, String> dataMap) {
        this.dataMap = dataMap;
    }

    private UserSessionData(final Builder builder) {
        setDataMap(builder.dataMap);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> getDataMap() {
        return dataMap;
    }

    public void setDataMap(
            @JsonSerialize(keyAs = String.class, contentAs = String.class)
            @JsonDeserialize(keyAs = String.class, contentAs = String.class)
            final Map<String, String> dataMap)
    {
        this.dataMap = dataMap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UserSessionData that = (UserSessionData) o;
        return Objects.equals(dataMap, that.dataMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataMap);
    }

    @Override
    public String toString() {
        return "UserSessionData{" +
                "dataMap=" + dataMap +
                '}';
    }

    public static final class Builder {
        private Map<String, String> dataMap;

        private Builder() {
        }

        public Builder dataMap(final Map<String, String> dataMap) {
            this.dataMap = dataMap;
            return this;
        }

        public UserSessionData build() {
            return new UserSessionData(this);
        }
    }

    @Target({ METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UserSessionData.Validator.class)
    public @interface Valid {
        String message() default "com.viveworks.appbase.service.user.session.data.UserSessionData";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator<UserSessionData.Valid, UserSessionData> {
        @Override
        public boolean isValid(final UserSessionData value, final ConstraintValidatorContext context) {
            return true;
        }
    }
}
