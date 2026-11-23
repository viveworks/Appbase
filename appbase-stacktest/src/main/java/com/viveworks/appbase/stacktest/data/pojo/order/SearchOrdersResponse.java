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
package com.viveworks.appbase.stacktest.data.pojo.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.stacktest.data.model.Order;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SearchOrdersResponse implements Traceable {
    @Nullable
    @JsonSerialize(contentAs = Order.class)
    @JsonDeserialize(contentAs = Order.class)
    private final List<Order> orders;

    @Nullable
    private String traceId;

    @JsonCreator
    public SearchOrdersResponse(
            @Nullable
            @JsonProperty("orders")
            final List<Order> orders,
            @Nullable
            @JsonProperty("traceId")
            final String traceId)
    {
        this.orders = orders;
        this.traceId = traceId;
    }

    @Nullable
    public List<Order> getOrders() {
        return orders;
    }

    @Nullable
    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public void setTraceId(@Nullable final String traceId) {
        this.traceId = traceId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SearchOrdersResponse that = (SearchOrdersResponse) o;
        return Objects.equals(orders, that.orders) &&
                Objects.equals(traceId, that.traceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orders, traceId);
    }

    @Override
    public String toString() {
        return "SearchOrdersResponse{" +
                "orders=" + orders +
                ", traceId='" + traceId + '\'' +
                '}';
    }


    @Target({ METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = SearchOrdersResponse.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.stacktest.data.pojo.order.SearchOrdersResponse}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator implements ConstraintValidator<SearchOrdersResponse.Valid, SearchOrdersResponse> {
        @Override
        public boolean isValid(
                final SearchOrdersResponse value,
                final ConstraintValidatorContext context)
        {
            final boolean notNull = AppbaseUtil.isNotNull(value);
            if (!notNull) {
                return false;
            }

            final List<Order> orders = value.getOrders();
            if (orders != null) {
                final Order.Validator validator = new Order.Validator();

                for (final Order order : orders) {
                    if (!AppbaseUtil.isValid(order, validator, context)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }
}
