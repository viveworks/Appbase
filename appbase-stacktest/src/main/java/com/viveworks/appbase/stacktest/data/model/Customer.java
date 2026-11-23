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
package com.viveworks.appbase.stacktest.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class Customer implements Cloneable {
    public static final String CUSTOMER_ID_INDEX_FIELD = "customer-id";
    public static final String EMAIL_INDEX_FIELD = "email";
    public static final String PHOTO_ID_INDEX_FIELD = "photo-id";

    @PositiveOrZero
    @ColumnName("customer_id")
    private Long customerId;

    @NotEmpty
    @ColumnName("email")
    private String email;

    @Nullable
    @ColumnName("photoId")
    private String photoId;

    public Customer() {
    }

    @JsonCreator
    public Customer(
            @JsonProperty("customerId")
            final Long customerId,
            @JsonProperty("email")
            final String email,
            @Nullable
            @JsonProperty("photoId")
            final String photoId)
    {
        this.customerId = customerId;
        this.email = email;
        this.photoId = photoId;
    }

    @Override
    public Customer clone() throws CloneNotSupportedException {
        try {
            final Customer clone = (Customer) super.clone();
            clone.setPhotoId(photoId);
            clone.setEmail(email);
            clone.setCustomerId(customerId);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(final Long customerId) {
        this.customerId = customerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @Nullable
    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(@Nullable final String photoId) {
        this.photoId = photoId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId) &&
                Objects.equals(email, customer.email) &&
                Objects.equals(photoId, customer.photoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, email, photoId);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", email='" + email + '\'' +
                ", photoId='" + photoId + '\'' +
                '}';
    }


    @Target({ METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = Customer.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.stacktest.data.model.Customer}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator implements ConstraintValidator<Customer.Valid, Customer> {
        @Override
        public boolean isValid(
                final Customer value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isPositiveOrZero(value.getCustomerId()) &&
                    AppbaseUtil.isNotBlank(value.getEmail());
        }
    }
}
