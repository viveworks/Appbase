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
package com.viveworks.appbase.service.supportmessage.data;

import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

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
 * @since 1.0.0
 */
@UpdateSupportMessageResponse.Valid
public class UpdateSupportMessageResponse extends AbstractTraceable {
    private boolean updated;

    public UpdateSupportMessageResponse() {
    }

    public UpdateSupportMessageResponse(
            final boolean updated,
            @Nullable final String traceId)
    {
        super(traceId);

        this.updated = updated;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(final boolean updated) {
        this.updated = updated;
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

        final UpdateSupportMessageResponse that = (UpdateSupportMessageResponse) o;
        return updated == that.updated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), updated);
    }

    @Override
    public String toString() {
        return "UpdateSupportMessageResponse{" +
                "updated=" + updated +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = UpdateSupportMessageResponse.Validator.class)
    public @interface Valid {
        String message() default "{com.viveworks.appbase.service.supportmessage.data.UpdateSupportMessageResponse}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }


    public static class Validator
            implements ConstraintValidator<UpdateSupportMessageResponse.Valid, UpdateSupportMessageResponse>
    {
        @Override
        public boolean isValid(
                final UpdateSupportMessageResponse value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value);
        }
    }
}
