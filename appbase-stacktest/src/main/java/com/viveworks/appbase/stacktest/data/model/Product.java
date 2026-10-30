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
public class Product implements Cloneable {
    public static final String SKU_INDEX_FIELD = "sku";
    public static final String DESCRIPTION_INDEX_FIELD = "description";
    public static final String PRICE_INDEX_FIELD = "price";

    @NotEmpty
    @ColumnName("sku")
    private String sku;

    @ColumnName("description")
    private String description;

    @PositiveOrZero
    @ColumnName("price")
    private Double price;

    public Product() {
    }

    @JsonCreator
    public Product(
            @JsonProperty("sku")
            final String sku,
            @JsonProperty("description")
            final String description,
            @JsonProperty("price")
            final Double price)
    {
        this.sku = sku;
        this.description = description;
        this.price = price;
    }

    public Product clone() throws CloneNotSupportedException {
        try {
            final Product clone = (Product) super.clone();
            clone.setSku(sku);
            clone.setDescription(description);
            clone.setPrice(price);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String getSku() {
        return sku;
    }

    public void setSku(final String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(final Double price) {
        this.price = price;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Product product = (Product) o;
        return Objects.equals(sku, product.sku) &&
                Objects.equals(description, product.description) &&
                Objects.equals(price, product.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, description, price);
    }

    @Override
    public String toString() {
        return "Product{" +
                "sku='" + sku + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                '}';
    }


    @Target({ METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = Product.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.stacktest.data.model.Product}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator implements ConstraintValidator<Product.Valid, Product> {
        @Override
        public boolean isValid(
                final Product value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getSku()) &&
                    AppbaseUtil.isPositiveOrZero(value.getPrice());
        }
    }
}
