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

import com.viveworks.appbase.core.service.tracer.AbstractTraceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.password.PasswordHash;
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
@AfterIdpConfirmPasswordResetRequest.Valid
public class AfterIdpConfirmPasswordResetRequest extends AbstractTraceable {
    @Nullable
    private String appbaseRequesterId;
    private String email;
    private PasswordHash passwordResetCodeHash;
    private PasswordHash newClientPasswordHash;

    public AfterIdpConfirmPasswordResetRequest() {
    }

    public AfterIdpConfirmPasswordResetRequest(
            @Nullable
            final String traceId,
            @Nullable
            final String appbaseRequesterId,
            final String email,
            final PasswordHash passwordResetCodeHash,
            final PasswordHash newClientPasswordHash)
    {
        super(traceId);

        this.appbaseRequesterId = appbaseRequesterId;
        this.email = email;
        this.passwordResetCodeHash = passwordResetCodeHash;
        this.newClientPasswordHash = newClientPasswordHash;
    }

    @Nullable
    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    public void setAppbaseRequesterId(@Nullable final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public PasswordHash getPasswordResetCodeHash() {
        return passwordResetCodeHash;
    }

    public void setPasswordResetCodeHash(final PasswordHash passwordResetCodeHash) {
        this.passwordResetCodeHash = passwordResetCodeHash;
    }

    public PasswordHash getNewClientPasswordHash() {
        return newClientPasswordHash;
    }

    public void setNewClientPasswordHash(final PasswordHash newClientPasswordHash) {
        this.newClientPasswordHash = newClientPasswordHash;
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

        final AfterIdpConfirmPasswordResetRequest that = (AfterIdpConfirmPasswordResetRequest) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(passwordResetCodeHash, that.passwordResetCodeHash)
                && Objects.equals(newClientPasswordHash, that.newClientPasswordHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), appbaseRequesterId, email, passwordResetCodeHash, newClientPasswordHash);
    }

    @Override
    public String toString() {
        return "AfterIdpConfirmPasswordResetRequest{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", email='" + email + '\'' +
                ", passwordResetCodeHash='" + passwordResetCodeHash + '\'' +
                ", newClientPasswordHash='" + newClientPasswordHash + '\'' +
                "} " + super.toString();
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Constraint(validatedBy = AfterIdpConfirmPasswordResetRequest.Validator.class)
    public @interface Valid {
        String message() default
                "com.viveworks.appbase.service.user.account.data.AfterIdpConfirmPasswordResetRequest";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Validator implements ConstraintValidator
            <AfterIdpConfirmPasswordResetRequest.Valid, AfterIdpConfirmPasswordResetRequest>
    {
        @Override
        public boolean isValid(
                final AfterIdpConfirmPasswordResetRequest value,
                final ConstraintValidatorContext context)
        {
            return AppbaseUtil.isNotNull(value) &&
                    AppbaseUtil.isNotBlank(value.getEmail()) &&
                    AppbaseUtil.isValid(value.getPasswordResetCodeHash(), new PasswordHash.Validator(), context) &&
                    AppbaseUtil.isValid(value.getNewClientPasswordHash(), new PasswordHash.Validator(), context);
        }
    }
}
