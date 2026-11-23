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
public class Order implements Cloneable {
    public static final String ORDER_ID_INDEX_FIELD = "order-id";
    public static final String CUSTOMER_ID_INDEX_FIELD = "customer-id";
    public static final String SKU_INDEX_FIELD = "sku";
    public static final String PRICE_INDEX_FIELD = "price";

    @PositiveOrZero
    @ColumnName("order_id")
    private Long orderId;

    @PositiveOrZero
    @ColumnName("customer_id")
    private Long customerId;

    @NotEmpty
    @ColumnName("sku")
    private String sku;

    @PositiveOrZero
    @ColumnName("price")
    private Double price;

    public Order() {
    }

    @JsonCreator
    public Order(
            @JsonProperty("orderId")
            final Long orderId,
            @JsonProperty("customerId")
            final Long customerId,
            @JsonProperty("sku")
            final String sku,
            @JsonProperty("price")
            final Double price)
    {
        this.orderId = orderId;
        this.customerId = customerId;
        this.sku = sku;
        this.price = price;
    }

    @Override
    public Order clone() throws CloneNotSupportedException {
        try {
            final Order clone = (Order) super.clone();
            clone.setOrderId(orderId);
            clone.setCustomerId(customerId);
            clone.setSku(sku);
            clone.setPrice(price);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(final Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(final Long customerId) {
        this.customerId = customerId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(final String sku) {
        this.sku = sku;
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

        final Order order = (Order) o;
        return Objects.equals(orderId, order.orderId) &&
                Objects.equals(customerId, order.customerId) &&
                Objects.equals(sku, order.sku) &&
                Objects.equals(price, order.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, sku, price);
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", sku='" + sku + '\'' +
                ", price=" + price +
                '}';
    }


    @Target({ METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = Order.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.stacktest.data.model.Order}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator implements ConstraintValidator<Order.Valid, Order> {
        @Override
        public boolean isValid(
                final Order value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isPositiveOrZero(value.getOrderId()) &&
                    AppbaseUtil.isPositiveOrZero(value.getCustomerId()) &&
                    AppbaseUtil.isNotBlank(value.getSku()) &&
                    AppbaseUtil.isPositiveOrZero(value.getPrice());
        }
    }
}
