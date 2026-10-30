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
package com.viveworks.appbase.service.supportmessage.data;

import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.PositiveOrZero;

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
 * @author Claude
 * @since 1.0.0
 */
@ReadSupportMessagesRequest.Valid
public class ReadSupportMessagesRequest extends AbstractTraceable {
    @PositiveOrZero
    private Integer offset;

    @PositiveOrZero
    private Integer limit;

    public ReadSupportMessagesRequest() {
    }

    public ReadSupportMessagesRequest(
            final Integer offset,
            final Integer limit,
            @Nullable final String traceId)
    {
        super(traceId);

        this.offset = offset;
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(final Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(final Integer limit) {
        this.limit = limit;
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

        final ReadSupportMessagesRequest that = (ReadSupportMessagesRequest) o;
        return Objects.equals(offset, that.offset) &&
                Objects.equals(limit, that.limit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), offset, limit);
    }

    @Override
    public String toString() {
        return "ReadSupportMessagesRequest{" +
                "offset=" + offset +
                ", limit=" + limit +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = ReadSupportMessagesRequest.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesRequest}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator
            implements ConstraintValidator<ReadSupportMessagesRequest.Valid, ReadSupportMessagesRequest>
    {
        @Override
        public boolean isValid(
                final ReadSupportMessagesRequest value,
                final ConstraintValidatorContext context)
        {
            // L4-03: the upper bound is enforced HERE, at the service boundary, not only on the
            // JAX-RS resource's @Max -- the servlet face and any direct service caller must hit
            // the same cap. The limit clause runs after isPositiveOrZero, which is null-safe and
            // short-circuits, so a null limit can never NPE here.
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isPositiveOrZero(value.getOffset()) &&
                    AppbaseUtil.isPositiveOrZero(value.getLimit()) &&
                    value.getLimit() <= AppbaseServiceConstants.MAX_SUPPORT_MESSAGE_READ_LIMIT;
        }
    }
}
