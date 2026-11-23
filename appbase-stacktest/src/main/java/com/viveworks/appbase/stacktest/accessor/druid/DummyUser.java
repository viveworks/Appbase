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
package com.viveworks.appbase.stacktest.accessor.druid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyUser {
    private String userId;

    private String name;

    private String email;

    private String country;

    public DummyUser() {
    }

    @JsonCreator
    public DummyUser(
            @JsonProperty("userId")
            final String userId,
            @JsonProperty("name")
            final String name,
            @JsonProperty("email")
            final String email,
            @JsonProperty("country")
            final String country)
    {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.country = country;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DummyUser dummyUser = (DummyUser) o;
        return Objects.equals(userId, dummyUser.userId) &&
                Objects.equals(name, dummyUser.name) &&
                Objects.equals(email, dummyUser.email) &&
                Objects.equals(country, dummyUser.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, name, email, country);
    }

    @Override
    public String toString() {
        return "DummyUser{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
